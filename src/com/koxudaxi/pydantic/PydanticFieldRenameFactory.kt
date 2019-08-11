package com.koxudaxi.pydantic

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.refactoring.rename.naming.AutomaticRenamerFactory
import com.intellij.usageView.UsageInfo
import com.jetbrains.python.codeInsight.PyCodeInsightSettings
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyTargetExpression
import java.util.*

class PydanticFieldRenameFactory : AutomaticRenamerFactory {
    override fun isApplicable(element: PsiElement): Boolean {
        // Field to KeywordArguments
        if (element is PyTargetExpression) {
            val pyClass = element.containingClass ?: return false
            if (pyClass.isSubclass("pydantic.main.BaseModel", null)) return true
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
            val pyTargetExpression = (element as? PyTargetExpression)
            if (pyTargetExpression?.name != null) {

                val pyClass = pyTargetExpression.containingClass
                ReferencesSearch.search(pyClass as @org.jetbrains.annotations.NotNull PsiElement).forEach { psiReference ->
                    val callee = PsiTreeUtil.getParentOfType(psiReference.element, PyCallExpression::class.java)
                    callee?.arguments?.forEach { argument ->
                        if (argument is PyKeywordArgument) {
                            if (argument.name == pyTargetExpression.name) {
                                myElements.add(argument)
                            }
                        }
                    }
                    return@forEach
                }
            suggestAllNames(element.name!!, newName)
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
