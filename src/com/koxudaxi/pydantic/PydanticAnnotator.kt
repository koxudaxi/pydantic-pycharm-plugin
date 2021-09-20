package com.koxudaxi.pydantic


import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.util.containers.nullize
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStarArgument
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.validation.PyAnnotator


class PydanticAnnotator : PyAnnotator() {
    private val pydanticTypeProvider = PydanticTypeProvider()
    override fun visitPyCallExpression(node: PyCallExpression) {
        super.visitPyCallExpression(node)
        annotatePydanticModelCallableExpression(node)
    }

    private fun annotatePydanticModelCallableExpression(pyCallExpression: PyCallExpression) {
        val context = TypeEvalContext.codeAnalysis(pyCallExpression.project, pyCallExpression.containingFile)
        val pyClass = getPydanticPyClass(pyCallExpression, context) ?: return
        if (getPydanticModelInit(pyClass, context) != null) return
        val unFilledArguments =
            getPydanticUnFilledArguments(pyClass, pyCallExpression, pydanticTypeProvider, context).nullize()
                ?: return
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).withFix(PydanticInsertArgumentsQuickFix(false))
            .create()
        unFilledArguments.filter { it.required }.nullize() ?: return
        val highlight = when {
            isSubClassOfBaseSetting(pyClass,
                context) || pyCallExpression.arguments.any { (it as? PyStarArgument)?.isKeyword == true } -> HighlightSeverity.INFORMATION
            else -> HighlightSeverity.WARNING
        }
        holder.newSilentAnnotation(highlight).withFix(PydanticInsertArgumentsQuickFix(true))
            .range(TextRange.from(pyCallExpression.textOffset + pyCallExpression.textLength - 1, 1)).create()
        holder.newSilentAnnotation(HighlightSeverity.INFORMATION).withFix(PydanticInsertArgumentsQuickFix(true))
            .range(TextRange.from(pyCallExpression.textOffset, pyCallExpression.textLength - 2)).create()
    }
}
