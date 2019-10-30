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
import com.jetbrains.python.psi.resolve.PyResolveContext
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

            inspectPydanticModelCallableExpression(node)
            inspectFromOrm(node)

        }

        private fun inspectPydanticModelCallableExpression(pyCallExpression: PyCallExpression) {
            val pyClass = getPyClassByPyCallExpression(pyCallExpression, myTypeEvalContext) ?: return
            if (!isPydanticModel(pyClass, myTypeEvalContext)) return
            if ((pyCallExpression.callee as? PyReferenceExpressionImpl)?.isQualified == true) return
            pyCallExpression.arguments
                    .filterNot { it is PyKeywordArgument || (it as? PyStarArgumentImpl)?.isKeyword == true }
                    .forEach {
                        registerProblem(it,
                                "class '${pyClass.name}' accepts only keyword arguments")
                    }
        }

        private fun inspectFromOrm(pyCallExpression: PyCallExpression) {
            if (!pyCallExpression.isCalleeText("from_orm")) return
            val resolveContext = PyResolveContext.noImplicits().withTypeEvalContext(myTypeEvalContext)
            val pyCallable = pyCallExpression.multiResolveCalleeFunction(resolveContext).firstOrNull() ?: return
            if (pyCallable.asMethod()?.qualifiedName != "pydantic.main.BaseModel.from_orm") return
            val typedElement = pyCallExpression.node?.firstChildNode?.firstChildNode?.psi as? PyTypedElement ?: return
            val pyClass = when (val type = myTypeEvalContext.getType(typedElement)) {
                is PyClass -> type
                is PyClassType -> getPyClassTypeByPyTypes(type).firstOrNull { isPydanticModel(it.pyClass) }?.pyClass
                else -> null
            } ?: return
            if (!isPydanticModel(pyClass)) return
            val config = getConfig(pyClass, myTypeEvalContext, true)
            if (config["orm_mode"] != true) {
                registerProblem(pyCallExpression,
                        "You must have the config attribute orm_mode=True to use from_orm",
                        ProblemHighlightType.GENERIC_ERROR)
            }
        }
    }
}