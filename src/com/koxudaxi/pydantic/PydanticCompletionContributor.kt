package com.koxudaxi.pydantic

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.completion.getTypeEvalContext
import com.jetbrains.python.documentation.PythonDocumentationProvider.getTypeHint
import com.jetbrains.python.psi.*
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
    }

    private abstract class PydanticCompletionProvider : CompletionProvider<CompletionParameters>() {

        abstract val icon: Icon

        abstract fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext, pydanticVersion: KotlinVersion?, config: HashMap<String, String?>): String

        val typeProvider: PydanticTypeProvider = PydanticTypeProvider()

        val excludeFields: HashSet<String> = hashSetOf("Config")

        private fun getTypeText(pyClass: PyClass, typeEvalContext: TypeEvalContext,
                                pyTargetExpression: PyTargetExpression,
                                ellipsis: PyNoneLiteralExpression,
                                pydanticVersion: KotlinVersion?,
                                config: HashMap<String, String?>): String {

            val parameter = typeProvider.fieldToParameter(pyTargetExpression, ellipsis, typeEvalContext, pyClass, pydanticVersion, config)
            val defaultValue = parameter?.defaultValue?.let {
                if (parameter.defaultValue is PyNoneLiteralExpression && !isBaseSetting(pyClass, typeEvalContext)) {
                    "=None"
                } else {
                    "=${parameter.defaultValueText}"
                }
            } ?: ""
            val typeHint = getTypeHint(parameter?.getType(typeEvalContext), typeEvalContext)
            return "${typeHint}$defaultValue ${pyClass.name}"
        }


        private fun addFieldElement(pyClass: PyClass, results: LinkedHashMap<String, LookupElement>,
                                    typeEvalContext: TypeEvalContext,
                                    ellipsis: PyNoneLiteralExpression,
                                    config: HashMap<String, String?>,
                                    excludes: HashSet<String>?) {
            val pydanticVersion = getPydanticVersion(pyClass.project, typeEvalContext)
            getClassVariables(pyClass, typeEvalContext)
                    .filter { it.name != null }
                    .filter {isValidFieldName(it.name!!)}
                    .forEach {
                        val elementName = getLookupNameFromFieldName(it, typeEvalContext, pydanticVersion, config)
                        if (excludes == null || !excludes.contains(elementName)) {
                            val element = PrioritizedLookupElement.withGrouping(
                                    LookupElementBuilder
                                            .createWithSmartPointer(elementName, it)
                                            .withTypeText(getTypeText(pyClass, typeEvalContext, it, ellipsis, pydanticVersion, config))
                                            .withIcon(icon), 1)
                            results[elementName] = PrioritizedLookupElement.withPriority(element, 100.0)
                        }
                    }
        }

        protected fun addAllFieldElement(parameters: CompletionParameters, result: CompletionResultSet,
                                         pyClass: PyClass, typeEvalContext: TypeEvalContext,
                                         ellipsis: PyNoneLiteralExpression,
                                         config: HashMap<String, String?>,
                                         excludes: HashSet<String>? = null) {

            val newElements: LinkedHashMap<String, LookupElement> = LinkedHashMap()

            pyClass.getAncestorClasses(typeEvalContext)
                    .filter { isPydanticModel(it) }
                    .forEach { addFieldElement(it, newElements, typeEvalContext, ellipsis, config, excludes) }

            addFieldElement(pyClass, newElements, typeEvalContext, ellipsis, config, excludes)

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

            if (!isPydanticModel(pyClass)) return

            val fieldElements: HashSet<String> = HashSet()

            pyClass.getAncestorClasses(typeEvalContext)
                    .filter { isPydanticModel(it) }
                    .forEach { fieldElements.addAll(it.classAttributes
                            .filter {attribute
                                -> attribute.name?.let { name -> isValidFieldName(name)} ?: false}
                            .mapNotNull { attribute -> attribute?.name }) }



            fieldElements.addAll(pyClass.classAttributes
                    .filter {attribute
                        -> attribute.name?.let { name -> isValidFieldName(name)} ?: false}
                    .mapNotNull { attribute -> attribute?.name })

            result.runRemainingContributors(parameters)
            { completionResult ->
                if (completionResult.lookupElement.psiElement?.getIcon(0) == AllIcons.Nodes.Field) {
                    completionResult.lookupElement.lookupString
                            .takeIf { name -> !fieldElements.contains(name) && (!excludes.contains(name)) }
                            ?.let { result.passResult(completionResult) }
                } else {
                    result.passResult(completionResult)
                }
            }
        }
    }

    private object KeywordArgumentCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext, pydanticVersion: KotlinVersion?, config: HashMap<String, String?>): String {
            return "${getFieldName(field, context, config, pydanticVersion)}="
        }

        override val icon: Icon = AllIcons.Nodes.Parameter

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val pyArgumentList = parameters.position.parent?.parent as? PyArgumentList ?: return
            val typeEvalContext = parameters.getTypeEvalContext()
            val pyClassType = (pyArgumentList.parent as? PyCallExpression)?.let{typeEvalContext.getType(it)} as? PyClassType
                    ?: return

            if (!isPydanticModel(pyClassType.pyClass, typeEvalContext)) return

            val definedSet = pyArgumentList.children
                    .mapNotNull { (it as? PyKeywordArgument)?.name }
                    .map { "${it}=" }
                    .toHashSet()
            val config = getConfig(pyClassType.pyClass, typeEvalContext, true)
            val ellipsis = PyElementGenerator.getInstance(pyClassType.pyClass.project).createEllipsis()
            addAllFieldElement(parameters, result, pyClassType.pyClass, typeEvalContext, ellipsis, config, definedSet)
        }
    }

    private object FieldCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext, pydanticVersion: KotlinVersion?, config: HashMap<String, String?>): String {
            return field.name!!
        }

        override val icon: Icon = AllIcons.Nodes.Field

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val typeEvalContext = parameters.getTypeEvalContext()
            val pyType = (parameters.position.parent?.firstChild as? PyTypedElement)?.let { typeEvalContext.getType(it)} ?: return

            val pyClassType = getPyClassTypeByPyTypes(pyType).firstOrNull { isPydanticModel(it.pyClass) } ?: return
            if (pyClassType.isDefinition) { // class
                removeAllFieldElement(parameters, result, pyClassType.pyClass, typeEvalContext, excludeFields)
                return
            }
            val config = getConfig(pyClassType.pyClass, typeEvalContext, true)
            val ellipsis = PyElementGenerator.getInstance(pyClassType.pyClass.project).createEllipsis()
            addAllFieldElement(parameters, result, pyClassType.pyClass, typeEvalContext, ellipsis, config)
        }
    }

    private object ConfigCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext, pydanticVersion: KotlinVersion?, config: HashMap<String, String?>): String {
            return "${getFieldName(field, context, config, pydanticVersion)}="
        }

        override val icon: Icon = AllIcons.Nodes.Field

        private fun getConfigAttributeAllElements(configClass: PyClass,
                                    typeEvalContext: TypeEvalContext,
                                    excludes: HashSet<String>?) : LinkedHashMap<String, LookupElement> {

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
            val configClass = parameters.position.parent?.parent?.parent?.parent as? PyClass ?: return
            if (!isConfigClass(configClass)) return
            val pydanticModel = configClass.parent?.parent as? PyClass ?:return
            if (!isPydanticModel(pydanticModel)) return
            val typeEvalContext = parameters.getTypeEvalContext()

            val definedSet = configClass.getClassAttributesInherited(typeEvalContext)
                    .mapNotNull { it.name }
                    .toHashSet()

            val project = configClass.project
            val baseConfig = getPydanticBaseConfig(project, typeEvalContext) ?: return

            val results = getConfigAttributeAllElements(baseConfig, typeEvalContext, definedSet)
            result.runRemainingContributors(parameters,false)
            result.addAllElements(results.values)
        }
    }

}
