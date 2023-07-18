package com.koxudaxi.pydantic

import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.PyCustomType
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider.isBitwiseOrUnionAvailable
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.*
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.*
import com.koxudaxi.pydantic.PydanticConfigService.Companion.getInstance
import one.util.streamex.StreamEx

class PydanticTypeProvider : PyTypeProviderBase() {
    private val pyTypingTypeProvider = PyTypingTypeProvider()

    override fun getCallType(
        pyFunction: PyFunction,
        callSite: PyCallSiteExpression,
        context: TypeEvalContext,
    ): Ref<PyType>? {
        return when (pyFunction.qualifiedName) {
            CON_LIST_Q_NAME -> Ref.create(
                createConListPyType(callSite, context)
                    ?: PyCollectionTypeImpl.createTypeByQName(callSite as PsiElement, LIST_Q_NAME, true)
            )

            CREATE_MODEL -> Ref.create(
                getPydanticDynamicModelTypeForFunction(pyFunction, callSite.getArguments(null), context)
            )

            else -> null
        }
    }

    override fun getReferenceType(
        referenceTarget: PsiElement,
        context: TypeEvalContext,
        anchor: PsiElement?,
    ): Ref<PyType>? {
        return when {
            referenceTarget is PyClass && anchor is PyCallExpression -> getPydanticTypeForClass(
                referenceTarget,
                context,
                getInstance(anchor.project).currentInitTyped,
                anchor
            )

            referenceTarget is PyCallExpression -> {
                getPydanticDynamicModelTypeForTargetExpression(referenceTarget, context)?.pyCallableType
            }

            referenceTarget is PyTargetExpression -> {
                val name = referenceTarget.name
                if (name is String) {
                    val pyClass = getPyClassByAttribute(referenceTarget.parent)
                        ?.takeIf { isPydanticModel(it, false, context) }
                    if (pyClass is PyClass) {
                        return Ref.create(getRefTypeFromFieldName(name, context, pyClass))
                    }
                }

                getPydanticDynamicModelTypeForTargetExpression(referenceTarget, context)?.let { return Ref.create(it) }
            }

            else -> null
        }?.let { Ref.create(it) }
    }

    override fun getParameterType(param: PyNamedParameter, func: PyFunction, context: TypeEvalContext): Ref<PyType>? {
        return when {
            !param.isPositionalContainer && !param.isKeywordContainer && param.annotationValue == null && func.name == PyNames.INIT -> {
                val pyClass = func.containingClass ?: return null
                if (!isPydanticModel(pyClass, false, context)) return null
                val name = param.name ?: return null
                getRefTypeFromFieldName(name, context, pyClass)
            }

            param.isSelf && func.isValidatorMethod(PydanticCacheService.getVersion(func.project)
                ) -> {
                val pyClass = func.containingClass ?: return null
                if (!isPydanticModel(pyClass, false, context)) return null
                context.getType(pyClass)
            }

            else -> null
        }?.let { Ref.create(it) }
    }

    private fun getRefTypeFromFieldNameInPyClass(
        name: String,
        pyClass: PyClass,
        context: TypeEvalContext,
        ellipsis: PyNoneLiteralExpression,
        pydanticVersion: KotlinVersion?,
    ): PyType? {
        return pyClass.findClassAttribute(name, false, context)
            ?.let { return getRefTypeFromField(it, ellipsis, context, pyClass, pydanticVersion) }
    }

    private fun getRefTypeFromFieldName(name: String, context: TypeEvalContext, pyClass: PyClass): PyType? {
        val ellipsis = PyElementGenerator.getInstance(pyClass.project).createEllipsis()

        val pydanticVersion = PydanticCacheService.getVersion(pyClass.project)
        return getRefTypeFromFieldNameInPyClass(name, pyClass, context, ellipsis, pydanticVersion)
            ?: getAncestorPydanticModels(pyClass, false, context).firstNotNullOfOrNull { ancestor ->
                getRefTypeFromFieldNameInPyClass(name, ancestor, context, ellipsis, pydanticVersion)
            }
    }

