package com.koxudaxi.pydantic

import com.intellij.psi.PsiReference
import com.jetbrains.python.inspections.PyInspectionExtension
import com.jetbrains.python.psi.*
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
        val pyClass = function.containingClass ?: return false
        if (!isPydanticModel(pyClass, true, context)) return false
        if (!function.hasValidatorMethod(PydanticCacheService.getVersion(function.project))) return false
        if (function.hasModelValidatorModeAfter()) return false
        return true
    }
}