package com.koxudaxi.pydantic


import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.util.TextRange
import com.intellij.util.containers.nullize
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyStarArgument
import com.jetbrains.python.psi.types.PyCallableType
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.validation.PyAnnotator


class PydanticAnnotator : PyAnnotator() {
    override fun visitPyCallExpression(node: PyCallExpression) {
        super.visitPyCallExpression(node)
        annotatePydanticModelCallableExpression(node)
    }

    private fun annotatePydanticModelCallableExpression(pyCallExpression: PyCallExpression) {
        val context = TypeEvalContext.codeAnalysis(pyCallExpression.project, pyCallExpression.containingFile)
        val pyClassType = pyCallExpression.callee?.getType(context) as? PyCallableType ?: return
        val pyClass = pyClassType.getReturnType(context)?.pyClassTypes?.firstOrNull()?.pyClass ?: return
        if (!isPydanticModel(pyClass, true, context)) return
        if (getPydanticModelInit(pyClass, context) != null) return
        if (!pyCallExpression.isDefinitionCallExpression(context)) return

        val unFilledArguments =
            getPydanticUnFilledArguments(pyClassType, pyCallExpression, context).nullize()
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