    private fun getRefTypeFromField(
        pyTargetExpression: PyTargetExpression, ellipsis: PyNoneLiteralExpression,
        context: TypeEvalContext, pyClass: PyClass,
        pydanticVersion: KotlinVersion?,
    ): PyType? {
        return dynamicModelFieldToParameter(
            pyTargetExpression,
            ellipsis,
            context,
            pyClass,
            pydanticVersion,
            getConfig(pyClass, context, true),
            getGenericTypeMap(pyClass, context)
        )?.getType(context)
    }


    private fun getPyType(pyExpression: PyExpression, context: TypeEvalContext): PyType? {
        return when (val type = context.getType(pyExpression)) {
            is PyClassLikeType -> type.toInstance()
            else -> type
        }
    }

    private fun getInjectedGenericType(
        pyExpression: PyExpression,
        context: TypeEvalContext,
    ): PyType? {
        when (pyExpression) {
            is PySubscriptionExpression -> {
                val rootOperand = (pyExpression.rootOperand as? PyReferenceExpression)
                    ?.let { pyReferenceExpression ->
                        getResolvedPsiElements(pyReferenceExpression, context)
                            .asSequence()
                            .filterIsInstance<PyQualifiedNameOwner>()
                            .firstOrNull()
                    }
                when (val qualifiedName = rootOperand?.qualifiedName) {
                    TYPE_Q_NAME -> return (pyExpression.indexExpression as? PyTypedElement)?.let { context.getType(it) }
                    in listOf(TUPLE_Q_NAME, UNION_Q_NAME, OPTIONAL_Q_NAME) -> {
                        val indexExpression = pyExpression.indexExpression
                        when (indexExpression) {
                            is PyTupleExpression -> indexExpression.elements
                                .map { element -> getInjectedGenericType(element, context) }

                            is PySubscriptionExpression -> listOf(getInjectedGenericType(indexExpression, context))
                            is PyTypedElement -> listOf(getPyType(indexExpression, context))
                            else -> null
                        }?.let {
                            return when (qualifiedName) {
                                UNION_Q_NAME -> PyUnionType.union(it)
                                OPTIONAL_Q_NAME -> PyUnionType.union(it + PyNoneType.INSTANCE)
                                else -> PyTupleType.create(indexExpression as PsiElement, it)
                            }
                        }
                    }
                }
            }

            is PyBinaryExpression -> {
                return pyExpression.children.filterIsInstance<PyExpression>()
                    .mapNotNull { element -> getInjectedGenericType(element, context) }.let {
                        PyUnionType.union(it)
                    }
            }
        }
        return getPyType(pyExpression, context)
    }

    private fun scopedGenericType(
        pyTargetExpression: PyTargetExpression,
        pyClass: PyClass?,
        context: TypeEvalContext
    ): PyGenericType? {
        val pyTypedElement = pyTargetExpression as? PyTypedElement ?: return null
        val pyGenericType = pyTypedElement.getType(context) as? PyGenericType ?: return null
        return pyGenericType.withScopeOwner(pyClass).withTargetExpression(pyTargetExpression)
    }

    private fun collectGenericTypes(pyClass: PyClass, context: TypeEvalContext): List<PyGenericType> {
        val pyCollectionType = pyTypingTypeProvider.getGenericType(pyClass, context) as? PyCollectionType
        val genericTypes = pyCollectionType?.elementTypes?.filterIsInstance<PyGenericType>() ?: emptyList()
        return (genericTypes +
                pyClass.superClassExpressions
                    .mapNotNull {
                        when (it) {
                            is PySubscriptionExpression -> it
                            is PyReferenceExpression -> getResolvedPsiElements(it, context)
                                .asSequence()
                                .filterIsInstance<PySubscriptionExpression>()
                                .firstOrNull()

                            else -> null
                        }
                    }.flatMap { pySubscriptionExpression ->
                        val referenceExpression =
                            pySubscriptionExpression.rootOperand as? PyReferenceExpression ?: return@flatMap emptyList()
                        val rootOperandType = context.getType(referenceExpression) ?: return@flatMap emptyList()
                        val isGenericModel =
                            rootOperandType is PyClassType && isSubClassOfPydanticGenericModel(
                                rootOperandType.pyClass,
                                context
                            )
                        if (!isGenericModel && (rootOperandType as? PyCustomType)?.classQName != GENERIC_Q_NAME) return@flatMap emptyList()

                        when (val indexExpression = pySubscriptionExpression.indexExpression) {
                            is PyTupleExpression -> indexExpression.elements
                                .filterIsInstance<PyReferenceExpression>().map { it.reference.resolve() }
                                .filterIsInstance<PyTargetExpression>().map { scopedGenericType(it, pyClass, context) }
                                .toList()

                            is PyTargetExpression -> listOf(scopedGenericType(indexExpression, pyClass, context))
                            else -> null
                        } ?: emptyList()
                    }.filterNotNull()
                ).distinct()
    }

