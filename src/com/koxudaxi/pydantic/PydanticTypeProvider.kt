package com.koxudaxi.pydantic

import com.intellij.icons.AllIcons
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.codeInsight.PyCustomMember
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.*
import com.jetbrains.python.psi.types.*
import com.koxudaxi.pydantic.PydanticConfigService.Companion.getInstance
import one.util.streamex.StreamEx

class PydanticTypeProvider : PyTypeProviderBase() {

    override fun getReferenceExpressionType(referenceExpression: PyReferenceExpression, context: TypeEvalContext): PyType? {
        return getPydanticTypeForCallee(referenceExpression, context)
    }

    override fun getCallType(function: PyFunction, callSite: PyCallSiteExpression, context: TypeEvalContext): Ref<PyType>? {
        return when (function.qualifiedName) {
            CON_LIST_Q_NAME -> Ref.create(createConListPyType(callSite, context)
                    ?: PyCollectionTypeImpl.createTypeByQName(callSite as PsiElement, LIST_Q_NAME, true))
            else -> null
        }
    }

    override fun getReferenceType(referenceTarget: PsiElement, context: TypeEvalContext, anchor: PsiElement?): Ref<PyType>? {
        if (referenceTarget is PyTargetExpression) {
            val pyClass = getPyClassByAttribute(referenceTarget.parent) ?: return null
            if (!isPydanticModel(pyClass, false, context)) return null
            val name = referenceTarget.name ?: return null
            getRefTypeFromFieldName(name, context, pyClass)?.let { return it }
        }
        return null
    }

    override fun getParameterType(param: PyNamedParameter, func: PyFunction, context: TypeEvalContext): Ref<PyType>? {
        return when {
            !param.isPositionalContainer && !param.isKeywordContainer && param.annotationValue == null && func.name == "__init__" -> {
                val pyClass = func.containingClass ?: return null
                if (!isPydanticModel(pyClass, false, context)) return null
                val name = param.name ?: return null
                getRefTypeFromFieldName(name, context, pyClass)
            }
            param.isSelf && isValidatorMethod(func) -> {
                val pyClass = func.containingClass ?: return null
                if (!isPydanticModel(pyClass, false, context)) return null
                Ref.create(context.getType(pyClass))
            }
            else -> null
        }
    }

    private fun getRefTypeFromFieldNameInPyClass(name: String, pyClass: PyClass, context: TypeEvalContext, ellipsis: PyNoneLiteralExpression, pydanticVersion: KotlinVersion?): Ref<PyType>? {
        return pyClass.findClassAttribute(name, false, context)?.let { return getRefTypeFromField(it, ellipsis, context, pyClass, pydanticVersion) }
    }

    private fun getRefTypeFromFieldName(name: String, context: TypeEvalContext, pyClass: PyClass): Ref<PyType>? {
        val ellipsis = PyElementGenerator.getInstance(pyClass.project).createEllipsis()

        val pydanticVersion = getPydanticVersion(pyClass.project, context)
        return getRefTypeFromFieldNameInPyClass(name, pyClass, context, ellipsis, pydanticVersion)
                ?: pyClass.getAncestorClasses(context)
                        .filter { isPydanticModel(it, false, context) }
                        .mapNotNull { ancestor ->
                            getRefTypeFromFieldNameInPyClass(name, ancestor, context, ellipsis, pydanticVersion)
                        }.firstOrNull()
    }

    private fun getRefTypeFromField(pyTargetExpression: PyTargetExpression, ellipsis: PyNoneLiteralExpression,
                                    context: TypeEvalContext, pyClass: PyClass,
                                    pydanticVersion: KotlinVersion?): Ref<PyType>? {
        return fieldToParameter(pyTargetExpression, ellipsis, context, pyClass, pydanticVersion, getConfig(pyClass, context, true))
                ?.let { parameter -> Ref.create(parameter.getType(context)) }

    }

