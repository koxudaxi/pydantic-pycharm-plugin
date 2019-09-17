package com.koxudaxi.pydantic

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.PyBundle
import com.jetbrains.python.PyNames
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.inspections.quickfix.RenameParameterQuickFix
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.impl.PyStarArgumentImpl
import com.jetbrains.python.psi.types.PyClassType

class PydanticInspection : PyInspection() {

    override fun buildVisitor(holder: ProblemsHolder,
                              isOnTheFly: Boolean,
                              session: LocalInspectionToolSession): PsiElementVisitor = Visitor(holder, session)

    private class Visitor(holder: ProblemsHolder, session: LocalInspectionToolSession) : PyInspectionVisitor(holder, session) {

        override fun visitPyFunction(node: PyFunction?) {
            super.visitPyFunction(node)

            val pyClass = node?.parent?.parent as? PyClass ?: return
            if (!isPydanticModel(pyClass, myTypeEvalContext) || !isValidatorMethod(node)) return
            val paramList = node.parameterList
            val params = paramList.parameters
            val firstParam = params.firstOrNull()
            if (firstParam == null) {
                registerProblem(paramList, PyBundle.message("INSP.must.have.first.parameter", PyNames.CANONICAL_CLS),
                        ProblemHighlightType.GENERIC_ERROR)
            } else if (firstParam.asNamed?.let { it.isSelf && it.name != PyNames.CANONICAL_CLS } == true) {
                registerProblem(PyUtil.sure(firstParam),
                        PyBundle.message("INSP.usually.named.\$0", PyNames.CANONICAL_CLS),
                        ProblemHighlightType.WEAK_WARNING, null,
                        RenameParameterQuickFix(PyNames.CANONICAL_CLS))
            }

        }

        override fun visitPyCallExpression(node: PyCallExpression?) {
            super.visitPyCallExpression(node)

            if (node == null) return

            val pyClassType = myTypeEvalContext.getType(node) as? PyClassType ?: return
            if (!isPydanticModel(pyClassType.pyClass, myTypeEvalContext)) return
            if ((node.callee as? PyReferenceExpressionImpl)?.isQualified == true) return
            node.arguments
                    .filterNot { it is PyKeywordArgument || (it as? PyStarArgumentImpl)?.isKeyword == true }
                    .forEach {
                        registerProblem(it,
                                "class '${pyClassType.pyClass.name}' accepts only keyword arguments")
                    }
        }
    }
}