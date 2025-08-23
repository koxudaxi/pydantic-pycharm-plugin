package com.koxudaxi.pydantic

import com.intellij.codeInspection.InspectionSuppressor
import com.intellij.codeInspection.SuppressQuickFix
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.PyBinaryExpression
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.PyTypedElement
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticTypeIgnoreInspectionSuppressor : InspectionSuppressor {

    override fun isSuppressedFor(element: PsiElement, toolId: String): Boolean {
        if (element is PsiFile) return false
        if (element.containingFile !is PyFile) return false
        if (toolId !in inspectionsToSuppress) return false
        if (ignorePydanticGenericModelClassGetitem(element)) return true
        return ignorePydanticGenericModel(element)
    }

    private val PsiElement.typeEvalContext get() = TypeEvalContext.codeAnalysis(project, containingFile)

    private fun PySubscriptionExpression.isPydanticGenericModelSubclass(context: TypeEvalContext): Boolean {
        val operandType = context.getType(operand) as? PyClassType
            ?: return false
        return isSubClassOfPydanticGenericModel(operandType.pyClass, context)
    }

    private fun ignorePydanticGenericModelClassGetitem(element: PsiElement): Boolean {
        val subscription = PsiTreeUtil.getParentOfType(
            element,
            PySubscriptionExpression::class.java,
            false
        ) ?: return false
        return subscription.isPydanticGenericModelSubclass(element.typeEvalContext)
    }

    private fun ignorePydanticGenericModel(element: PsiElement): Boolean {
        if (element !is PyBinaryExpression) return false
        val pySubscriptionExpression = element.parent as? PySubscriptionExpression ?: return false
        val pyTypedElement = pySubscriptionExpression.parent as? PyTypedElement ?: return false
        if (!PyTypingTypeProvider.isBitwiseOrUnionAvailable(pySubscriptionExpression)) return false
        val context = element.typeEvalContext
        context.getType(pyTypedElement)?.pyClassTypes?.forEach {
            if (isSubClassOfPydanticGenericModel(it.pyClass, context)) {
                return true
            }
        }
        return false
    }

    override fun getSuppressActions(element: PsiElement?, toolId: String): Array<SuppressQuickFix> {
        return SuppressQuickFix.EMPTY_ARRAY
    }

    companion object {
        private val inspectionsToSuppress = listOf(
            "PyTypeChecker",
            "PydanticTypeChecker"
        )
    }
}