    override fun prepareCalleeTypeForCall(
        type: PyType?,
        call: PyCallExpression,
        context: TypeEvalContext,
    ): Ref<PyCallableType?>? {
        val pyClass = (type as? PyClassType)?.pyClass ?: return null
        if (!isSubClassOfPydanticGenericModel(pyClass, context) || pyClass.isPydanticGenericModel) return null

        return getPydanticTypeForClass(
            pyClass,
            context,
            getInstance(pyClass.project).currentInitTyped,
            call
        )?.let { Ref.create(it) }
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
        return PyCollectionTypeImpl.createTypeByQName(
            pyCallExpression as PsiElement,
            LIST_Q_NAME,
            true,
            listOf(typeArgumentListReturnType)
        )
    }


    private fun getPydanticDynamicModelPyClass(
        pyTargetExpression: PyTargetExpression,
        context: TypeEvalContext,
    ): PyClass? {
        return getPydanticDynamicModelTypeForTargetExpression(pyTargetExpression, context)?.pyClass
    }

    private fun getPydanticDynamicModelTypeForTargetExpression(
        pyTargetExpression: PyTargetExpression,
        context: TypeEvalContext,
    ): PydanticDynamicModelClassType? {
        val pyCallExpression = pyTargetExpression.findAssignedValue() as? PyCallExpression ?: return null
        return getPydanticDynamicModelTypeForTargetExpression(pyCallExpression, context)
    }

    private fun getPydanticDynamicModelTypeForTargetExpression(
        pyCallExpression: PyCallExpression,
        context: TypeEvalContext,
    ): PydanticDynamicModelClassType? {
        val arguments = pyCallExpression.arguments.toList()
        if (arguments.isEmpty()) return null
        val pyFunction = pyCallExpression.multiResolveCalleeFunction(PyResolveContext.defaultContext(context))
            .asSequence()
            .filterIsInstance<PyFunction>()
            .map { it.takeIf { pyFunction -> pyFunction.isPydanticCreateModel } }.firstOrNull()
            ?: return null
        return getPydanticDynamicModelTypeForFunction(pyFunction, arguments, context)
    }

