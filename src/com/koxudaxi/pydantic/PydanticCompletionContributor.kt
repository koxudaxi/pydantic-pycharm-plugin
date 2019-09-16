package com.koxudaxi.pydantic

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.psi.util.PsiTreeUtil.getParentOfType
import com.intellij.util.ProcessingContext
import com.jetbrains.python.PyTokenTypes
import com.jetbrains.python.codeInsight.completion.getTypeEvalContext
import com.jetbrains.python.documentation.PythonDocumentationProvider.getTypeHint
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.PyResolveContext
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
    }

    private abstract class PydanticCompletionProvider : CompletionProvider<CompletionParameters>() {

        abstract val icon: Icon

        abstract fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext): String

        val typeProvider: PydanticTypeProvider = PydanticTypeProvider()

        val excludeFields: HashSet<String> = hashSetOf("Config")

        private fun getTypeText(pyClass: PyClass, typeEvalContext: TypeEvalContext,
                                pyTargetExpression: PyTargetExpression,
                                ellipsis: PyNoneLiteralExpression): String {
            val parameter = typeProvider.fieldToParameter(pyTargetExpression, ellipsis, typeEvalContext, pyClass)
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

        private fun getPydanticPyClassTypesFromPyNamedParameter(pyNamedParameter: PyNamedParameter, typeEvalContext: TypeEvalContext): PyClassType? {
            return pyNamedParameter.getArgumentType(typeEvalContext)?.let {
                getPyClassTypeByPyTypes(it)
            }?.firstOrNull { it1 -> isPydanticModel(it1.pyClass) }
        }

        protected fun getPyClassByPyReferenceExpression(pyReferenceExpression: PyReferenceExpression, typeEvalContext: TypeEvalContext, parameters: CompletionParameters?, result: CompletionResultSet?): PyClass? {
            val resolveContext = PyResolveContext.defaultContext().withTypeEvalContext(typeEvalContext)
            return pyReferenceExpression.multiFollowAssignmentsChain(resolveContext).mapNotNull {
                when (val resolveElement = it.element) {
                    is PyClass -> {
                        resolveElement.getType(typeEvalContext)?.isDefinition
                        if (parameters != null && result != null) {
                            removeAllFieldElement(parameters, result, resolveElement, typeEvalContext, excludeFields)
                            null
                        } else {
                            resolveElement
                        }
                    }
                    is PyCallExpression -> getPyClassByPyCallExpression(resolveElement, typeEvalContext)
                    is PyNamedParameter -> {
                        if (parameters != null && result != null) {
                            if (resolveElement.isSelf) {
                                getParentOfType(resolveElement, PyFunction::class.java)
                                        ?.takeIf { it.modifier == PyFunction.Modifier.CLASSMETHOD }
                                        ?.takeIf { it.containingClass is PyClass }
                                        ?.let {
                                            removeAllFieldElement(parameters, result, it.containingClass!!, typeEvalContext, excludeFields)
                                            return null
                                        }
                            }
                            val pyClassType = getPydanticPyClassTypesFromPyNamedParameter(resolveElement, typeEvalContext) ?: return null
                            if (pyClassType.isDefinition) {  // is class
                                removeAllFieldElement(parameters, result, pyClassType.pyClass, typeEvalContext, excludeFields)
                                return null
                            }
                            return pyClassType.pyClass
                        }
                        getPydanticPyClassTypesFromPyNamedParameter(resolveElement, typeEvalContext)?.pyClass
                    }
                    else -> null
                }
            }.firstOrNull()
        }

        private fun addFieldElement(pyClass: PyClass, results: LinkedHashMap<String, LookupElement>,
                                    typeEvalContext: TypeEvalContext,
                                    ellipsis: PyNoneLiteralExpression,
                                    excludes: HashSet<String>?) {
            getClassVariables(pyClass, typeEvalContext)
                    .filter { it.name != null }
                    .forEach {
                        val elementName = getLookupNameFromFieldName(it, typeEvalContext)
                        if (excludes == null || !excludes.contains(elementName)) {
                            val element = PrioritizedLookupElement.withGrouping(
                                    LookupElementBuilder
                                            .createWithSmartPointer(elementName, it)
                                            .withTypeText(getTypeText(pyClass, typeEvalContext, it, ellipsis))
                                            .withIcon(icon), 1)
                            results[elementName] = PrioritizedLookupElement.withPriority(element, 100.0)
                        }
                    }
        }

        protected fun addAllFieldElement(parameters: CompletionParameters, result: CompletionResultSet,
                                         pyClass: PyClass, typeEvalContext: TypeEvalContext,
                                         ellipsis: PyNoneLiteralExpression,
                                         excludes: HashSet<String>? = null) {

            val newElements: LinkedHashMap<String, LookupElement> = LinkedHashMap()

            pyClass.getAncestorClasses(typeEvalContext)
                    .filter { isPydanticModel(it) }
                    .forEach { addFieldElement(it, newElements, typeEvalContext, ellipsis, excludes) }

            addFieldElement(pyClass, newElements, typeEvalContext, ellipsis, excludes)

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
                    .forEach { fieldElements.addAll(it.classAttributes.mapNotNull { attribute -> attribute?.name }) }


            fieldElements.addAll(pyClass.classAttributes.mapNotNull { attribute -> attribute?.name })

            result.runRemainingContributors(parameters)
            { completionResult ->
                if (completionResult.lookupElement.psiElement?.getIcon(0) == AllIcons.Nodes.Field) {
                    completionResult.lookupElement.lookupString
                            .takeIf { name -> !fieldElements.contains(name) && (excludes == null || !excludes.contains(name)) }
                            ?.let { result.passResult(completionResult) }
                } else {
                    result.passResult(completionResult)
                }
            }
        }
    }

    private object KeywordArgumentCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext): String {
            return "${getAliasedFieldName(field, context)}="
        }

        override val icon: Icon = AllIcons.Nodes.Parameter

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val pyArgumentList = parameters.position.parent!!.parent!! as PyArgumentList
            val typeEvalContext = parameters.getTypeEvalContext()

            val pyClass = when (val pyCallableElement = pyArgumentList.parent!!) {
                is PyReferenceExpression -> getPyClassByPyReferenceExpression(pyCallableElement, typeEvalContext, null, null)
                        ?: return
                is PyCallExpression -> getPyClassByPyCallExpression(pyCallableElement, typeEvalContext, onlyDefinition = true)
                        ?: return
                else -> return
            }

            if (!isPydanticModel(pyClass, typeEvalContext)) return

            val definedSet = pyArgumentList.children
                    .mapNotNull { (it as? PyKeywordArgument)?.name }
                    .map { "${it}=" }
                    .toHashSet()
            val ellipsis = PyElementGenerator.getInstance(pyClass.project).createEllipsis()
            addAllFieldElement(parameters, result, pyClass, typeEvalContext, ellipsis, definedSet)
        }
    }

    private object FieldCompletionProvider : PydanticCompletionProvider() {
        override fun getLookupNameFromFieldName(field: PyTargetExpression, context: TypeEvalContext): String {
            return field.name!!
        }

        override val icon: Icon = AllIcons.Nodes.Field

        override fun addCompletions(parameters: CompletionParameters, context: ProcessingContext, result: CompletionResultSet) {
            val typeEvalContext = parameters.getTypeEvalContext()
            val pyClass = when (val instance = parameters.position.parent.firstChild) {
                is PyReferenceExpression -> getPyClassByPyReferenceExpression(instance, typeEvalContext, parameters, result)
                        ?: return
                is PyCallExpression -> getPyClassByPyCallExpression(instance, typeEvalContext) ?: return
                is PySubscriptionExpression ->  {
                    val pyType = typeEvalContext.getType(instance as PyTypedElement) ?: return
                    getPyClassTypeByPyTypes(pyType).filter {  isPydanticModel(it.pyClass) }.map {
                        when {
                            it.isDefinition -> {
                                removeAllFieldElement(parameters, result, it.pyClass, typeEvalContext, excludeFields)
                                null
                            }
                            else -> it.pyClass
                        }
                    }.firstOrNull() ?: return
                }
                else -> return
            }

            if (!isPydanticModel(pyClass, typeEvalContext)) return
            val ellipsis = PyElementGenerator.getInstance(pyClass.project).createEllipsis()
            addAllFieldElement(parameters, result, pyClass, typeEvalContext, ellipsis)
        }
    }
}
