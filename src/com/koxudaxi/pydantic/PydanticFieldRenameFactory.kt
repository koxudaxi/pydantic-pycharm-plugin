package com.koxudaxi.pydantic

import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import com.jetbrains.python.codeInsight.PyCodeInsightSettings
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.search.PyClassInheritorsSearch


class PydanticFieldRenameFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement): Boolean {
        when (element) {
            is PyTargetExpression -> {
                val pyClass = element.containingClass ?: return false
                if (isPydanticModel(pyClass)) return true
            }
            is PyKeywordArgument -> {
                val pyClass = getPyClassByPyKeywordArgument(element) ?: return false
                if (isPydanticModel(pyClass)) return true
            }
        }
        return false
    }

    override fun getOptionName(): String? {
        return "Rename fields in hierarchy"
    }

    override fun isEnabled(): Boolean {
        return PyCodeInsightSettings.getInstance().RENAME_PARAMETERS_IN_HIERARCHY
    }

    override fun setEnabled(enabled: Boolean) {
        PyCodeInsightSettings.getInstance().RENAME_PARAMETERS_IN_HIERARCHY = enabled
    }

    override fun createRenamer(element: PsiElement, newName: String, usages: Collection<UsageInfo>): AutomaticRenamer {
        return PydanticFieldRenamer(element, newName)
    }

    class PydanticFieldRenamer(element: PsiElement, newName: String) : AutomaticRenamer() {
        init {
            val added = mutableSetOf<PyClass>()
            when (element) {
                is PyTargetExpression -> if (element.name != null) {
                    val pyClass = element.containingClass
                    if (pyClass is PyClass) {
                        addAllElement(pyClass, element.name!!, added)
                    }
                    suggestAllNames(element.name, newName)

                }
                is PyKeywordArgument -> if (element.name != null) {
                    val pyClass = getPyClassByPyKeywordArgument(element)
                    if (pyClass is PyClass) {
                        addAllElement(pyClass, element.name!!, added)
                    }
                    suggestAllNames(element.name!!, newName)
                }
            }
        }

        private fun addAllElement(pyClass: PyClass, elementName: String, added: MutableSet<PyClass>) {
            added.add(pyClass)
            addClassAttributes(pyClass, elementName)
            addKeywordArguments(pyClass, elementName)
            pyClass.getAncestorClasses(null).forEach { ancestorClass ->
                if (!isPydanticBaseModel(ancestorClass)) {
                    if (isPydanticModel(ancestorClass) &&
                            !added.contains(ancestorClass)) {
                        addAllElement(ancestorClass, elementName, added)
                    }
                }
            }
            PyClassInheritorsSearch.search(pyClass, true).forEach { inheritorsPyClass ->
                if (!isPydanticBaseModel(inheritorsPyClass) && !added.contains(inheritorsPyClass)) {
                    addAllElement(inheritorsPyClass, elementName, added)
                }
            }
        }

        private fun addClassAttributes(pyClass: PyClass, elementName: String) {
            val pyTargetExpression = pyClass.findClassAttribute(elementName, false, null) ?: return
            myElements.add(pyTargetExpression)
        }

        private fun addKeywordArguments(pyClass: PyClass, elementName: String) {
            ReferencesSearch.search(pyClass as PsiElement).forEach { psiReference ->
                val callee = PsiTreeUtil.getParentOfType(psiReference.element, PyCallExpression::class.java)
                callee?.arguments?.forEach { argument ->
                    if (argument is PyKeywordArgument && argument.name == elementName) {
                        myElements.add(argument)
                    }
                }
            }
        }

        override fun getDialogTitle(): String {
            return "Rename Fields"
        }

        override fun getDialogDescription(): String {
            return "Rename field in hierarchy to:"
        }

        override fun entityName(): String {
            return "Field"
        }

        override fun isSelectedByDefault(): Boolean {
            return true
        }
    }

}