    private fun getPydanticDynamicModelTypeForFunction(
        pyFunction: PyFunction,
        pyArguments: List<PyExpression>,
        context: TypeEvalContext,
    ): PydanticDynamicModelClassType? {
        val project = pyFunction.project
        val typed = getInstance(project).currentInitTyped
        val pydanticVersion = PydanticCacheService.getVersion(pyFunction.project)
        val collected = linkedMapOf<String, PydanticDynamicModel.Attribute>()
        val newVersion = pydanticVersion == null || pydanticVersion.isAtLeast(1, 5)
        val modelNameParameterName = if (newVersion) "__model_name" else "model_name"

        val keywordArguments: Map<String, PyExpression> = pyArguments
            .filter { it is PyKeywordArgument || (it as? PyStarArgumentImpl)?.isKeyword == true }
            .mapNotNull { it.name?.let { name -> name to it } }
            .toMap()
        val modelNameArgument = if (pyArguments.size == keywordArguments.size) {
            // TODO: Support model name on StartArgument
            (keywordArguments[modelNameParameterName] as? PyKeywordArgument)?.valueExpression
        } else {
            pyArguments.firstOrNull()
        } ?: return null
        val modelName = when (modelNameArgument) {
            is PyReferenceExpression -> getResolvedPsiElements(modelNameArgument, context)
                .asSequence()
                .filterIsInstance<PyTargetExpression>()
                .map { it.findAssignedValue() }
                .firstOrNull()
                .let { PyPsiUtils.strValue(it) }

            else -> PyPsiUtils.strValue(modelNameArgument)
        } ?: return null
        // TODO get config
//        val config = getConfig(pyClass, context, true)
        // TODO: Support __base__ on StartArgument
        val baseClass =
            when (val baseArgument = (keywordArguments["__base__"] as? PyKeywordArgument)?.valueExpression) {
                is PyReferenceExpression -> {
                    getResolvedPsiElements(baseArgument, context)
                        .asSequence()
                        .map {
                            when (it) {
                                is PyTargetExpression -> getPydanticDynamicModelPyClass(it, context)
                                is PyClass -> it.takeIf { isPydanticModel(it, false, context) }
                                else -> null
                            }
                        }.firstOrNull()
                }

                is PyClass -> baseArgument.takeIf { isPydanticModel(baseArgument, false, context) }
                else -> null
            }
                ?.let { baseClass ->
                    val baseClassCollected = linkedMapOf<String, PydanticDynamicModel.Attribute>()
                    (context.getType(baseClass) as? PyClassLikeType).let { baseClassType ->
                        for (currentType in StreamEx.of(baseClassType).append(baseClass.getAncestorTypes(context))) {
                            if (currentType !is PyClassType) continue
                            val current = currentType.pyClass
                            if (!isPydanticModel(current, false, context)) continue
                            if (current is PydanticDynamicModel) {
                                baseClassCollected.putAll(current.attributes)
                                continue
                            }
                            baseClassCollected.putAll(getClassVariables(current, context)
                                .map { it to dynamicModelFieldToParameter(it, context, typed) }
                                .mapNotNull { (field, parameter) ->
                                    parameter.name?.let { name -> Triple(field, parameter, name) }
                                }
                                .filterNot { (_, _, name) -> collected.containsKey(name) }
                                .map { (field, parameter, name) ->
                                    name to PydanticDynamicModel.createAttribute(
                                        name,
                                        parameter,
                                        field,
                                        context,
                                        true
                                    )
                                }
                            )

                        }
                    }
                    collected.putAll(baseClassCollected.entries.reversed().map { it.key to it.value })
                    baseClass
                } ?: getPydanticBaseModel(project, context) ?: return null

        collected.putAll(keywordArguments
            .filter { (name, _) -> name.isValidFieldName(pydanticVersion.isV2) && !name.startsWith('_') }
            .filter { (name, _) -> (newVersion || name != "model_name") }
            .map { (name, field) ->
                val parameter = dynamicModelFieldToParameter(field, context, typed)
                name to PydanticDynamicModel.createAttribute(name, parameter, field, context, false)
            }
        )

        return PydanticDynamicModelClassType(
            PydanticDynamicModel(
                PyElementGenerator.getInstance(project)
                    .createFromText(
                        LanguageLevel.forElement(pyFunction),
                        PyClass::class.java,
                        "class ${modelName}: pass"
                    ).node,
                baseClass,
                collected
            ),
            true
        )
    }

    private fun getBaseSettingInitParameters(
        baseSetting: PyClass,
        context: TypeEvalContext,
        typed: Boolean,
    ): List<PyCallableParameter>? {
        return baseSetting.findInitOrNew(true, context)?.parameterList?.parameters
            ?.filterIsInstance<PyNamedParameter>()
            ?.filter { it.name?.matches(Regex("^_[^_].*")) == true }
            ?.mapNotNull { argumentToParameter(it, context, typed) }
    }

