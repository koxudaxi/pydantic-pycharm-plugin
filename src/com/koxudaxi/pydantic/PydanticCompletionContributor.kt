package com.koxudaxi.pydantic

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyNames
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.completion.getTypeEvalContext
import com.jetbrains.python.documentation.PythonDocumentationProvider.getTypeHint
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyEvaluator
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.PyGenericType
import com.jetbrains.python.psi.types.PyType
import com.jetbrains.python.psi.types.TypeEvalContext
import javax.swing.Icon


class PydanticCompletionContributor : CompletionContributor() {
    init {
        extend(CompletionType.BASIC,
            psiElement(PyTokenTypes.IDENTIFIER).withParents(
                PyReferenceExpression::class.java,
                PyArgumentList::class.java,
                PyCallExpression::class.java),
            KeywordArgumentCompletionProvider)
        extend(CompletionType.BASIC,
            psiElement(PyTokenTypes.IDENTIFIER)
                .afterLeaf(psiElement(PyTokenTypes.DOT))
                .withParent(psiElement(PyReferenceExpression::class.java)),
            FieldCompletionProvider)
        extend(CompletionType.BASIC,
            psiElement(PyTokenTypes.IDENTIFIER).withParents(
                PyReferenceExpression::class.java,
                PyExpressionStatement::class.java,
                PyStatementList::class.java,
                PyClass::class.java,
                PyStatementList::class.java,
                PyClass::class.java),
            ConfigCompletionProvider)
        extend(CompletionType.BASIC,
            psiElement(PyTokenTypes.IDENTIFIER).withParents(
                PyReferenceExpression::class.java,
                PyExpressionStatement::class.java,
                PyStatementList::class.java,
                PyClass::class.java),
            ConfigClassCompletionProvider)
    }

    private abstract class PydanticCompletionProvider : CompletionProvider<CompletionParameters>() {

        abstract val icon: Icon

        abstract fun getLookupNameFromFieldName(
            field: PyTargetExpression,
            context: TypeEvalContext,
            pydanticVersion: KotlinVersion?,
            config: HashMap<String, Any?>,
        ): String

        val typeProvider: PydanticTypeProvider = PydanticTypeProvider()

        val excludeFields: HashSet<String> = hashSetOf("Config")

        private fun getTypeText(
            pyClass: PyClass, typeEvalContext: TypeEvalContext,
            pyTargetExpression: PyTargetExpression,
            ellipsis: PyNoneLiteralExpression,
            pydanticVersion: KotlinVersion?,
            config: HashMap<String, Any?>,
            isDataclass: Boolean,
            genericTypeMap: Map<PyGenericType, PyType>?,
        ): String? {

            val parameter = typeProvider.fieldToParameter(pyTargetExpression,
                ellipsis,
                typeEvalContext,
                pyClass,
                pydanticVersion,
                config,
                genericTypeMap,
                isDataclass = isDataclass)
            val parameterName = parameter?.name ?: return null
            if (!PyNames.isIdentifier(parameterName)) return null
            val defaultValue = parameter.defaultValue?.let {
                when {
                    parameter.defaultValue is PyNoneLiteralExpression && !isSubClassOfBaseSetting(pyClass,
                        typeEvalContext) -> "=None"
                    else -> parameter.defaultValueText?.let { "=$it" } ?: ""
                }
            } ?: ""
            return getTypeHint(parameter.getType(typeEvalContext), typeEvalContext)
                .let { typeHint -> "${typeHint}$defaultValue ${pyClass.name}" }
        }

        private fun isInInit(field: PyTargetExpression): Boolean {
            val assignedValue = field.findAssignedValue() as? PyCallExpression ?: return true
            val initValue = assignedValue.getKeywordArgument("init") ?: return true
            return PyEvaluator.evaluateAsBoolean(initValue, true)
        }

        private fun addFieldElement(
            pyClass: PyClass, results: LinkedHashMap<String, LookupElement>,
            typeEvalContext: TypeEvalContext,
            ellipsis: PyNoneLiteralExpression,
            config: HashMap<String, Any?>,
            excludes: HashSet<String>?,
            isDataclass: Boolean,
            genericTypeMap: Map<PyGenericType, PyType>?,
        ) {
            val pydanticVersion = PydanticCacheService.getVersion(pyClass.project, typeEvalContext)
            getClassVariables(pyClass, typeEvalContext)
                .filter { it.name != null }
                .filterNot { isUntouchedClass(it.findAssignedValue(), config, typeEvalContext) }
                .filter { isValidField(it, typeEvalContext) }
                .filter { !isDataclass || isInInit(it) }
                .forEach {
                    val elementName = getLookupNameFromFieldName(it, typeEvalContext, pydanticVersion, config)
                    if (excludes == null || !excludes.contains(elementName)) {
                        val typeText = getTypeText(pyClass,
                            typeEvalContext,
                            it,
                            ellipsis,
                            pydanticVersion,
                            config,
                            isDataclass,
                            genericTypeMap)
                        if (typeText is String) {
                            val element = PrioritizedLookupElement.withGrouping(
                                LookupElementBuilder
                                    .createWithSmartPointer(elementName, it)
                                    .withTypeText(typeText)
                                    .withIcon(icon), 1)
                            results[elementName] = PrioritizedLookupElement.withPriority(element, 100.0)
                        }
                    }
                }
        }

