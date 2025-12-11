package com.koxudaxi.pydantic

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticDataclassInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (element is PsiFile) return false
        if (element.containingFile !is PyFile) return false
        if (toolId != "PyDataclass") return false

        val pyClass = PsiTreeUtil.getParentOfType(element, PyClass::class.java, false) ?: return false

        val context = TypeEvalContext.codeAnalysis(pyClass.project, pyClass.containingFile)
        return isPydanticModel(pyClass, false, context)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return SuppressQuickFix.EMPTY_ARRAY
    }
}
