package com.koxudaxi.pydantic


import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.util.containers.nullize
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.validation.PyAnnotator


class PydanticAnnotator : PyAnnotator() {
    private val pydanticTypeProvider = PydanticTypeProvider()
    override fun visitPyCallExpression(node: PyCallExpression?) {
        super.visitPyCallExpression(node)
        if (node == null) return
        annotatePydanticModelCallableExpression(node)
    }

    private fun annotatePydanticModelCallableExpression(pyCallExpression: PyCallExpression) {
        val context = TypeEvalContext.userInitiated(pyCallExpression.project, pyCallExpression.containingFile)
        val pyClass = getPydanticPyClass(pyCallExpression, context) ?: return
        val unFilledArguments = getPydanticUnFilledArguments(pyClass, pyCallExpression, pydanticTypeProvider, context).nullize()
                ?: return
        holder.newAnnotation(HighlightSeverity.INFORMATION, "Insert all arguments").withFix(PydanticInsertArgumentsQuickFix(false)).create()
        unFilledArguments.filter { it.required }.nullize() ?: return
        val highlight = when {
            isBaseSetting(pyClass, context) -> HighlightSeverity.INFORMATION
            else -> HighlightSeverity.WARNING
        }
        holder.newAnnotation(highlight, "Insert required arguments").withFix(PydanticInsertArgumentsQuickFix(true)).create()

    }
}