        protected fun addAllFieldElement(
            parameters: CompletionParameters, result: CompletionResultSet,
            pyClass: PyClass, typeEvalContext: TypeEvalContext,
            ellipsis: PyNoneLiteralExpression,
            config: HashMap<String, Any?>,
            genericTypeMap: Map<PyGenericType, PyType>?,
            excludes: HashSet<String>? = null,
            isDataclass: Boolean,
        ) {

            val newElements: LinkedHashMap<String, LookupElement> = LinkedHashMap()

            getAncestorPydanticModels(pyClass, true, typeEvalContext)
                .forEach {
                    addFieldElement(it,
                        newElements,
                        typeEvalContext,
                        ellipsis,
                        config,
                        excludes,
                        isDataclass,
                        genericTypeMap)
                }

            addFieldElement(pyClass,
                newElements,
                typeEvalContext,
                ellipsis,
                config,
                excludes,
                isDataclass,
                genericTypeMap)

            result.runRemainingContributors(parameters)
            { completionResult ->
                completionResult.lookupElement.lookupString
                    .takeIf { name -> !newElements.containsKey(name) && (excludes == null || !excludes.contains(name)) }
                    ?.let { result.passResult(completionResult) }
            }
            result.addAllElements(newElements.values)
        }

        protected fun removeAllFieldElement(
            parameters: CompletionParameters, result: CompletionResultSet,
            pyClass: PyClass, typeEvalContext: TypeEvalContext,
            excludes: HashSet<String>, config: HashMap<String, Any?>,
        ) {

            if (!isPydanticModel(pyClass, true, typeEvalContext)) return

            val fieldElements: HashSet<String> = HashSet()

            getAncestorPydanticModels(pyClass, true, typeEvalContext)
                .forEach {
                    fieldElements.addAll(it.classAttributes
                        .filterNot { attribute ->
                            isUntouchedClass(attribute.findAssignedValue(),
                                config,
                                typeEvalContext)
                        }
                        .filter { attribute ->
                            isValidField(attribute, typeEvalContext)
                        }
                        .mapNotNull { attribute -> attribute?.name })
                }



            fieldElements.addAll(pyClass.classAttributes
                .filterNot { isUntouchedClass(it.findAssignedValue(), config, typeEvalContext) }
                .filter { isValidField(it, typeEvalContext) }
                .mapNotNull { attribute -> attribute?.name })

            result.runRemainingContributors(parameters)
            { completionResult ->
                when (AllIcons.Nodes.Field) {
                    completionResult.lookupElement.psiElement?.getIcon(0) -> {
                        completionResult.lookupElement.lookupString
                            .takeIf { name -> !fieldElements.contains(name) && (!excludes.contains(name)) }
                            ?.let { result.passResult(completionResult) }
                    }
                    else -> result.passResult(completionResult)
                }
            }
        }
    }

    private abstract class PydanticConfigCompletionProvider : CompletionProvider<CompletionParameters>() {

        abstract val icon: Icon
    }

    private object KeywordArgumentCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(
            field: PyTargetExpression,
            context: TypeEvalContext,
            pydanticVersion: KotlinVersion?,
            config: HashMap<String, Any?>,
        ): String {
            return "${getFieldName(field, context, config, pydanticVersion)}="
        }