    private fun getBaseSetting(pyClass: PyClass, context: TypeEvalContext): PyClass? {
        return pyClass.getSuperClasses(context).firstNotNullOfOrNull {
            if (it.isBaseSettings) {
                it
            } else {
                getBaseSetting(it, context)
            }
        }
    }

    fun getGenericTypeMap(
        pyClass: PyClass,
        context: TypeEvalContext,
        pyCallExpression: PyCallExpression? = null,
    ): Map<PyGenericType, PyType>? {
        if (!PyTypingTypeProvider.isGeneric(pyClass, context)) return null
        if (!(isSubClassOfPydanticGenericModel(pyClass, context) && !pyClass.isPydanticGenericModel)) return null

        // class Response(GenericModel, Generic[TypeA, TypeB]): pass
        val pyClassGenericTypeMap = pyTypingTypeProvider.getGenericSubstitutions(pyClass, context)
            .mapNotNull { (key, value) -> key to value }.filterIsInstance<Pair<PyGenericType, PyType>>().toMap()

        // Response[TypeA]
        val pySubscriptionExpression = when (val firstChild = pyCallExpression?.firstChild) {
            is PySubscriptionExpression -> firstChild
            is PyReferenceExpression -> getResolvedPsiElements(firstChild, context)
                .firstOrNull()
                ?.let { it as? PyTargetExpression }
                ?.findAssignedValue() as? PySubscriptionExpression

            else -> null
        } ?: return pyClassGenericTypeMap.takeIf { it.isNotEmpty() }

        // Response[TypeA, TypeB]()
        val injectedTypes = (pySubscriptionExpression.indexExpression as? PyTupleExpression)
            ?.elements
            ?.map { getInjectedGenericType(it, context) }
        // Response[TypeA]()
            ?: listOf((pySubscriptionExpression.indexExpression?.let { getInjectedGenericType(it, context) }))


        return pyClassGenericTypeMap.toMutableMap().apply {
            this.putAll(collectGenericTypes(pyClass, context)
                .take(injectedTypes.size)
                .mapIndexedNotNull { index, genericType ->
                    genericType.let {
                        injectedTypes[index]?.let { injectedType -> genericType to injectedType }
                    }
                }
                .toMap()
            )
        }.takeIf { it.isNotEmpty() }
    }

    fun getPydanticTypeForClass(
        pyClass: PyClass,
        context: TypeEvalContext,
        init: Boolean = false,
        pyCallExpression: PyCallExpression,
    ): PyCallableType? {
        if (!isPydanticModel(pyClass, false, context)) return null
        val clsType = (context.getType(pyClass) as? PyClassLikeType) ?: return null

        getPydanticModelInit(pyClass, context)?.let {
            val callParameters = it.getParameters(context)
                .filterNot { parameter -> parameter.isSelf }
            return PyCallableTypeImpl(callParameters, clsType.toInstance())
        }

        val ellipsis = PyElementGenerator.getInstance(pyClass.project).createEllipsis()

        val typed = !init || getInstance(pyClass.project).currentInitTyped
        val collected = linkedMapOf<String, PyCallableParameter>()

        if (isSubClassOfBaseSetting(pyClass, context)) {
            getBaseSetting(pyClass, context)?.let { baseSetting ->
                getBaseSettingInitParameters(baseSetting, context, typed)
                    ?.mapNotNull { parameter -> parameter.name?.let { name -> name to parameter } }
                    ?.let { collected.putAll(it) }
            }
        }
        val genericTypeMap = getGenericTypeMap(pyClass, context, pyCallExpression)
        val pydanticVersion = PydanticCacheService.getVersion(pyClass.project)
        val config = getConfig(pyClass, context, true)
        for (currentType in StreamEx.of(clsType).append(pyClass.getAncestorTypes(context))) {
            if (currentType !is PyClassType) continue

            val current = currentType.pyClass
            if (!isPydanticModel(current, false, context)) continue

            getClassVariables(current, context)
                .filterNot { isUntouchedClass(it.findAssignedValue(), config, context) }
                .mapNotNull {
                    dynamicModelFieldToParameter(
                        it,
                        ellipsis,
                        context,
                        current,
                        pydanticVersion,
                        config,
                        genericTypeMap,
                        typed,
                    )
                }
                .filter { parameter ->
                    parameter.name?.let {
                        PyNames.isIdentifier(it) && !collected.containsKey(it)
                    } ?: false
                }
                .forEach { parameter -> collected[parameter.name!!] = parameter }
        }

        return PyCallableTypeImpl(collected.values.reversed(), clsType.toInstance())
    }

