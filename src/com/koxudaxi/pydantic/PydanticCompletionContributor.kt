package com.koxudaxi.pydantic

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.completion.getTypeEvalContext
import com.jetbrains.python.documentation.PythonDocumentationProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.PyUnionType
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
    }

    private abstract class PydanticCompletionProvider : CompletionProvider<CompletionParameters>() {

        abstract val icon: Icon

        abstract fun getLookupNameFromFieldName(fieldName: String): String


        private fun getTypeHint(pyClass: PyClass, typeEvalContext: TypeEvalContext, pyTargetExpression: PyTargetExpression): String {
            val defaultValue = pyTargetExpression.findAssignedValue()?.text?.let { "=$it" } ?: ""
            val typeHint = PythonDocumentationProvider.getTypeHint(typeEvalContext.getType(pyTargetExpression), typeEvalContext)
            return "${typeHint}$defaultValue ${pyClass.name}"
        }

        private fun getPyClassFromPyNamedParameter(pyNamedParameter: PyNamedParameter, typeEvalContext: TypeEvalContext): PyClass? {
            return when (val pyClassTypes = pyNamedParameter.getArgumentType(typeEvalContext)) {
                is PyClassType -> pyClassTypes.pyClass
                is PyUnionType -> pyClassTypes.members.filterIsInstance<PyClassType>()
                        .map { pyClassType -> pyClassType.pyClass }
                        .firstOrNull()
                else -> null
            }
        }

        protected fun getPyClassByPyReferenceExpression(pyReferenceExpression: PyReferenceExpression, typeEvalContext: TypeEvalContext): PyClass? {
            val resolveContext = PyResolveContext.defaultContext().withTypeEvalContext(typeEvalContext)
            return pyReferenceExpression.multiFollowAssignmentsChain(resolveContext).mapNotNull {
                return when (val resolveElement = it.element) {
                    is PyClass -> resolveElement
                    is PyCallExpression -> getPyClassByPyCallExpression(resolveElement)
                    is PyNamedParameter -> getPyClassFromPyNamedParameter(resolveElement, typeEvalContext)
                    else -> null
                }
            }.firstOrNull()
        }

        private fun addFieldElement(pyClass: PyClass, results: LinkedHashMap<String, LookupElement>, typeEvalContext: TypeEvalContext, excludes: HashSet<String>?) {
            getClassVariables(pyClass, typeEvalContext)
                    .filter { it.name != null }
                    .forEach {
                        val elementName = getLookupNameFromFieldName(it.name!!)
                        if (excludes == null || !excludes.contains(elementName)) {
                            val element = PrioritizedLookupElement.withGrouping(
                                    LookupElementBuilder
                                            .createWithSmartPointer(elementName, it)
                                            .withTypeText(getTypeHint(pyClass, typeEvalContext, it))
                                            .withIcon(icon), 1)
                            results[elementName] = PrioritizedLookupElement.withPriority(element, 100.0)
                        }
                    }
        }

        protected fun addAllFieldElement(parameters: CompletionParameters, result: CompletionResultSet,
                                         pyClass: PyClass, typeEvalContext: TypeEvalContext,
                                         excludes: HashSet<String>? = null) {

            val newElements: LinkedHashMap<String, LookupElement> = LinkedHashMap()

            pyClass.getAncestorClasses(typeEvalContext)
                    .filter { isPydanticModel(it) }
                    .forEach { addFieldElement(it, newElements, typeEvalContext, excludes) }

            addFieldElement(pyClass, newElements, typeEvalContext, excludes)

            result.runRemainingContributors(parameters)
            { completionResult ->
                completionResult.lookupElement.lookupString
                        .takeIf { name -> !newElements.containsKey(name) && (excludes == null || !excludes.contains(name)) }
                        ?.let { result.passResult(completionResult) }
            }
            result.addAllElements(newElements.values)
        }
    }

    private object KeywordArgumentCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(fieldName: String): String {
            return "${fieldName}="
        }

        override val icon: Icon = AllIcons.Nodes.Parameter

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val pyArgumentList = parameters.position.parent!!.parent!! as PyArgumentList
            val typeEvalContext = parameters.getTypeEvalContext()

            val pyClass = when (val pyCallableElement = pyArgumentList.parent!!) {
                is PyReferenceExpression -> getPyClassByPyReferenceExpression(pyCallableElement, typeEvalContext)
                        ?: return
                is PyCallExpression -> getPyClassByPyCallExpression(pyCallableElement) ?: return
                else -> return
            }

            if (!isPydanticModel(pyClass, typeEvalContext)) return

            val definedSet = pyArgumentList.children
                    .mapNotNull { (it as? PyKeywordArgument)?.name }
                    .map { "${it}=" }
                    .toHashSet()

            addAllFieldElement(parameters, result, pyClass, typeEvalContext, definedSet)
        }
    }

    private object FieldCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(fieldName: String): String {
            return fieldName
        }

        override val icon: Icon = AllIcons.Nodes.Field

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val typeEvalContext = parameters.getTypeEvalContext()
            val pyClass = when (val instance = parameters.position.parent.firstChild) {
                is PyReferenceExpression -> getPyClassByPyReferenceExpression(instance, typeEvalContext) ?: return
                is PyCallExpression -> getPyClassByPyCallExpression(instance) ?: return
                else -> return
            }

            if (!isPydanticModel(pyClass, typeEvalContext)) return

            addAllFieldElement(parameters, result, pyClass, typeEvalContext)
        }
    }
}