        override val icon: Icon = AllIcons.Nodes.Parameter

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet,
        ) {
            if (parameters.position.text == "." || parameters.position.prevSibling?.text == ".") return
            val pyArgumentList = parameters.position.parent?.parent as? PyArgumentList ?: return

            val typeEvalContext = parameters.getTypeEvalContext()
            val pyCallExpression = pyArgumentList.parent as? PyCallExpression
            val pyClass =
                pyCallExpression?.let { (typeEvalContext.getType(it) as? PyClassType)?.pyClass }
                    ?: return

            if (!isPydanticModel(pyClass, true, typeEvalContext)) return
            if (getPydanticModelInit(pyClass, typeEvalContext) is PyFunction) return

            val definedSet = pyArgumentList.children
                .mapNotNull { (it as? PyKeywordArgument)?.name }
                .map { "${it}=" }
                .toHashSet()

            addAllFieldElement(
                parameters,
                result,
                pyClass,
                typeEvalContext,
                PyElementGenerator.getInstance(pyClass.project).createEllipsis(),
                getConfig(pyClass, typeEvalContext, true),
                typeProvider.getGenericTypeMap(pyClass, typeEvalContext, pyCallExpression),
                definedSet,
                pyClass.isPydanticDataclass,
            )
        }
    }

    private object FieldCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(
            field: PyTargetExpression,
            context: TypeEvalContext,
            pydanticVersion: KotlinVersion?,
            config: HashMap<String, Any?>,
        ): String {
            return field.name!!
        }

        override val icon: Icon = AllIcons.Nodes.Field

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet,
        ) {
            val typeEvalContext = parameters.getTypeEvalContext()
            val pyTypedElement = parameters.position.parent?.firstChild as? PyTypedElement ?: return

            val pyType = typeEvalContext.getType(pyTypedElement) ?: return

            val pyClassType =
                pyType.pyClassTypes.firstOrNull { isPydanticModel(it.pyClass, true, typeEvalContext) }
                    ?: return
            val pyClass = pyClassType.pyClass
            val config = getConfig(pyClass, typeEvalContext, true)
            if (pyClassType.isDefinition) { // class
                removeAllFieldElement(parameters, result, pyClass, typeEvalContext, excludeFields, config)
                return
            }
            val ellipsis = PyElementGenerator.getInstance(pyClass.project).createEllipsis()
            addAllFieldElement(
                parameters,
                result,
                pyClass,
                typeEvalContext,
                ellipsis,
                config,
                typeProvider.getGenericTypeMap(pyClass, typeEvalContext, pyTypedElement as? PyCallExpression),
                isDataclass = pyClass.isPydanticDataclass,
            )
        }
    }

    private object ConfigCompletionProvider : PydanticConfigCompletionProvider() {

        override val icon: Icon = AllIcons.Nodes.Field

        private fun getConfigAttributeAllElements(
            configClass: PyClass,
            excludes: HashSet<String>?,
        ): LinkedHashMap<String, LookupElement> {

            val newElements: LinkedHashMap<String, LookupElement> = LinkedHashMap()

            configClass.classAttributes
                .asReversed()
                .asSequence()
                .filter { it.name != null && it.hasAssignedValue() }
                .forEach {
                    val elementName = it.name!!
                    val assignedValue = it.findAssignedValue()!!
                    if (excludes == null || !excludes.contains(elementName)) {
                        val element = PrioritizedLookupElement.withGrouping(
                            LookupElementBuilder
                                .createWithSmartPointer("$elementName = ${assignedValue.text}", it)
                                .withTypeText(configClass.name)
                                .withIcon(icon), 1)
                        newElements[elementName] = PrioritizedLookupElement.withPriority(element, 100.0)
                    }
                }
            return newElements
        }


        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet,
        ) {
            val configClass = getPyClassByAttribute(parameters.position.parent?.parent) ?: return
            if (configClass.isConfigClass != true) return
            val pydanticModel = getPyClassByAttribute(configClass) ?: return
            val typeEvalContext = parameters.getTypeEvalContext()
            if (!isPydanticModel(pydanticModel, true, typeEvalContext)) return


            val definedSet = configClass.classAttributes
                .mapNotNull { it.name }
                .toHashSet()

            val project = configClass.project
            val baseConfig = getPydanticBaseConfig(project, typeEvalContext) ?: return

            val results = getConfigAttributeAllElements(baseConfig, definedSet)
            result.runRemainingContributors(parameters, false)
            result.addAllElements(results.values)
        }
    }

    private object ConfigClassCompletionProvider : PydanticConfigCompletionProvider() {

        override val icon: Icon = AllIcons.Nodes.Class

        override fun addCompletions(
            parameters: CompletionParameters,
            context: ProcessingContext,
            result: CompletionResultSet,
        ) {
            val pydanticModel = getPyClassByAttribute(parameters.position.parent?.parent) ?: return
            val typeEvalContext = parameters.getTypeEvalContext()
            if (!isPydanticModel(pydanticModel, true, typeEvalContext)) return
            if (pydanticModel.findNestedClass("Config", false) != null) return
            val element = PrioritizedLookupElement.withGrouping(
                LookupElementBuilder
                    .create("class Config:")
                    .withIcon(icon), 1)
            result.addElement(PrioritizedLookupElement.withPriority(element, 100.0))
        }
    }

}
