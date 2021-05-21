package com.koxudaxi.pydantic

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.PyNames
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.inspections.quickfix.RenameParameterQuickFix
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyTargetExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.PyClassTypeImpl


class PydanticInspection : PyInspection() {

    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ): PsiElementVisitor = Visitor(holder, session)

    inner class Visitor(holder: ProblemsHolder, session: LocalInspectionToolSession) :
        PyInspectionVisitor(holder, session) {

        val pydanticConfigService = PydanticConfigService.getInstance(holder.project)

        override fun visitPyFunction(node: PyFunction) {
            super.visitPyFunction(node)

            val pyClass = getPyClassByAttribute(node) ?: return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext) || !node.isValidatorMethod) return
            val paramList = node.parameterList
            val params = paramList.parameters
            val firstParam = params.firstOrNull()
            if (firstParam == null) {
                registerProblem(paramList, "Method must have a first parameter, usually called 'cls'",
                    ProblemHighlightType.GENERIC_ERROR)
            } else if (firstParam.asNamed?.let { it.isSelf && it.name != PyNames.CANONICAL_CLS } == true) {
                registerProblem(PyUtil.sure(firstParam),
                    "Usually first parameter of such methods is named 'cls'",
                    ProblemHighlightType.WEAK_WARNING, null,
                    RenameParameterQuickFix(PyNames.CANONICAL_CLS))
            }

        }

        override fun visitPyCallExpression(node: PyCallExpression) {
            super.visitPyCallExpression(node)

            inspectPydanticModelCallableExpression(node)
            inspectFromOrm(node)

        }

        override fun visitPyAssignmentStatement(node: PyAssignmentStatement) {
            super.visitPyAssignmentStatement(node)

            if (pydanticConfigService.currentWarnUntypedFields) {
                inspectWarnUntypedFields(node)
            }
            inspectCustomRootField(node)
            inspectReadOnlyProperty(node)
            inspectAnnotatedAssignedField(node)
        }

        override fun visitPyTypeDeclarationStatement(node: PyTypeDeclarationStatement) {
            super.visitPyTypeDeclarationStatement(node)

            inspectAnnotatedField(node)
        }

        override fun visitPyClass(node: PyClass) {
            super.visitPyClass(node)

            inspectConfig(node)
        }

        private fun inspectPydanticModelCallableExpression(pyCallExpression: PyCallExpression) {
            val pyClass = getPydanticPyClass(pyCallExpression, myTypeEvalContext) ?: return
            if (getPydanticModelInit(pyClass, myTypeEvalContext) != null) return
            pyCallExpression.arguments
                .filterNot { it is PyKeywordArgument || (it as? PyStarArgument)?.isKeyword == true }
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
            val type =
                (pyCallExpression.node?.firstChildNode?.firstChildNode?.psi as? PyTypedElement)?.getType(
                    myTypeEvalContext)
                    ?: return
            val pyClass = when (type) {
                is PyClass -> type
                is PyClassType -> type.pyClassTypes.firstOrNull {
                    isPydanticModel(it.pyClass,
                        false, myTypeEvalContext)
                }?.pyClass
                else -> null
            } ?: return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            val config = getConfig(pyClass, myTypeEvalContext, true)
            if (config["orm_mode"] != true) {
                registerProblem(pyCallExpression,
                    "You must have the config attribute orm_mode=True to use from_orm",
                    ProblemHighlightType.GENERIC_ERROR)
            }
        }

        private fun inspectConfig(pyClass: PyClass) {
            val pydanticVersion = PydanticCacheService.getVersion(pyClass.project, myTypeEvalContext)
            if (pydanticVersion?.isAtLeast(1, 8) != true) return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            validateConfig(pyClass, myTypeEvalContext)?.forEach {
                registerProblem(it,
                    "Specifying config in two places is ambiguous, use either Config attribute or class kwargs",
                    ProblemHighlightType.GENERIC_ERROR)
            }
        }

        private fun inspectReadOnlyProperty(node: PyAssignmentStatement) {
            val pyType =
                (node.leftHandSideExpression?.firstChild as? PyTypedElement)?.getType(myTypeEvalContext) ?: return
            if ((pyType as? PyClassTypeImpl)?.isDefinition == true) return
            val pyClass = pyType.pyClassTypes.firstOrNull()?.pyClass ?: return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            val attributeName = (node.leftHandSideExpression as? PyTargetExpressionImpl)?.name ?: return
            val config = getConfig(pyClass, myTypeEvalContext, true)
            val version = PydanticCacheService.getVersion(pyClass.project, myTypeEvalContext)
            if (config["allow_mutation"] == false || (version?.isAtLeast(1, 8) == true && config["frozen"] == true)) {
                registerProblem(node,
                    "Property \"${attributeName}\" defined in \"${pyClass.name}\" is read-only",
                    ProblemHighlightType.GENERIC_ERROR)
            }
        }

        private fun inspectWarnUntypedFields(node: PyAssignmentStatement) {
            val pyClass = getPyClassByAttribute(node) ?: return
            if (!isPydanticModel(pyClass, true, myTypeEvalContext)) return
            if (node.annotation != null) return
            if ((node.leftHandSideExpression as? PyTargetExpressionImpl)?.text?.isValidFieldName != true) return
            registerProblem(node,
                "Untyped fields disallowed", ProblemHighlightType.WARNING)

        }

        private fun inspectCustomRootField(node: PyAssignmentStatement) {
            val pyClass = getPyClassByAttribute(node) ?: return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            val fieldName = (node.leftHandSideExpression as? PyTargetExpressionImpl)?.text ?: return
            if (fieldName.startsWith('_')) return
            val rootModel = pyClass.findClassAttribute("__root__", true, myTypeEvalContext)?.containingClass ?: return
            if (!isPydanticModel(rootModel, false, myTypeEvalContext)) return
            registerProblem(node,
                "__root__ cannot be mixed with other fields", ProblemHighlightType.WARNING)
        }

        private fun validateDefaultAndDefaultFactory(default: PyExpression?, defaultFactory: PyExpression?): Boolean {
            if (default == null || defaultFactory == null) return true
            registerProblem(
                defaultFactory.parent,
                "cannot specify both default and default_factory",
                ProblemHighlightType.WARNING
            )
            return false
        }

        private fun inspectAnnotatedField(node: PyTypeDeclarationStatement) {
            val pyClass = getPyClassByAttribute(node) ?: return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            val fieldName = node.target.name ?: return

            val annotationValue = node.annotation?.value ?: return
            val qualifiedName = getQualifiedName(annotationValue, myTypeEvalContext)
            if (qualifiedName != ANNOTATED_Q_NAME) return

            val annotatedField = getFieldFromAnnotated(annotationValue, myTypeEvalContext) ?: return
            val default = getDefaultFromField(annotatedField)
            if (default != null) {
                registerProblem(
                    default.parent,
                    "`Field` default cannot be set in `Annotated` for '$fieldName'",
                    ProblemHighlightType.WARNING
                )
            }
        }

        private fun inspectAnnotatedAssignedField(node: PyAssignmentStatement) {
            val pyClass = getPyClassByAttribute(node) ?: return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            val fieldName = (node.leftHandSideExpression as? PyTargetExpressionImpl)?.text ?: return
            val assignedValue = node.assignedValue


            val assignedValueField =
                assignedValue?.let { getFieldFromPyExpression(assignedValue, myTypeEvalContext, null) }
            if (assignedValueField != null) {
                val default: PyExpression? = getDefaultFromField(assignedValueField)
                val defaultFactory: PyExpression? = getDefaultFactoryFromField(assignedValueField)
                if (!validateDefaultAndDefaultFactory(default, defaultFactory)) return
            }

            val annotationValue = node.annotation?.value ?: return
            val qualifiedName = getQualifiedName(annotationValue, myTypeEvalContext)
            if (qualifiedName != ANNOTATED_Q_NAME) return
            if (assignedValueField != null) {
                registerProblem(
                    assignedValueField,
                    "cannot specify `Annotated` and value `Field`s together for '$fieldName'",
                    ProblemHighlightType.WARNING
                )
                return
            }
            val annotatedField = getFieldFromAnnotated(annotationValue, myTypeEvalContext) ?: return
            val default = getDefaultFromField(annotatedField)
            val defaultFactory = getDefaultFactoryFromField(annotatedField)
            if (!validateDefaultAndDefaultFactory(assignedValue, defaultFactory)) return
            if (default != null) {
                registerProblem(
                    assignedValue,
                    "`Field` default cannot be set in `Annotated` for '$fieldName'",
                    ProblemHighlightType.WARNING
                )
            }
        }
    }

//    override fun createOptionsPanel(): JComponent? {
//        val panel = MultipleCheckboxOptionsPanel(this)
//        panel.addCheckbox( "Warning untyped fields", "warnUntypedFields")
//        return panel
//    }
}