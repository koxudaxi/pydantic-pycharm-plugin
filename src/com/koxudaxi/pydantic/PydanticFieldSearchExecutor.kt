package com.koxudaxi.pydantic

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction.run
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.SearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.search.PyClassInheritorsSearch
import com.jetbrains.python.psi.types.TypeEvalContext


class PydanticFieldSearchExecutor : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
    fun PyClass.isInScope(searchScope: SearchScope): Boolean = searchScope.contains(containingFile.virtualFile)
    override fun processQuery(
        queryParameters: ReferencesSearch.SearchParameters,
        consumer: Processor<in PsiReference>,
    ) {
        when (val element = queryParameters.elementToSearch) {
            is PyKeywordArgument -> run<RuntimeException> {
                element.name
                    ?.let { elementName ->
                        val context = TypeEvalContext.userInitiated(element.project, element.containingFile)
                        getPydanticModelByPyKeywordArgument(element, true, context)
                            ?.let { pyClass ->
                                searchDirectReferenceField(
                                    pyClass,
                                    elementName,
                                    consumer,
                                    queryParameters.effectiveSearchScope,
                                    context
                                )
                            }
                    }
            }

            is PyTargetExpression -> run<RuntimeException> {
                element.name
                    ?.let { elementName ->
                        val context = TypeEvalContext.userInitiated(element.project, element.containingFile)
                        element.containingClass
                            ?.takeIf { it.findClassAttribute(elementName, true, context) == element }
                            ?.takeIf { pyClass -> isPydanticModel(pyClass, true, context) }
                            ?.let { pyClass ->
                                searchAllElementReference(
                                    pyClass,
                                    elementName,
                                    mutableSetOf(),
                                    consumer,
                                    queryParameters.effectiveSearchScope,
                                    context
                                )
                            }
                    }
            }
        }
    }

    private fun searchField(
        pyClass: PyClass,
        elementName: String,
        consumer: Processor<in PsiReference>,
        searchScope: SearchScope,
        context: TypeEvalContext,
    ): Boolean {
        if (!pyClass.isInScope(searchScope)) return false
        if (!isPydanticModel(pyClass, true, context)) return false
        val pyTargetExpression = pyClass.findClassAttribute(elementName, false, context) ?: return false
        consumer.process(pyTargetExpression.reference)
        return true
    }

    private fun searchKeywordArgumentByPsiReference(
        psiReference: PsiReference,
        elementName: String,
        consumer: Processor<in PsiReference>,
        searchScope: SearchScope
    ) {
        if (!searchScope.contains(psiReference.element.containingFile.virtualFile)) return
        PsiTreeUtil.getParentOfType(psiReference.element, PyCallExpression::class.java)
            ?.let { callee ->
                callee.arguments.firstOrNull { it.name == elementName }?.let { consumer.process(it.reference) }
            }
    }

    private fun searchKeywordArgument(
        pyClass: PyClass,
        elementName: String,
        consumer: Processor<in PsiReference>,
        searchScope: SearchScope,
        typeEvalContext: TypeEvalContext,
    ) {
        ReferencesSearch.search(pyClass as PsiElement).forEach { psiReference ->
            searchKeywordArgumentByPsiReference(psiReference, elementName, consumer, searchScope)

            PsiTreeUtil.getParentOfType(psiReference.element, PyNamedParameter::class.java)
                ?.let { param ->
                    param.getArgumentType(
                        TypeEvalContext.userInitiated(
                            psiReference.element.project,
                            psiReference.element.containingFile
                        )
                    )
                        ?.let { pyType ->
                            pyType.pyClassTypes.firstOrNull { pyClassType ->
                                isPydanticModel(
                                    pyClassType.pyClass,
                                    true,
                                    typeEvalContext
                                )
                            }
                                ?.let {
                                    ReferencesSearch.search(param as PsiElement).forEach {
                                        searchKeywordArgumentByPsiReference(it, elementName, consumer, searchScope)
                                    }
                                }
                        }
                }
        }

    }


    private fun searchDirectReferenceField(
        pyClass: PyClass,
        elementName: String,
        consumer: Processor<in PsiReference>,
        searchScope: SearchScope,
        context: TypeEvalContext

    ): Boolean {
        if (searchField(pyClass, elementName, consumer, searchScope, context)) return true

        return getAncestorPydanticModels(pyClass, true, context)
            .firstOrNull {
                searchDirectReferenceField(
                    it,
                    elementName,
                    consumer,
                    searchScope,
                    context
                )
            } != null
    }

    private fun searchAllElementReference(
        pyClass: PyClass,
        elementName: String,
        added: MutableSet<PyClass>,
        consumer: Processor<in PsiReference>,
        searchScope: SearchScope,
        context: TypeEvalContext,
    ) {
        added.add(pyClass)
        searchField(pyClass, elementName, consumer, searchScope, context)
        searchKeywordArgument(pyClass, elementName, consumer, searchScope, context)

        getAncestorPydanticModels(pyClass, true, context)
            .filterNot { added.contains(it) }
            .forEach { searchField(it, elementName, consumer, searchScope, context) }

        PyClassInheritorsSearch.search(pyClass, true)
            .filterNot { added.contains(it) }
            .forEach { searchAllElementReference(it, elementName, added, consumer, searchScope, context) }
    }
}
