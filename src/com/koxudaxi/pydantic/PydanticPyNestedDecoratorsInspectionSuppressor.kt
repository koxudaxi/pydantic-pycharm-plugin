package com.koxudaxi.pydantic

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyDecorator
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticPyNestedDecoratorsInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (element is PsiFile) return false
        if (element.containingFile !is PyFile) return false
        if (toolId !in inspectionsToSuppress) return false
        if (element !is PyDecorator) return false
        return element.include(MODEL_VALIDATOR_QUALIFIED_NAMES)
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return SuppressQuickFix.EMPTY_ARRAY
    }

    companion object {
        private val inspectionsToSuppress = listOf(
            "PyNestedDecorators"
        )
    }
}