    private fun hasAnnotationValue(field: PyTargetExpression): Boolean {
        return field.annotationValue != null
    }


    internal fun dynamicModelFieldToParameter(
        field: PyTargetExpression,
        ellipsis: PyNoneLiteralExpression,
        context: TypeEvalContext,
        pyClass: PyClass,
        pydanticVersion: KotlinVersion?,
        config: HashMap<String, Any?>,
        genericTypeMap: Map<PyGenericType, PyType>?,
        typed: Boolean = true,
        isDataclass: Boolean = false,
    ): PyCallableParameter? {
        if (!isValidField(field, context, pydanticVersion.isV2)) return null
        if (!hasAnnotationValue(field) && !field.hasAssignedValue()) return null // skip fields that are invalid syntax

        val defaultValueFromField =
            getDefaultValueForParameter(field, ellipsis, context, pydanticVersion, isDataclass)
        val defaultValue = when {
            isSubClassOfBaseSetting(pyClass, context) -> ellipsis
            else -> defaultValueFromField
        }

        val typeForParameter = when {
            !typed -> null
            // get type from default value
            !hasAnnotationValue(field) && defaultValueFromField is PyTypedElement -> context.getType(
                defaultValueFromField
            )
            // get type from annotation
            else -> getTypeForParameter(field, context)
        }?.let {
            when (genericTypeMap) {
                null -> it
                else -> PyTypeChecker.substitute(it, genericTypeMap, context)
            }
        }

        return PyCallableParameterImpl.nonPsi(
            getFieldName(field, context, config, pydanticVersion),
            typeForParameter,
            if (defaultValue == null && typeForParameter?.isNullable == true) ellipsis else defaultValue
        )
    }

    private fun argumentToParameter(
        parameter: PyNamedParameter,
        context: TypeEvalContext,
        typed: Boolean = true,
    ): PyCallableParameter? {
        val name = parameter.name ?: return null

        val typeForParameter = when {
            !typed -> null
            else -> parameter.getArgumentType(context)
        }

        return PyCallableParameterImpl.nonPsi(
            name,
            typeForParameter,
            parameter.defaultValue
        )
    }

