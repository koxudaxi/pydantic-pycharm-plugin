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
import com.jetbrains.python.psi.types.*
import com.koxudaxi.pydantic.PydanticConfigService.Companion.getInstance
import one.util.streamex.StreamEx

class PydanticTypeProvider : PyTypeProviderBase() {
    private val pyTypingTypeProvider = PyTypingTypeProvider()
    override fun getReferenceExpressionType(
        referenceExpression: PyReferenceExpression,
        context: TypeEvalContext,
    ): PyType? {
        return getPydanticTypeForCallee(referenceExpression, context)
    }

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
        if (referenceTarget !is PyTargetExpression) return null
        val pyClass = getPyClassByAttribute(referenceTarget.parent) ?: return null
        if (!isPydanticModel(pyClass, false, context)) return null
        val name = referenceTarget.name ?: return null
        return getRefTypeFromFieldName(name, context, pyClass)
    }

    override fun getParameterType(param: PyNamedParameter, func: PyFunction, context: TypeEvalContext): Ref<PyType>? {
        return when {
            !param.isPositionalContainer && !param.isKeywordContainer && param.annotationValue == null && func.name == PyNames.INIT -> {
                val pyClass = func.containingClass ?: return null
                if (!isPydanticModel(pyClass, false, context)) return null
                val name = param.name ?: return null
                getRefTypeFromFieldName(name, context, pyClass)
            }

            param.isSelf && func.isValidatorMethod -> {
                val pyClass = func.containingClass ?: return null
                if (!isPydanticModel(pyClass, false, context)) return null
                Ref.create(context.getType(pyClass))
            }

            else -> null
        }
    }

    private fun getRefTypeFromFieldNameInPyClass(
        name: String,
        pyClass: PyClass,
        context: TypeEvalContext,
        ellipsis: PyNoneLiteralExpression,
        pydanticVersion: KotlinVersion?,
    ): Ref<PyType>? {
        return pyClass.findClassAttribute(name, false, context)
            ?.let { return getRefTypeFromField(it, ellipsis, context, pyClass, pydanticVersion) }
    }

    private fun getRefTypeFromFieldName(name: String, context: TypeEvalContext, pyClass: PyClass): Ref<PyType>? {
        val ellipsis = PyElementGenerator.getInstance(pyClass.project).createEllipsis()

        val pydanticVersion = PydanticCacheService.getVersion(pyClass.project, context)
        return getRefTypeFromFieldNameInPyClass(name, pyClass, context, ellipsis, pydanticVersion)
            ?: getAncestorPydanticModels(pyClass, false, context).firstNotNullOfOrNull { ancestor ->
                getRefTypeFromFieldNameInPyClass(name, ancestor, context, ellipsis, pydanticVersion)
            }
    }

    private fun getRefTypeFromField(
        pyTargetExpression: PyTargetExpression, ellipsis: PyNoneLiteralExpression,
        context: TypeEvalContext, pyClass: PyClass,
        pydanticVersion: KotlinVersion?,
    ): Ref<PyType>? {
        return fieldToParameter(
            pyTargetExpression,
            ellipsis,
            context,
            pyClass,
            pydanticVersion,
            getConfig(pyClass, context, true),
            getGenericTypeMap(pyClass, context)
        )
            ?.let { parameter -> Ref.create(parameter.getType(context)) }
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


    private fun collectGenericTypes(pyClass: PyClass, context: TypeEvalContext): List<PyGenericType?> {
        return pyClass.superClassExpressions
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
                    rootOperandType is PyClassType && isSubClassOfPydanticGenericModel(rootOperandType.pyClass, context)
                if (!isGenericModel && (rootOperandType as? PyCustomType)?.classQName != GENERIC_Q_NAME) return@flatMap emptyList()

                when (val indexExpression = pySubscriptionExpression.indexExpression) {
                    is PyTupleExpression -> indexExpression.elements.map { context.getType(it) }.toList()
                    is PyTypedElement -> listOf(context.getType(indexExpression))
                    else -> null
                } ?: emptyList()
            }.filterIsInstance<PyGenericType>().distinct()
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

    private fun getPydanticTypeForCallee(
        referenceExpression: PyReferenceExpression,
        context: TypeEvalContext,
    ): PyType? {
        val pyCallExpression = PyCallExpressionNavigator.getPyCallExpressionByCallee(referenceExpression) ?: return null

        return getResolvedPsiElements(referenceExpression, context)
            .asSequence()
            .mapNotNull {
                when {
                    it is PyClass -> getPydanticTypeForClass(it, context, true, pyCallExpression)
                    it is PyParameter && it.isSelf ->
                        PsiTreeUtil.getParentOfType(it, PyFunction::class.java)
                            ?.takeIf { pyFunction -> pyFunction.modifier == PyFunction.Modifier.CLASSMETHOD }
                            ?.containingClass?.let { pyClass ->
                                getPydanticTypeForClass(
                                    pyClass,
                                    context,
                                    true,
                                    pyCallExpression
                                )
                            }

                    it is PyNamedParameter -> it.getArgumentType(context)?.pyClassTypes?.filter { pyClassType ->
                        pyClassType.isDefinition
                    }?.map { filteredPyClassType ->
                        getPydanticTypeForClass(
                            filteredPyClassType.pyClass,
                            context,
                            true,
                            pyCallExpression
                        )
                    }?.firstOrNull()

                    it is PyTargetExpression -> (it as? PyTypedElement)
                        ?.let { pyTypedElement ->
                            context.getType(pyTypedElement)?.pyClassTypes
                                ?.filter { pyClassType -> pyClassType.isDefinition }
                                ?.filterNot { pyClassType -> pyClassType is PydanticDynamicModelClassType }
                                ?.map { filteredPyClassType ->
                                    getPydanticTypeForClass(
                                        filteredPyClassType.pyClass,
                                        context,
                                        true,
                                        pyCallExpression
                                    )
                                }?.firstOrNull()
                        } ?: getPydanticDynamicModelTypeForTargetExpression(it, context)?.pyCallableType

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
        val referenceExpression = (pyCallExpression.callee as? PyReferenceExpression) ?: return null
        val pyFunction = getResolvedPsiElements(referenceExpression, context)
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
        val pydanticVersion = PydanticCacheService.getVersion(pyFunction.project, context)
        val collected = linkedMapOf<String, PydanticDynamicModel.Attribute>()
        val newVersion = pydanticVersion == null || pydanticVersion.isAtLeast(1, 5)
        val modelNameParameterName = if (newVersion) "__model_name" else "model_name"

        val keywordArguments: Map<String, PyExpression> = pyArguments
            .filter { it is PyKeywordArgument || (it as? PyStarArgumentImpl)?.isKeyword == true }
            .map { it.name to it }
            .filterIsInstance<Pair<String, PyExpression>>()
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
                                .map { it to fieldToParameter(it, context, typed) }
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
            .filter { (name, _) -> name.isValidFieldName && !name.startsWith('_') }
            .filter { (name, _) -> (newVersion || name != "model_name") }
            .map { (name, field) ->
                val parameter = fieldToParameter(field, context, typed)
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
                .mapIndexed { index, genericType -> genericType to injectedTypes[index] }
                .filterIsInstance<Pair<PyGenericType, PyType>>().toMap()
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
                    ?.map { parameter -> parameter.name to parameter }
                    ?.filterIsInstance<Pair<String, PyCallableParameter>>()
                    ?.let { collected.putAll(it) }
            }
        }
        val genericTypeMap = getGenericTypeMap(pyClass, context, pyCallExpression)
        val pydanticVersion = PydanticCacheService.getVersion(pyClass.project, context)
        val config = getConfig(pyClass, context, true)
        for (currentType in StreamEx.of(clsType).append(pyClass.getAncestorTypes(context))) {
            if (currentType !is PyClassType) continue

            val current = currentType.pyClass
            if (!isPydanticModel(current, false, context)) continue

            getClassVariables(current, context)
                .filterNot { isUntouchedClass(it.findAssignedValue(), config, context) }
                .mapNotNull {
                    fieldToParameter(
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


    internal fun fieldToParameter(
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
        if (!isValidField(field, context)) return null
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
            defaultValue
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

    private fun fieldToParameter(
        field: PyExpression,
        context: TypeEvalContext,
        typed: Boolean = true,
    ): PyCallableParameter {
        var type: PyType? = null
        var defaultValue: PyExpression? = null
        when (val tupleValue = PsiTreeUtil.findChildOfType(field, PyTupleExpression::class.java)) {
            is PyTupleExpression -> {
                tupleValue.toList().let {
                    if (it.size > 1) {
                        type = getPyTypeFromPyExpression(it[0], context)
                        defaultValue = it[1]
                    }
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
                        it -> getDefaultValue(assignedValue, context, ellipsis)
                        else -> assignedValue
                    }
                }
        }
    }

    private fun getDefaultValue(assignedValue: PyCallExpression, ellipsis: PyNoneLiteralExpression, typeEvalContext: TypeEvalContext): PyExpression? {
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

    internal fun injectDefaultValue(
        pyClass: PyClass,
        pyCallableParameter: PyCallableParameter,
        ellipsis: PyNoneLiteralExpression,
        pydanticVersion: KotlinVersion?,
        context: TypeEvalContext
    ): PyCallableParameter? {
        val name = pyCallableParameter.name ?: return null
        val attribute = pyClass.findClassAttribute(name, true, context) ?: return null
        val defaultValue =
            getDefaultValueByAssignedValue(attribute, ellipsis, context, pydanticVersion, true)
        return PyCallableParameterImpl.nonPsi(
            name,
            pyCallableParameter.getArgumentType(context),
            defaultValue
        )
    }

}
