package com.koxudaxi.pydantic

import com.intellij.psi.PsiReference
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticIgnoreInspection : PyInspectionExtension() {
    override fun ignoreUnresolvedReference(
        node: PyElement,
        reference: PsiReference,
        context: TypeEvalContext
    ): Boolean {
        if (node !is PyStringLiteralExpression) return false
        return isValidatorField(node, context)


    }

    override fun ignoreMethodParameters(function: PyFunction, context: TypeEvalContext): Boolean {
        return function.containingClass?.let {
            isPydanticModel(it,
                true,
                context) && function.isValidatorMethod(PydanticCacheService.getVersion(function.project))
        } == true
    }
}