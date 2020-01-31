package com.koxudaxi.pydantic

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.codeInspection.ui.MultipleCheckboxOptionsPanel
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.PyBundle
import com.jetbrains.python.PyNames
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.inspections.quickfix.RenameParameterQuickFix
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyReferenceExpressionImpl
import com.jetbrains.python.psi.impl.PyStarArgumentImpl
import com.jetbrains.python.psi.impl.PyTargetExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.PyClassTypeImpl
import javax.swing.JComponent

class PydanticInspection : PyInspection() {

    override fun buildVisitor(holder: ProblemsHolder,
                              isOnTheFly: Boolean,
                              session: LocalInspectionToolSession): PsiElementVisitor = Visitor(holder, session)

    inner class Visitor(holder: ProblemsHolder, session: LocalInspectionToolSession) : PyInspectionVisitor(holder, session) {

        val pydanticConfigService = PydanticConfigService.getInstance(holder.project)

        override fun visitPyFunction(node: PyFunction?) {
            super.visitPyFunction(node)

            if (node == null) return
            val pyClass = getPyClassByAttribute(node) ?: return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext) || !isValidatorMethod(node)) return
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

        override fun visitPyAssignmentStatement(node: PyAssignmentStatement?) {
            super.visitPyAssignmentStatement(node)

            if (node == null) return
             if (pydanticConfigService.warnUntypedFields) {
                inspectWarnUntypedFields(node)
            }
            inspectReadOnlyProperty(node)
        }

        private fun inspectPydanticModelCallableExpression(pyCallExpression: PyCallExpression) {
            val pyClass = getPyClassByPyCallExpression(pyCallExpression, false, myTypeEvalContext) ?: return
            if (!isSubClassOfPydanticBaseModel(pyClass, myTypeEvalContext) || isPydanticBaseModel(pyClass)) return
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
            val resolveContext = PyResolveContext.defaultContext().withTypeEvalContext(myTypeEvalContext)
            val pyCallable = pyCallExpression.multiResolveCalleeFunction(resolveContext).firstOrNull() ?: return
            if (pyCallable.asMethod()?.qualifiedName != "pydantic.main.BaseModel.from_orm") return
            val typedElement = pyCallExpression.node?.firstChildNode?.firstChildNode?.psi as? PyTypedElement ?: return
            val pyClass = when (val type = myTypeEvalContext.getType(typedElement)) {
                is PyClass -> type
                is PyClassType -> getPyClassTypeByPyTypes(type).firstOrNull { isPydanticModel(it.pyClass, false) }?.pyClass
                else -> null
            } ?: return
            if (!isPydanticModel(pyClass, false)) return
            val config = getConfig(pyClass, myTypeEvalContext, true)
            if (config["orm_mode"] != true) {
                registerProblem(pyCallExpression,
                        "You must have the config attribute orm_mode=True to use from_orm",
                        ProblemHighlightType.GENERIC_ERROR)
            }
        }

        private fun inspectReadOnlyProperty(node: PyAssignmentStatement){
            val pyTypedElement = node.leftHandSideExpression?.firstChild as? PyTypedElement ?: return
            val pyType = myTypeEvalContext.getType(pyTypedElement) ?: return
            if ((pyType as? PyClassTypeImpl)?.isDefinition == true) return
            val pyClass = getPyClassTypeByPyTypes(pyType).firstOrNull()?.pyClass ?: return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            val attributeName = (node.leftHandSideExpression as? PyTargetExpressionImpl)?.name ?: return
            val config = getConfig(pyClass, myTypeEvalContext, true)
            if (config["allow_mutation"] != false) return
            registerProblem(node,
                    "Property \"${attributeName}\" defined in \"${pyClass.name}\" is read-only",
                    ProblemHighlightType.GENERIC_ERROR)

        }

        private fun inspectWarnUntypedFields(node: PyAssignmentStatement){
            val pyClass = getPyClassByAttribute(node) ?: return
            if (!isPydanticModel(pyClass, true, myTypeEvalContext)) return
            if (node.annotation != null) return
            if ((node.leftHandSideExpression as? PyTargetExpressionImpl)?.text?.startsWith("_") == true) return
            registerProblem(node,
                    "Untyped fields disallowed", ProblemHighlightType.WARNING)

        }
    }

//    override fun createOptionsPanel(): JComponent? {
//        val panel = MultipleCheckboxOptionsPanel(this)
//        panel.addCheckbox( "Warning untyped fields", "warnUntypedFields")
//        return panel
//    }
}