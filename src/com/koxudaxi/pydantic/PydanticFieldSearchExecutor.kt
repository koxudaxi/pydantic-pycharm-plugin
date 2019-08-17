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

private fun searchField(pyClass: PyClass, elementName: String, consumer: Processor<in PsiReference>, single: Boolean = false): Boolean {
    if (!pyClass.isSubclass("pydantic.main.BaseModel", null)) return false
    pyClass.classAttributes.forEach { pyTargetExpression ->
        if (pyTargetExpression.name == elementName) {
            consumer.process(pyTargetExpression.reference)
            if (single) return true
        }
    }
    return false
}

private fun searchKeywordArgument(pyClass: PyClass, elementName: String, consumer: Processor<in PsiReference>) {
    if (!pyClass.isSubclass("pydantic.main.BaseModel", null)) return
    ReferencesSearch.search(pyClass as PsiElement).forEach { psiReference ->
        val callee = PsiTreeUtil.getParentOfType(psiReference.element, PyCallExpression::class.java)
        callee?.arguments?.forEach { argument ->
            if (argument is PyKeywordArgument && argument.name == elementName) {
                consumer.process(argument.reference)

            }
        }
    }
}

private fun searchDirectReferenceField(pyClass: PyClass, elementName: String, consumer: Processor<in PsiReference>): Boolean {
    if (searchField(pyClass, elementName, consumer, true)) {
        return true
    }
    pyClass.getAncestorClasses(null).forEach {  ancestorClass ->
        if (ancestorClass.qualifiedName != "pydantic.main.BaseModel") {
            if (ancestorClass.isSubclass("pydantic.main.BaseModel", null)) {
                if (searchDirectReferenceField(ancestorClass, elementName, consumer)) {
                    return true
                }
            }
            }
        }
    return false
    }

private fun searchAllElementReference(pyClass: PyClass?, elementName: String, added: MutableSet<PyClass>, consumer: Processor<in PsiReference>) {
    if (pyClass == null) return
    added.add(pyClass)
    searchField(pyClass, elementName, consumer)
    searchKeywordArgument(pyClass, elementName, consumer)
    PyClassInheritorsSearch.search(pyClass, true).forEach { inheritorsPyClass ->
        if (inheritorsPyClass.qualifiedName != "pydantic.main.BaseModel" && ! added.contains(inheritorsPyClass)) {
            searchAllElementReference(inheritorsPyClass, elementName, added, consumer)
        }
    }
}

class PydanticFieldSearchExecutor : QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters>() {
    override fun processQuery(queryParameters: ReferencesSearch.SearchParameters, consumer: Processor<in PsiReference>) {
        val element = queryParameters.elementToSearch

        when (element) {
            is PyKeywordArgument -> run<RuntimeException> {
                val elementName = element.name ?: return@run
                val pyClass = getPyClassByPyKeywordArgument(element) ?: return@run
                if (!pyClass.isSubclass("pydantic.main.BaseModel", null)) return@run
                searchDirectReferenceField(pyClass, elementName, consumer)
            }
            is PyTargetExpression -> run<RuntimeException> {
                val elementName = element.name ?: return@run
                val pyClass = element.containingClass ?: return@run
                if (!pyClass.isSubclass("pydantic.main.BaseModel", null)) return@run
                searchAllElementReference(pyClass, elementName, mutableSetOf(), consumer)
            }
        }
        }
    }