    private fun getPydanticTypeForCallee(referenceExpression: PyReferenceExpression, context: TypeEvalContext): PyType? {
        if (PyCallExpressionNavigator.getPyCallExpressionByCallee(referenceExpression) == null) return null

        val resolveResults = getResolveElements(referenceExpression, context)

        return PyUtil.filterTopPriorityResults(resolveResults)
                .asSequence()
                .map {
                    when {
                        it is PyClass -> getPydanticTypeForClass(it, context, true)
                        it is PyParameter && it.isSelf -> {
                            PsiTreeUtil.getParentOfType(it, PyFunction::class.java)
                                    ?.takeIf { it.modifier == PyFunction.Modifier.CLASSMETHOD }
                                    ?.let { it.containingClass?.let { getPydanticTypeForClass(it, context) } }
                        }
                        it is PyNamedParameter -> it.getArgumentType(context)?.let { pyType ->
                            getPyClassTypeByPyTypes(pyType).filter { pyClassType ->
                                pyClassType.isDefinition
                            }.map { filteredPyClassType -> getPydanticTypeForClass(filteredPyClassType.pyClass, context, true) }.firstOrNull()
                        }
                        it is PyTargetExpression -> (it as? PyTypedElement)
                                ?.let { pyTypedElement ->
                                    context.getType(pyTypedElement)
                                            ?.let { pyType -> getPyClassTypeByPyTypes(pyType) }
                                            ?.filter { pyClassType -> pyClassType.isDefinition }
                                            ?.map { filteredPyClassType ->
                                                getPydanticTypeForClass(filteredPyClassType.pyClass, context, true)
                                            }?.firstOrNull()
                                } ?: getPydanticTypeForCreateModel(it, context, true)
                        else -> null
                    }
                }
                .firstOrNull()
    }


    private fun createConListPyType(pyCallSiteExpression: PyCallSiteExpression, context: TypeEvalContext): PyType? {
        val pyCallExpression = pyCallSiteExpression as? PyCallExpression ?: return null
        val argumentList = pyCallExpression.argumentList ?: return null
        if (argumentList.arguments.isEmpty()) return null
        val typeArgumentList = argumentList.getKeywordArgument("item_type") ?: argumentList.arguments[0]
        // TODO support PySubscriptionExpression
        val typeArgumentListType = context.getType(typeArgumentList) ?: return null
        val typeArgumentListReturnType = (typeArgumentListType as? PyCallableType)?.getReturnType(context)
                ?: return null
        return PyCollectionTypeImpl.createTypeByQName(pyCallExpression as PsiElement, LIST_Q_NAME, true, listOf(typeArgumentListReturnType))
    }

    private fun getPydanticTypeForCreateModel(pyTargetExpression: PyTargetExpression, context: TypeEvalContext, init: Boolean = false): PyCallableType? {
        val pyCallExpression = pyTargetExpression.findAssignedValue() as? PyCallExpression ?: return null
        val referenceExpression = (pyCallExpression.callee as? PyReferenceExpression) ?: return null
        val resolveResults = getResolveElements(referenceExpression, context)
        if (!PyUtil.filterTopPriorityResults(resolveResults).any { (it as? PyFunction)?.let { pyFunction -> isPydanticCreateModel(pyFunction) } == true }) return null

        val project = pyCallExpression.project


        val typed = !init || getInstance(project).currentInitTyped
        val collected = linkedMapOf<String, Triple<PyCallableParameter, PyCustomMember, PyElement>>()
        val pydanticVersion = getPydanticVersion(project, context)
        // TODO get config
//        val config = getConfig(pyClass, context, true)
        val baseClass = when (val baseArgument = pyCallExpression.getKeywordArgument("__base__")) {
            is PyReferenceExpression -> {
                PyUtil.filterTopPriorityResults(getResolveElements(baseArgument, context))
                        .filterIsInstance<PyClass>().firstOrNull { isPydanticModel(it, false, context) }
            }
            is PyClass -> baseArgument
            else -> null
        }?.let { baseClass ->
            val baseClassCollected = linkedMapOf<String, Triple<PyCallableParameter, PyCustomMember, PyElement>>()
            (context.getType(baseClass) as? PyClassLikeType).let { baseClassType ->
                for (currentType in StreamEx.of(baseClassType).append(baseClass.getAncestorTypes(context))) {
                    if (currentType !is PyClassType) continue
                    val current = currentType.pyClass
                    if (!isPydanticModel(current, false, context)) continue
                    getClassVariables(current, context)
                            .map { Pair(fieldToParameter(it, context, pydanticVersion, hashMapOf(), typed), it) }
                            .filter { (parameter, _) -> parameter?.name?.let { !collected.containsKey(it) } ?: false }
                            .forEach { (parameter, field) ->
                                parameter?.name?.let { name ->
                                    val type = parameter.getType(context)
                                    val member = PyCustomMember(name, null) { type }
                                            .toPsiElement(field)
                                            .withIcon(AllIcons.Nodes.Field)
                                    baseClassCollected[name] = Triple(parameter, member, field)
                                }
                            }
                }
            }
            baseClassCollected.entries.reversed().forEach {
                collected[it.key] = it.value
            }
            baseClass
        } ?: getPydanticBaseModel(project, context) ?: return null

        val modelClass = PyPsiFacade.getInstance(baseClass.project).createClassByQName(BASE_MODEL_Q_NAME, baseClass)
                ?: return null

        pyCallExpression.arguments
                .filter { it is PyKeywordArgument || (it as? PyStarArgumentImpl)?.isKeyword == true }
                .filterNot { it.name?.startsWith("_") == true || it.name == "model_name" }
                .forEach {
                    val parameter = fieldToParameter(it, context, pydanticVersion, hashMapOf(), typed)!!
                    parameter.name?.let { name ->
                        val type = parameter.getType(context)
                        val member = PyCustomMember(name, null) { type }
                                .toPsiElement(it)
                                .withIcon(AllIcons.Nodes.Field)
                        collected[name] = Triple(parameter, member, it)
                    }
                }

        val modelClassType = PydanticDynamicModelClassType(modelClass, false, collected.values.map { it.second }, collected.entries.map { it.key to it.value.third }.toMap())
        return PyCallableTypeImpl(collected.values.map { it.first }, modelClassType.toInstance())
    }

