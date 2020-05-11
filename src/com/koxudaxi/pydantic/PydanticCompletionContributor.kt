package com.koxudaxi.pydantic

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.completion.getTypeEvalContext
import com.jetbrains.python.documentation.PythonDocumentationProvider.getTypeHint
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyEvaluator
import com.jetbrains.python.psi.types.PyClassType
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

        abstract fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext, pydanticVersion: KotlinVersion?, config: HashMap<String, Any?>): String

        val typeProvider: PydanticTypeProvider = PydanticTypeProvider()

        val excludeFields: HashSet<String> = hashSetOf("Config")

        private fun getTypeText(pyClass: PyClass, typeEvalContext: TypeEvalContext,
                                pyTargetExpression: PyTargetExpression,
                                ellipsis: PyNoneLiteralExpression,
                                pydanticVersion: KotlinVersion?,
                                config: HashMap<String, Any?>,
                                isDataclass: Boolean): String {

            val parameter = typeProvider.fieldToParameter(pyTargetExpression, ellipsis, typeEvalContext, pyClass, pydanticVersion, config, isDataclass = isDataclass)
            val defaultValue = parameter?.defaultValue?.let {
                when {
                    parameter.defaultValue is PyNoneLiteralExpression && !isBaseSetting(pyClass, typeEvalContext) -> "=None"
                    else -> parameter.defaultValueText?.let { "=$it" } ?: ""
                }
            } ?: ""
            return getTypeHint(parameter?.getType(typeEvalContext), typeEvalContext)
                    .let { typeHint -> "${typeHint}$defaultValue ${pyClass.name}" }
        }

        private fun isInInit(field: PyTargetExpression): Boolean {
            val assignedValue = field.findAssignedValue() as? PyCallExpression ?: return true
            val initValue = assignedValue.getKeywordArgument("init") ?: return true
            return PyEvaluator.evaluateAsBoolean(initValue, true)
        }

        private fun addFieldElement(pyClass: PyClass, results: LinkedHashMap<String, LookupElement>,
                                    typeEvalContext: TypeEvalContext,
                                    ellipsis: PyNoneLiteralExpression,
                                    config: HashMap<String, Any?>,
                                    excludes: HashSet<String>?,
                                    isDataclass: Boolean) {
            val pydanticVersion = getPydanticVersion(pyClass.project, typeEvalContext)
            getClassVariables(pyClass, typeEvalContext)
                    .filter { it.name != null }
                    .filter { isValidFieldName(it.name!!) }
                    .filter { !isDataclass || isInInit(it) }
                    .forEach {
                        val elementName = getLookupNameFromFieldName(it, typeEvalContext, pydanticVersion, config)
                        if (excludes == null || !excludes.contains(elementName)) {
                            val element = PrioritizedLookupElement.withGrouping(
                                    LookupElementBuilder
                                            .createWithSmartPointer(elementName, it)
                                            .withTypeText(getTypeText(pyClass, typeEvalContext, it, ellipsis, pydanticVersion, config, isDataclass))
                                            .withIcon(icon), 1)
                            results[elementName] = PrioritizedLookupElement.withPriority(element, 100.0)
                        }
                    }
        }

        protected fun addAllFieldElement(parameters: CompletionParameters, result: CompletionResultSet,
                                         pyClass: PyClass, typeEvalContext: TypeEvalContext,
                                         ellipsis: PyNoneLiteralExpression,
                                         config: HashMap<String, Any?>,
                                         excludes: HashSet<String>? = null,
                                         isDataclass: Boolean) {

            val newElements: LinkedHashMap<String, LookupElement> = LinkedHashMap()

            pyClass.getAncestorClasses(typeEvalContext)
                    .filter { isPydanticModel(it, true) }
                    .forEach { addFieldElement(it, newElements, typeEvalContext, ellipsis, config, excludes, isDataclass) }

            addFieldElement(pyClass, newElements, typeEvalContext, ellipsis, config, excludes, isDataclass)

            result.runRemainingContributors(parameters)
            { completionResult ->
                completionResult.lookupElement.lookupString
                        .takeIf { name -> !newElements.containsKey(name) && (excludes == null || !excludes.contains(name)) }
                        ?.let { result.passResult(completionResult) }
            }
            result.addAllElements(newElements.values)
        }

        protected fun removeAllFieldElement(parameters: CompletionParameters, result: CompletionResultSet,
                                            pyClass: PyClass, typeEvalContext: TypeEvalContext,
                                            excludes: HashSet<String>) {

            if (!isPydanticModel(pyClass, true)) return

            val fieldElements: HashSet<String> = HashSet()

            pyClass.getAncestorClasses(typeEvalContext)
                    .filter { isPydanticModel(it, true) }
                    .forEach {
                        fieldElements.addAll(it.classAttributes
                                .filter { attribute
                                    ->
                                    attribute.name?.let { name -> isValidFieldName(name) } ?: false
                                }
                                .mapNotNull { attribute -> attribute?.name })
                    }



            fieldElements.addAll(pyClass.classAttributes
                    .filter { attribute
                        ->
                        attribute.name?.let { name -> isValidFieldName(name) } ?: false
                    }
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
        override fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext, pydanticVersion: KotlinVersion?, config: HashMap<String, Any?>): String {
            return "${getFieldName(field, context, config, pydanticVersion)}="
        }

        override val icon: Icon = AllIcons.Nodes.Parameter

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            if (parameters.position.text == "." || parameters.position.prevSibling?.text == ".") return
            val pyArgumentList = parameters.position.parent?.parent as? PyArgumentList ?: return

            val typeEvalContext = parameters.getTypeEvalContext()
            val pyClassType = (pyArgumentList.parent as? PyCallExpression)?.let { typeEvalContext.getType(it) } as? PyClassType
                    ?: return

            if (!isPydanticModel(pyClassType.pyClass, true, typeEvalContext)) return

            val definedSet = pyArgumentList.children
                    .mapNotNull { (it as? PyKeywordArgument)?.name }
                    .map { "${it}=" }
                    .toHashSet()
            val config = getConfig(pyClassType.pyClass, typeEvalContext, true)
            val ellipsis = PyElementGenerator.getInstance(pyClassType.pyClass.project).createEllipsis()
            addAllFieldElement(parameters, result, pyClassType.pyClass, typeEvalContext, ellipsis, config, definedSet, isPydanticDataclass(pyClassType.pyClass))
        }
    }

    private object FieldCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext, pydanticVersion: KotlinVersion?, config: HashMap<String, Any?>): String {
            return field.name!!
        }

        override val icon: Icon = AllIcons.Nodes.Field

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val typeEvalContext = parameters.getTypeEvalContext()
            val pyType = (parameters.position.parent?.firstChild as? PyTypedElement)?.let { typeEvalContext.getType(it) }
                    ?: return

            val pyClassType = getPyClassTypeByPyTypes(pyType).firstOrNull { isPydanticModel(it.pyClass, true) }
                    ?: return
            if (pyClassType.isDefinition) { // class
                removeAllFieldElement(parameters, result, pyClassType.pyClass, typeEvalContext, excludeFields)
                return
            }
            val config = getConfig(pyClassType.pyClass, typeEvalContext, true)
            val ellipsis = PyElementGenerator.getInstance(pyClassType.pyClass.project).createEllipsis()
            addAllFieldElement(parameters, result, pyClassType.pyClass, typeEvalContext, ellipsis, config, isDataclass = isPydanticDataclass(pyClassType.pyClass))
        }
    }

    private object ConfigCompletionProvider : PydanticConfigCompletionProvider() {

        override val icon: Icon = AllIcons.Nodes.Field

        private fun getConfigAttributeAllElements(configClass: PyClass,
                                                  excludes: HashSet<String>?): LinkedHashMap<String, LookupElement> {

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


        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val configClass = getPyClassByAttribute(parameters.position.parent?.parent) ?: return
            if (!isConfigClass(configClass)) return
            val pydanticModel = getPyClassByAttribute(configClass) ?: return
            if (!isPydanticModel(pydanticModel, true)) return
            val typeEvalContext = parameters.getTypeEvalContext()

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

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val pydanticModel = getPyClassByAttribute(parameters.position.parent?.parent) ?: return
            if (!isPydanticModel(pydanticModel, true)) return
            if (pydanticModel.findNestedClass("Config", false) != null) return
            val element = PrioritizedLookupElement.withGrouping(
                    LookupElementBuilder
                            .create("class Config:")
                            .withIcon(icon), 1)
            result.addElement(PrioritizedLookupElement.withPriority(element, 100.0))
        }
    }

}
