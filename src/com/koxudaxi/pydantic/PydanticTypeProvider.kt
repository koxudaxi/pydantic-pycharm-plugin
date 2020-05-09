package com.koxudaxi.pydantic

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.*
import com.jetbrains.python.psi.types.*
import com.koxudaxi.pydantic.PydanticConfigService.Companion.getInstance
import one.util.streamex.StreamEx

class PydanticTypeProvider : PyTypeProviderBase() {

    override fun getReferenceExpressionType(referenceExpression: PyReferenceExpression, context: TypeEvalContext): PyType? {
        return getPydanticTypeForCallee(referenceExpression, context)
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

    private fun getPydanticTypeForCallee(referenceExpression: PyReferenceExpression, context: TypeEvalContext): PyCallableType? {
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
                        it is PyTargetExpression -> (it as? PyTypedElement)?.let { pyTypedElement ->
                            context.getType(pyTypedElement)
                                    ?.let { pyType -> getPyClassTypeByPyTypes(pyType) }
                                    ?.filter { pyClassType -> pyClassType.isDefinition }
                                    ?.map { filteredPyClassType ->
                                        getPydanticTypeForClass(filteredPyClassType.pyClass, context, true)
                                    }?.firstOrNull()
                        }
                        else -> null
                    }
                }
                .firstOrNull()
    }

    private fun getPydanticTypeForClass(pyClass: PyClass, context: TypeEvalContext, init: Boolean = false): PyCallableType? {
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
