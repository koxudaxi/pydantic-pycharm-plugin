package com.koxudaxi.pydantic

import com.intellij.openapi.application.QueryExecutorBase
import com.intellij.openapi.application.ReadAction.run
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.Processor
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.search.PyClassInheritorsSearch

private fun searchField(pyClass: PyClass, elementName: String, consumer: Processor<in PsiReference>): Boolean {
    if (!isPydanticModel(pyClass)) return false
    val pyTargetExpression = pyClass.findClassAttribute(elementName, false, null) ?: return false
    consumer.process(pyTargetExpression.reference)
    return true
}

private fun searchKeywordArgument(pyClass: PyClass, elementName: String, consumer: Processor<in PsiReference>) {
    if (!isPydanticModel(pyClass)) return
    ReferencesSearch.search(pyClass as PsiElement).forEach { psiReference ->
        PsiTreeUtil.getParentOfType(psiReference.element, PyCallExpression::class.java)
                ?.let { callee ->
                    callee.arguments
                            .filterIsInstance<PyKeywordArgument>()
                            .filter { it.name == elementName }
                            .forEach { consumer.process(it.reference) }
                }
    }
}

private fun searchDirectReferenceField(pyClass: PyClass, elementName: String, consumer: Processor<in PsiReference>): Boolean {
    if (searchField(pyClass, elementName, consumer)) return true

    return pyClass.getAncestorClasses(null)
            .filterNot { isPydanticBaseModel(it) }
            .firstOrNull { isPydanticModel(it) && searchDirectReferenceField(it, elementName, consumer) } != null
}

private fun searchAllElementReference(pyClass: PyClass, elementName: String, added: MutableSet<PyClass>, consumer: Processor<in PsiReference>) {
    added.add(pyClass)
    searchField(pyClass, elementName, consumer)
    searchKeywordArgument(pyClass, elementName, consumer)
    pyClass.getAncestorClasses(null)
            .filter { !isPydanticBaseModel(it) && !added.contains(it) }
            .forEach { searchField(it, elementName, consumer) }

    PyClassInheritorsSearch.search(pyClass, true)
            .filter { !isPydanticBaseModel(it) && !added.contains(it) }
            .forEach { searchAllElementReference(it, elementName, added, consumer) }
}

class PydanticFieldSearchExecutor : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        when (val element = queryParameters.elementToSearch) {
            is PyKeywordArgument -> run<RuntimeException> {
                element.name
                        ?.let { elementName ->
                            getPyClassByPyKeywordArgument(element)
                                    ?.takeIf { pyClass -> isPydanticModel(pyClass) }
                                    ?.let { pyClass -> searchDirectReferenceField(pyClass, elementName, consumer) }
                        }
            }
            is PyTargetExpression -> run<RuntimeException> {
                element.name
                        ?.let { elementName ->
                            element.containingClass
                                    ?.takeIf { pyClass -> isPydanticModel(pyClass) }
                                    ?.let { pyClass -> searchAllElementReference(pyClass, elementName, mutableSetOf(), consumer) }
                        }
            }
        }
    }
}
