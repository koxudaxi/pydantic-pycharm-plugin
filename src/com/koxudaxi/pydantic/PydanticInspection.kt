package com.koxudaxi.pydantic

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.impl.PyStarArgumentImpl
import com.jetbrains.python.psi.types.PyClassTypeImpl

class PydanticInspection : PyInspection() {

    override fun buildVisitor(holder: ProblemsHolder,
                              isOnTheFly: Boolean,
                              session: LocalInspectionToolSession): PsiElementVisitor = Visitor(holder, session)

    private class Visitor(holder: ProblemsHolder, session: LocalInspectionToolSession) : PyInspectionVisitor(holder, session) {


        override fun visitPyCallExpression(node: PyCallExpression?) {
            super.visitPyCallExpression(node)

            if (node != null) {
                val pyClass: PyClass = (myTypeEvalContext.getType(node) as? PyClassTypeImpl)?.pyClass ?: return
                if (!pyClass.isSubclass("pydantic.main.BaseModel", myTypeEvalContext)) return
                for (argument in node.arguments) {
                    if (argument is PyKeywordArgument) {
                        continue
                    }
                    if ((argument as? PyStarArgumentImpl)?.isKeyword == true) {
                        continue
                    }
                    registerProblem(argument,
                            "class '${pyClass.name}' accepts only keyword arguments")
                }

            }
        }
    }
}