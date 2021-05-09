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
import com.jetbrains.python.psi.types.TypeEvalContext


class PydanticFieldRenameFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement): Boolean {
        when (element) {
            is PyTargetExpression -> {
                val pyClass = element.containingClass ?: return false
                val content = TypeEvalContext.codeAnalysis(element.project, element.containingFile)
                if (isPydanticModel(pyClass, true, content)) return true
            }
            is PyKeywordArgument -> {
                val context = TypeEvalContext.codeAnalysis(element.project, element.containingFile)
                val pyClass = getPyClassByPyKeywordArgument(element, context) ?: return false
                if (isPydanticModel(pyClass, true, context)) return true
            }
        }
        return false
    }

    override fun getOptionName(): String {
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
                is PyTargetExpression ->
                    element.name?.let { name ->
                        element.containingClass
                            ?.let { pyClass ->
                                val content = TypeEvalContext.codeAnalysis(element.project, element.containingFile)
                                addAllElement(pyClass, name, added, content)
                            }
                        suggestAllNames(name, newName)
                    }
                is PyKeywordArgument ->
                    element.name?.let { name ->
                        val context = TypeEvalContext.userInitiated(element.project, element.containingFile)
                        getPyClassByPyKeywordArgument(element, context)
                            ?.let { pyClass ->
                                addAllElement(pyClass, name, added, context)
                            }
                        suggestAllNames(name, newName)
                    }
            }
        }

        private fun addAllElement(
            pyClass: PyClass,
            elementName: String,
            added: MutableSet<PyClass>,
            context: TypeEvalContext,
        ) {
            added.add(pyClass)
            addClassAttributes(pyClass, elementName)
            addKeywordArguments(pyClass, elementName)
            pyClass.getAncestorClasses(null)
                .filter { isPydanticModel(it, true, context) && !added.contains(it) }
                .forEach { addAllElement(it, elementName, added, context) }

            PyClassInheritorsSearch.search(pyClass, true)
                .filterNot { added.contains(it) }
                .forEach { addAllElement(it, elementName, added, context) }
        }

        private fun addClassAttributes(pyClass: PyClass, elementName: String) {
            val pyTargetExpression = pyClass.findClassAttribute(elementName, false, null) ?: return
            myElements.add(pyTargetExpression)
        }

        private fun addKeywordArguments(pyClass: PyClass, elementName: String) {
            ReferencesSearch.search(pyClass as PsiElement).forEach { psiReference ->
                PsiTreeUtil.getParentOfType(psiReference.element, PyCallExpression::class.java)
                    ?.let { callee ->
                        callee.arguments
                            .filterIsInstance<PyKeywordArgument>()
                            .filter { it.name == elementName }
                            .forEach { myElements.add(it) }
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