    fun getPydanticTypeForClass(pyClass: PyClass, context: TypeEvalContext, init: Boolean = false): PyCallableType? {
        if (!isPydanticModel(pyClass, false, context)) return null
        val clsType = (context.getType(pyClass) as? PyClassLikeType) ?: return null
        val ellipsis = PyElementGenerator.getInstance(pyClass.project).createEllipsis()

        val typed = !init || getInstance(pyClass.project).currentInitTyped
        val collected = linkedMapOf<String, PyCallableParameter>()
        val pydanticVersion = getPydanticVersion(pyClass.project, context)
        val config = getConfig(pyClass, context, true)
        for (currentType in StreamEx.of(clsType).append(pyClass.getAncestorTypes(context))) {
            if (currentType !is PyClassType) continue

            val current = currentType.pyClass
            if (!isPydanticModel(current, false, context)) continue

            getClassVariables(current, context)
                    .mapNotNull { fieldToParameter(it, ellipsis, context, current, pydanticVersion, config, typed) }
                    .filter { parameter -> parameter.name?.let { !collected.containsKey(it) } ?: false }
                    .forEach { parameter -> collected[parameter.name!!] = parameter }
        }
        return PyCallableTypeImpl(collected.values.reversed(), clsType.toInstance())
    }

    private fun hasAnnotationValue(field: PyTargetExpression): Boolean {
        return field.annotationValue != null
    }

    internal fun fieldToParameter(field: PyTargetExpression,
                                  ellipsis: PyNoneLiteralExpression,
                                  context: TypeEvalContext,
                                  pyClass: PyClass,
                                  pydanticVersion: KotlinVersion?,
                                  config: HashMap<String, Any?>,
                                  typed: Boolean = true,
                                  isDataclass: Boolean = false): PyCallableParameter? {
        if (field.name == null || !isValidFieldName(field.name!!)) return null
        if (!hasAnnotationValue(field) && !field.hasAssignedValue()) return null // skip fields that are invalid syntax

        val defaultValueFromField = getDefaultValueForParameter(field, ellipsis, context, pydanticVersion, isDataclass)
        val defaultValue = when {
            isBaseSetting(pyClass, context) -> ellipsis
            else -> defaultValueFromField
        }

        val typeForParameter = when {
            !typed -> null
            !hasAnnotationValue(field) && defaultValueFromField is PyTypedElement -> {
                // get type from default value
                context.getType(defaultValueFromField)
            }
            else -> {
                // get type from annotation
                getTypeForParameter(field, context)
            }
        }

        return PyCallableParameterImpl.nonPsi(
                getFieldName(field, context, config, pydanticVersion),
                typeForParameter,
                defaultValue
        )
    }

    internal fun fieldToParameter(field: PyExpression,
                                  context: TypeEvalContext,
                                  pydanticVersion: KotlinVersion?,
                                  config: HashMap<String, Any?>,
                                  typed: Boolean = true): PyCallableParameter? {
        var type: PyType?
        var defaultValue: PyExpression?
        when (val tupleValue = PsiTreeUtil.findChildOfType(field, PyTupleExpression::class.java)) {
            is PyTupleExpression -> {
                tupleValue.toList().let {
                    type = when (val typeValue = it[0]) {
                        is PyType -> typeValue
                        is PyReferenceExpression -> {
                            val resolveResults = getResolveElements(typeValue, context)
                            PyUtil.filterTopPriorityResults(resolveResults)
                                    .filterIsInstance<PyClass>()
                                    .map { pyClass -> pyClass.getType(context)?.getReturnType(context) }
                                    .firstOrNull()
                        }
                        else -> null
                    }
                    defaultValue = it[1]
                }
            }
            else -> {
                type = context.getType(field)
                defaultValue = (field as? PyKeywordArgumentImpl)?.valueExpression
            }
        }
        val typeForParameter = when {
            !typed -> null
            else -> {
                type
            }
        }

        return PyCallableParameterImpl.nonPsi(
                field.name,
//                getFieldName(field, context, config, pydanticVersion),
                typeForParameter,
                defaultValue
        )
    }

