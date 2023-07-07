package com.koxudaxi.pydantic

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.stdlib.PyDataclassTypeProvider
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.inspections.quickfix.RenameParameterQuickFix
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionImpl
import com.jetbrains.python.psi.impl.PyTargetExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.*


class PydanticInspection : PyInspection() {
    private val pydanticTypeProvider = PydanticTypeProvider()
    private val pyDataclassTypeProvider = PyDataclassTypeProvider()
    override fun buildVisitor(
        holder: ProblemsHolder,
        isOnTheFly: Boolean,
        session: LocalInspectionToolSession,
    ): PsiElementVisitor = Visitor(holder, PyInspectionVisitor.getContext(session))

    inner class Visitor(holder: ProblemsHolder, context: TypeEvalContext) :
        PyInspectionVisitor(holder, context) {

        private val pydanticConfigService = PydanticConfigService.getInstance(holder.project)
        private val pydanticCacheService = PydanticCacheService.getInstance(holder.project)

        override fun visitPyFunction(node: PyFunction) {
            super.visitPyFunction(node)

            if (getPydanticModelByAttribute(node, true, myTypeEvalContext) == null) return
            if (!node.isValidatorMethod(pydanticCacheService.getOrPutVersion())) return
            val paramList = node.parameterList
            val params = paramList.parameters
            val firstParam = params.firstOrNull()
            if (firstParam == null) {
                registerProblem(
                    paramList, "Method must have a first parameter, usually called 'cls'",
                    ProblemHighlightType.GENERIC_ERROR
                )
            } else if (firstParam.asNamed?.let { it.isSelf && it.name != PyNames.CANONICAL_CLS } == true) {
                registerProblem(
                    PyUtil.sure(firstParam),
                    "Usually first parameter of such methods is named 'cls'",
                    ProblemHighlightType.WEAK_WARNING, null,
                    RenameParameterQuickFix(PyNames.CANONICAL_CLS)
                )
            }

        }

        override fun visitPyCallExpression(node: PyCallExpression) {
            super.visitPyCallExpression(node)


            inspectFromOrm(node)

            if (!node.isDefinitionCallExpression(myTypeEvalContext)) return
            inspectPydanticModelCallableExpression(node)
            inspectExtraForbid(node)

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

            if(pydanticCacheService.isV2) {
                inspectCustomRootFieldV2(node)
            }
            inspectConfig(node)
            inspectDefaultFactory(node)
        }

        private fun inspectCustomRootFieldV2(pyClass: PyClass) {
            if (getRootField(pyClass) == null) return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            registerProblem(
                pyClass.nameNode?.psi,
                "__root__ models are no longer supported in v2; a migration guide will be added in the near future", ProblemHighlightType.GENERIC_ERROR
            )
        }

        private fun inspectDefaultFactory(pyClass: PyClass) {
            if (!isPydanticModel(pyClass, true, myTypeEvalContext)) return
            val defaultFactories = (pyClass.classAttributes + getAncestorPydanticModels(
                pyClass,
                true,
                myTypeEvalContext
            ).flatMap { it.classAttributes }).mapNotNull {
                val name = it.name ?: return@mapNotNull null
                val assignedValue = it.findAssignedValue() as? PyCallExpression ?: return@mapNotNull null
                return@mapNotNull when (val defaultFactory = assignedValue.getKeywordArgument("default_factory")) {
                    is PyCallable -> defaultFactory as PyExpression to defaultFactory
                    is PyReferenceExpression -> getResolvedPsiElements(
                        defaultFactory,
                        myTypeEvalContext
                    ).filterIsInstance<PyCallable>().firstOrNull()?.let { resolved -> defaultFactory to resolved }

                    else -> null
                }?.let { defaultFactory -> name to defaultFactory }
            }.toMap()

            if (defaultFactories.isEmpty()) return

            PyCallExpressionImpl(pyClass.node).let { callSite ->
                when {
                    pyClass.isPydanticDataclass ->
                        pyDataclassTypeProvider.getReferenceType(
                            pyClass,
                            myTypeEvalContext,
                            callSite
                        )?.get() as? PyCallableType
                    else -> pydanticTypeProvider.getPydanticTypeForClass(
                        pyClass,
                        myTypeEvalContext,
                        true,
                        callSite
                    )
                }
            }?.getParameters(myTypeEvalContext)?.forEach {
                val defaultFactory = defaultFactories[it.name] ?: return@forEach
                val expectedType = it.getArgumentType(myTypeEvalContext) ?: return@forEach
                val actualType = myTypeEvalContext.getReturnType(defaultFactory.second) ?: return@forEach
                if (PyTypeChecker.match(expectedType, actualType, myTypeEvalContext)) return@forEach
                registerProblem(
                    defaultFactory.first.parent,
                    String.format(
                        "Expected type '%s', '%s' is set as return value of default_factory",
                        expectedType.name,
                        actualType.name
                    ),
                    ProblemHighlightType.WARNING
                )

            }

        }

        private fun inspectPydanticModelCallableExpression(pyCallExpression: PyCallExpression) {
            val pyClass = getPydanticPyClass(pyCallExpression, myTypeEvalContext) ?: return
            if (getPydanticModelInit(pyClass, myTypeEvalContext) != null) return
            pyCallExpression.arguments
                .filterNot { it is PyKeywordArgument || (it as? PyStarArgument)?.isKeyword == true }
                .forEach {
                    registerProblem(
                        it,
                        "class '${pyClass.name}' accepts only keyword arguments"
                    )
                }
        }

        private fun inspectExtraForbid(pyCallExpression: PyCallExpression) {
            val pyClass = getPydanticPyClass(pyCallExpression, myTypeEvalContext) ?: return
            if (getPydanticModelInit(pyClass, myTypeEvalContext) != null) return
            val config = getConfig(pyClass, myTypeEvalContext, true)
            if (config["extra"] != EXTRA.FORBID) return
            pyClass.getAncestorClasses(myTypeEvalContext)
            val parameters = (getAncestorPydanticModels(pyClass, false, myTypeEvalContext) + pyClass)
                .flatMap { pydanticModel ->
                    getClassVariables(pydanticModel, myTypeEvalContext)
                        .filter { it.name != null }
                        .filter { isValidField(it, myTypeEvalContext) }
                        .map { it.name }
                }.toSet()
            pyCallExpression.arguments
                .filterIsInstance<PyKeywordArgument>()
                .filterNot { it.name in parameters }
                .forEach {
                    registerProblem(
                        it,
                        "'${it.name}' extra fields not permitted",
                        ProblemHighlightType.GENERIC_ERROR
                    )
                }
        }

        private fun inspectFromOrm(pyCallExpression: PyCallExpression) {
            if (!pyCallExpression.isCalleeText("from_orm")) return
            val resolveContext = PyResolveContext.defaultContext(myTypeEvalContext)
            val pyCallable = pyCallExpression.multiResolveCalleeFunction(resolveContext).firstOrNull() ?: return
            if (pyCallable.asMethod()?.qualifiedName != "pydantic.main.BaseModel.from_orm") return
            val pyReferenceExpression = pyCallExpression.node?.firstChildNode?.firstChildNode?.psi as? PyReferenceExpression ?: return
            val pyClass = pyReferenceExpression.reference.resolve() as? PyClass ?: return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return

            val config = getConfig(pyClass, myTypeEvalContext, true)
            if (config["orm_mode"] != true) {
                registerProblem(
                    pyCallExpression,
                    "You must have the config attribute orm_mode=True to use from_orm",
                    ProblemHighlightType.GENERIC_ERROR
                )
            }
        }

        private fun inspectConfig(pyClass: PyClass) {
            val pydanticVersion = PydanticCacheService.getVersion(pyClass.project)
            if (pydanticVersion?.isAtLeast(1, 8) != true) return
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            validateConfig(pyClass, myTypeEvalContext)?.forEach {
                registerProblem(
                    it,
                    "Specifying config in two places is ambiguous, use either Config attribute or class kwargs",
                    ProblemHighlightType.GENERIC_ERROR
                )
            }
        }

        private fun inspectReadOnlyProperty(node: PyAssignmentStatement) {
            val pyTypedElement =
                node.leftHandSideExpression?.firstChild as? PyTypedElement ?: return
            val pyClassType = getPydanticPyClassType(pyTypedElement, myTypeEvalContext, false) ?: return
            if (pyClassType.isDefinition) return
            val pyClass = pyClassType.pyClass
            val attributeName = (node.leftHandSideExpression as? PyTargetExpressionImpl)?.name ?: return
            val config = getConfig(pyClass, myTypeEvalContext, true)
            val version = PydanticCacheService.getVersion(pyClass.project)
            if (config["allow_mutation"] == false || (version?.isAtLeast(1, 8) == true && config["frozen"] == true)) {
                registerProblem(
                    node,
                    "Property \"${attributeName}\" defined in \"${pyClass.name}\" is read-only",
                    ProblemHighlightType.GENERIC_ERROR
                )
            }
        }

        private fun inspectWarnUntypedFields(node: PyAssignmentStatement) {
            if (getPydanticModelByAttribute(node, true, myTypeEvalContext) == null) return
            if (node.annotation != null) return
            if ((node.leftHandSideExpression as? PyTargetExpressionImpl)?.text?.isValidFieldName != true) return
            registerProblem(
                node,
                "Untyped fields disallowed", ProblemHighlightType.WARNING
            )

        }

        private fun inspectCustomRootField(node: PyAssignmentStatement) {
            val pyClass = getPydanticModelByAttribute(node, false, myTypeEvalContext) ?: return

            val fieldName = (node.leftHandSideExpression as? PyTargetExpressionImpl)?.text ?: return
            if (fieldName.startsWith('_')) return
            val rootModel = getRootField(pyClass)?.containingClass ?: return
            if (!isPydanticModel(rootModel, false, myTypeEvalContext)) return
            registerProblem(
                node,
                "__root__ cannot be mixed with other fields", ProblemHighlightType.WARNING
            )
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
            if (!isPydanticModel(pyClass, true, myTypeEvalContext)) return
            val fieldName = node.target.name ?: return

            val annotationValue = node.annotation?.value ?: return
            val qualifiedName = getQualifiedName(annotationValue, myTypeEvalContext)
            if (qualifiedName != ANNOTATED_Q_NAME) return

            val annotatedField = getFieldFromAnnotated(annotationValue, myTypeEvalContext) ?: return
            val default = getDefaultFromField(annotatedField, myTypeEvalContext)
            if (default != null) {
                registerProblem(
                    default.parent,
                    "`Field` default cannot be set in `Annotated` for '$fieldName'",
                    ProblemHighlightType.WARNING
                )
            }
        }

        private fun inspectAnnotatedAssignedField(node: PyAssignmentStatement) {
            if (getPydanticModelByAttribute(node, true, myTypeEvalContext) == null) return

            val fieldName = (node.leftHandSideExpression as? PyTargetExpressionImpl)?.text ?: return
            val assignedValue = node.assignedValue


            val assignedValueField =
                assignedValue?.let { getFieldFromPyExpression(assignedValue, myTypeEvalContext, null) }
            if (assignedValueField != null) {
                val default: PyExpression? = getDefaultFromField(assignedValueField, myTypeEvalContext)
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
            val default = getDefaultFromField(annotatedField, myTypeEvalContext)
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

        private fun getRootField(pyClass: PyClass): PyTargetExpression? {
            return pyClass.findClassAttribute("__root__", true, myTypeEvalContext)
        }
    }

//    override fun createOptionsPanel(): JComponent? {
//        val panel = MultipleCheckboxOptionsPanel(this)
//        panel.addCheckbox( "Warning untyped fields", "warnUntypedFields")
//        return panel
//    }
}