    private fun dynamicModelFieldToParameter(
        field: PyExpression,
        context: TypeEvalContext,
        typed: Boolean = true,
    ): PyCallableParameter {
        var type: PyType? = null
        var defaultValue: PyExpression? = null
        when (val tupleValue = PsiTreeUtil.findChildOfType(field, PyTupleExpression::class.java)) {
            is PyTupleExpression -> {
                tupleValue.firstOrNull()?.let {
                    type = context.getType(it)?.pyClassTypes?.first()?.getReturnType(context)
                    defaultValue = it
                }
            }

            else -> {
                type = context.getType(field)
                defaultValue = (field as? PyKeywordArgument)?.valueExpression
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
            typeForParameter,
            defaultValue
        )
    }

    private fun getTypeForParameter(
        field: PyTargetExpression,
        context: TypeEvalContext,
    ): PyType? {

        return context.getType(field)
    }

    internal fun getDefaultValueForParameter(
        field: PyTargetExpression,
        ellipsis: PyNoneLiteralExpression,
        context: TypeEvalContext,
        pydanticVersion: KotlinVersion?,
        isDataclass: Boolean,
    ): PyExpression? {

        val value = field.findAssignedValue()
        if (value is PyExpression) {
            return getDefaultValueByAssignedValue(field, ellipsis, context, pydanticVersion, isDataclass)
        }
        val annotationValue = field.annotation?.value ?: return null

        fun parseAnnotation(pyExpression: PyExpression, context: TypeEvalContext): PyExpression? {
            val qualifiedName = getQualifiedName(pyExpression, context)
                ?: takeIf { isBitwiseOrUnionAvailable(pyExpression) }?.let {
                    pyExpression.children.filterIsInstance<PyNoneLiteralExpression>().run { return ellipsis }
                }
            when (qualifiedName) {
                ANY_Q_NAME -> return ellipsis
                OPTIONAL_Q_NAME -> return ellipsis
                UNION_Q_NAME -> pyExpression.children
                    .filterIsInstance<PyTupleExpression>()
                    .flatMap { it.children.toList() }
                    .filterIsInstance<PyNoneLiteralExpression>()
                    .firstOrNull()
                    ?.run { return ellipsis }

                ANNOTATED_Q_NAME -> return getFieldFromAnnotated(pyExpression, context)
                    ?.takeIf { it.arguments.any { arg -> arg.name == "default_factory" } }
                    ?: value
                    ?: getTypeExpressionFromAnnotated(pyExpression)?.let {
                        parseAnnotation(it, context)
                    }

                else -> return value
            }
            return value
        }

        return parseAnnotation(annotationValue, context)
    }


    private fun getDefaultValueByAssignedValue(
        field: PyTargetExpression,
        ellipsis: PyNoneLiteralExpression,
        context: TypeEvalContext,
        pydanticVersion: KotlinVersion?,
        isDataclass: Boolean,
    ): PyExpression? {
        val assignedValue = field.findAssignedValue() ?: return null

        if (assignedValue.text == "...") return null

        val callee = (assignedValue as? PyCallExpression)?.callee ?: return assignedValue
        val referenceExpression = callee.reference?.element as? PyReferenceExpression ?: return ellipsis

        val resolveResults = getResolvedPsiElements(referenceExpression, context)
        if (isDataclass) {
            resolveResults
                .any {
                    it.isDataclassField || it.isPydanticField
                }
                .let {
                    return when {
                        it -> getDefaultValueForDataclass(assignedValue, context)
                        else -> assignedValue
                    }
                }
        } else {

            val versionZero = pydanticVersion?.major == 0
            resolveResults
                .any {
                    when {
                        versionZero -> isPydanticSchemaByPsiElement(it, context)
                        else -> it.isPydanticField || it.isCustomModelField
                    }

                }
                .let {
                    return when {
                        it -> getDefaultValue(assignedValue, context)
                        else -> assignedValue
                    }
                }
        }
    }

    private fun getDefaultValue(assignedValue: PyCallExpression, typeEvalContext: TypeEvalContext): PyExpression? {
        getDefaultFactoryFromField(assignedValue)
            ?.let {
                return it
            }
        return getDefaultFromField(assignedValue, typeEvalContext)?.takeIf { it.text != "..." }
    }

    private fun getDefaultValueForDataclass(
        assignedValue: PyCallExpression,
        context: TypeEvalContext,
        argumentName: String?,
    ): PyExpression? {
        val defaultValue = if (argumentName is String) {
            assignedValue.getKeywordArgument(argumentName)
        } else {
            assignedValue.argumentList?.arguments?.firstOrNull().takeIf { it !is PyKeywordArgument }
        }
        return when {
            defaultValue == null -> null
            defaultValue.text == "..." -> null
            defaultValue is PyReferenceExpression -> {
                getResolvedPsiElements(defaultValue, context).any { it.isDataclassMissing }.let {
                    return when {
                        it -> null
                        else -> defaultValue
                    }
                }
            }

            else -> defaultValue
        }
    }

    private fun getDefaultValueForDataclass(
        assignedValue: PyCallExpression,
        context: TypeEvalContext,
    ): PyExpression? {
        val defaultValue = getDefaultValueForDataclass(assignedValue, context, "default")
        val defaultFactoryValue = getDefaultValueForDataclass(assignedValue, context, "default_factory")
        return when {
            defaultValue != null -> defaultValue
            defaultFactoryValue != null -> defaultFactoryValue
            else -> getDefaultValueForDataclass(assignedValue, context, null)
        }
    }
}