    private fun getTypeForParameter(field: PyTargetExpression,
                                    context: TypeEvalContext): PyType? {

        return context.getType(field)
    }

    private fun getDefaultValueForParameter(field: PyTargetExpression,
                                            ellipsis: PyNoneLiteralExpression,
                                            context: TypeEvalContext,
                                            pydanticVersion: KotlinVersion?,
                                            isDataclass: Boolean): PyExpression? {

        when (val value = field.findAssignedValue()) {
            null -> {
                when (val annotation = field.annotation?.value) {
                    is PySubscriptionExpressionImpl -> {
                        when {
                            annotation.qualifier == null -> return value
                            annotation.qualifier!!.text == "Optional" -> return ellipsis
                            annotation.qualifier!!.text == "Union" -> annotation.children
                                    .filterIsInstance<PyTupleExpression>()
                                    .forEach {
                                        it.children
                                                .forEach { type -> if (type is PyNoneLiteralExpression) return ellipsis }
                                    }
                        }
                        return value
                    }
                    is PyReferenceExpressionImpl -> {
                        return if (annotation.text == "Any") {
                            ellipsis
                        } else null
                    }
                    else -> {
                        return null
                    }
                }
            }
            else -> return getDefaultValueByAssignedValue(field, ellipsis, context, pydanticVersion, isDataclass)
        }
    }

    private fun getDefaultValueByAssignedValue(field: PyTargetExpression,
                                               ellipsis: PyNoneLiteralExpression,
                                               context: TypeEvalContext,
                                               pydanticVersion: KotlinVersion?,
                                               isDataclass: Boolean): PyExpression? {
        val assignedValue = field.findAssignedValue()!!

        if (assignedValue.text == "...") {
            return null
        }

        val callee = (assignedValue as? PyCallExpressionImpl)?.callee ?: return assignedValue
        val referenceExpression = callee.reference?.element as? PyReferenceExpression ?: return ellipsis

        val resolveResults = getResolveElements(referenceExpression, context)
        if (isDataclass) {
            PyUtil.filterTopPriorityResults(resolveResults)
                    .any {
                        isDataclassFieldByPsiElement(it)
                    }
                    .let {
                        return when {
                            it -> getDefaultValueForDataclass(assignedValue, context)
                            else -> assignedValue
                        }
                    }
        } else {

            val versionZero = pydanticVersion?.major == 0
            PyUtil.filterTopPriorityResults(resolveResults)
                    .any {
                        when {
                            versionZero -> isPydanticSchemaByPsiElement(it, context)
                            else -> isPydanticFieldByPsiElement(it)
                        }

                    }
                    .let {
                        return when {
                            it -> getDefaultValue(assignedValue)
                            else -> assignedValue
                        }
                    }
        }
    }

    private fun getDefaultValue(assignedValue: PyCallExpression): PyExpression? {
        val defaultValue = assignedValue.getKeywordArgument("default")
                ?: assignedValue.getArgument(0, PyExpression::class.java)
        return when {
            defaultValue == null -> null
            defaultValue.text == "..." -> null
            else -> defaultValue
        }
    }

    private fun getDefaultValueForDataclass(assignedValue: PyCallExpression, context: TypeEvalContext, argumentName: String): PyExpression? {
        val defaultValue = assignedValue.getKeywordArgument(argumentName)
        return when {
            defaultValue == null -> null
            defaultValue.text == "..." -> null
            defaultValue is PyReferenceExpression -> {
                val resolveResults = getResolveElements(defaultValue, context)
                PyUtil.filterTopPriorityResults(resolveResults).any { isDataclassMissingByPsiElement(it) }.let {
                    return when {
                        it -> null
                        else -> defaultValue
                    }
                }
            }
            else -> defaultValue
        }
    }

    private fun getDefaultValueForDataclass(assignedValue: PyCallExpression, context: TypeEvalContext): PyExpression? {
        val defaultValue = getDefaultValueForDataclass(assignedValue, context, "default")
        val defaultFactoryValue = getDefaultValueForDataclass(assignedValue, context, "default_factory")
        return when {
            defaultValue == null && defaultFactoryValue == null -> null
            defaultValue != null -> defaultValue
            else -> defaultFactoryValue
        }
    }
}
