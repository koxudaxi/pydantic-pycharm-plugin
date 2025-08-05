package com.koxudaxi.pydantic

import com.intellij.codeInspection.LocalInspectionToolSession
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.stdlib.PyDataclassTypeProvider
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyInspectionVisitor
import com.jetbrains.python.inspections.quickfix.RenameParameterQuickFix
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionImpl
import com.jetbrains.python.psi.impl.PyEvaluator
import com.jetbrains.python.psi.impl.PyTargetExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyCallableType
import com.jetbrains.python.psi.types.PyClassType
import com.jetbrains.python.psi.types.PyTypeChecker
import com.jetbrains.python.psi.types.TypeEvalContext


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
            if (!node.hasValidatorMethod(pydanticCacheService.getOrPutVersion())) return
            if (node.hasModelValidatorModeAfter()) return
            val paramList = (node as? PyCallable)?.parameterList ?: return
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
            inspectCustomRootField(node)
        }

        override fun visitPyClass(node: PyClass) {
            super.visitPyClass(node)


            inspectConfig(node)
            inspectDefaultFactory(node)
        }

        private fun inspectValidatorField(pyStringLiteralExpression: PyStringLiteralExpression) {
            if (pyStringLiteralExpression.reference?.resolve() != null) return
            val pyArgumentList = pyStringLiteralExpression.parent as? PyArgumentList ?: return
            pyArgumentList.getKeywordArgument("check_fields")?.let {
                // ignore unresolved value
                if (PyEvaluator.evaluateAsBoolean(it.value) != true) return
            }
            val stringValue = pyStringLiteralExpression.stringValue
            if (stringValue == "*") return
            registerProblem(
                    pyStringLiteralExpression,
                    "Cannot find field '${stringValue}'",
                    ProblemHighlightType.GENERIC_ERROR
            )
        }

        override fun visitPyStringLiteralExpression(node: PyStringLiteralExpression) {
            super.visitPyStringLiteralExpression(node)

            if (isValidatorField(node, myTypeEvalContext)) {
                inspectValidatorField(node)
            }
        }

        override fun visitPyReferenceExpression(node: PyReferenceExpression) {
            super.visitPyReferenceExpression(node)
            inspectModelAttribute(node)
            if (!pydanticCacheService.isV2) return
            val pyFunction = node.reference.resolve() as? PyFunction ?: return

            val qualifiedName = (pyFunction as? PyQualifiedNameOwner)?.qualifiedName ?: return
            if (!qualifiedName.startsWith("pydantic.")) return
            if (!isPydanticDeprecatedSince20(pyFunction)) return
            registerProblem(
                    node.nameElement?.psi ?: node,
                    "<html><body>" +
                            "Pydantic V2 Migration Guide: " +
                            "<a href=\"https://docs.pydantic.dev/dev-v2/migration/\">" +
                            "https://docs.pydantic.dev/dev-v2/migration/" +
                            "</a>" +
                            "</body></html>",
                    ProblemHighlightType.LIKE_DEPRECATED
            )

        }

        private fun isPydanticDeprecatedSince20(pyFunction: PyFunction): Boolean =
                pyFunction.statementList.statements.filterIsInstance<PyExpressionStatement>()
                        .mapNotNull { (it.expression as? PyCallExpression)?.getArgument(1, PyReferenceExpression::class.java) }
                        .any { (it.reference.resolve() as? PyTargetExpression)?.findAssignedValue()?.name == "PydanticDeprecatedSince20" }

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
                val expectedType = it.getType(myTypeEvalContext) ?: return@forEach
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
                        getClassVariables(pydanticModel, myTypeEvalContext, false)
                                .filter { it.name != null }
                                .filter { isValidField(it, myTypeEvalContext, pydanticCacheService.isV2, false) }
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
            val pyReferenceExpression = pyCallExpression.node?.firstChildNode?.firstChildNode?.psi as? PyReferenceExpression
                    ?: return
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
            if ((node.leftHandSideExpression as? PyTargetExpressionImpl)?.text?.isValidFieldName(pydanticCacheService.isV2) != true) return
            registerProblem(
                    node,
                    "Untyped fields disallowed", ProblemHighlightType.WARNING
            )

        }

        private fun inspectCustomRootField(node: PyAssignmentStatement) {
            val field = node.leftHandSideExpression as? PyTargetExpression ?: return
            inspectCustomRootField(field)
        }

        private fun inspectCustomRootField(node: PyTypeDeclarationStatement) {
            val field = node.target as? PyTargetExpression ?: return
            inspectCustomRootField(field)
        }

        private fun inspectCustomRootField(field: PyTargetExpression) {
            val pyClass = getPydanticModelByAttribute(field.parent, false, myTypeEvalContext) ?: return

            if (PyTypingTypeProvider.isClassVar(field, myTypeEvalContext) || PyTypingTypeProvider.isFinal(field, myTypeEvalContext)) return
            val fieldName = field.text ?: return
            val isV2 = pydanticCacheService.isV2
            if (isV2 && fieldName == "__root__") {
                registerProblem(
                        pyClass.nameNode?.psi,
                        "__root__ models are no longer supported in v2; a migration guide will be added in the near future", ProblemHighlightType.GENERIC_ERROR
                )
                registerProblem(field, "To define root models, use `pydantic.RootModel` rather than a field called '__root__'", ProblemHighlightType.WARNING)
                return
            }
            if (fieldName.startsWith('_')) return
            val message = when {
                isV2 -> {
                    if (fieldName == "root") return
                    if (!isSubClassOfPydanticRootModel(pyClass, myTypeEvalContext)) return
                    if (pyClass.findClassAttribute("root", true, myTypeEvalContext) == null) return
                    "Unexpected field with name ${fieldName}; only 'root' is allowed as a field of a `RootModel`"
                }

                else -> {
                    if (pyClass.findClassAttribute("__root__", true, myTypeEvalContext) == null) return
                    "__root__ cannot be mixed with other fields"
                }
            }
            registerProblem(field, message, ProblemHighlightType.WARNING)
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
            val default = getDefaultFromField(annotatedField, myTypeEvalContext) ?: return
            if (!pydanticCacheService.isV2) {
                registerProblem(
                        default.parent,
                        "`Field` default cannot be set in `Annotated` for '$fieldName'",
                        ProblemHighlightType.WARNING
                )
                return
            }

            val defaultFactory = getDefaultFactoryFromField(annotatedField) ?: return
            registerProblem(
                    defaultFactory.parent,
                    "cannot specify both default and default_factory",
                    ProblemHighlightType.WARNING
            )
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

        private fun inspectModelAttribute(node: PyQualifiedExpression) {
            val qualifier = node.qualifier ?: return
            val name = node.name ?: return
            val pyClassType = myTypeEvalContext.getType(qualifier) as? PyClassType ?: return
            val pyClass = pyClassType.pyClass
            if (!isPydanticModel(pyClass, false, myTypeEvalContext)) return
            if (pyClass.findNestedClass(name, true) is PyClass) return
            if (pyClass.findProperty(name, true, myTypeEvalContext) != null) return
            if (pyClass.findMethodByName(name, true, myTypeEvalContext) != null) return
            val field = pyClass.findClassAttribute(name, true, myTypeEvalContext)
            if (field is PyAnnotationOwner && (PyTypingTypeProvider.isClassVar(field, myTypeEvalContext) || PyTypingTypeProvider.isFinal(field, myTypeEvalContext))) return

            val pydanticVersion = PydanticCacheService.getVersion(pyClass.project)

            // Check private field or model fields
            if (name.startsWith("_") || pydanticVersion.isV2 && name.startsWith(MODEL_FIELD_PREFIX)) return

            if (pyClassType.isDefinition) {
                if(field == null && node.reference?.resolve() is PyTargetExpression) return
            } else {
                val config = getConfig(pyClass, myTypeEvalContext, true)
                getAncestorPydanticModels(pyClass, true, myTypeEvalContext).forEach {
                    if (hasAttribute(it, config, pydanticVersion.isV2, name)) return
                }
                if (hasAttribute(pyClass, config, pydanticVersion.isV2, name)) return
            }
            registerProblem(node.node.lastChildNode.psi, "Unresolved attribute reference '${name}' for class '${pyClass.name}' ")
        }
        private fun hasAttribute(pyClass: PyClass, config: HashMap<String, Any?>, isV2: Boolean, name: String): Boolean =
            getPydanticField(pyClass, myTypeEvalContext, config, isV2, false, name).any()

    }

//    override fun createOptionsPanel(): JComponent? {
//        val panel = MultipleCheckboxOptionsPanel(this)
//        panel.addCheckbox( "Warning untyped fields", "warnUntypedFields")
//        return panel
//    }
}