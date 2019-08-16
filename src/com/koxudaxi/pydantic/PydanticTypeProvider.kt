package com.koxudaxi.pydantic

import com.intellij.openapi.util.Ref
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.containers.isNullOrEmpty
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionImpl
import com.jetbrains.python.psi.impl.PyCallExpressionNavigator
import com.jetbrains.python.psi.impl.PySubscriptionExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.*
import one.util.streamex.StreamEx

class PydanticTypeProvider : PyTypeProviderBase() {

    override fun getReferenceExpressionType(referenceExpression: PyReferenceExpression, context: TypeEvalContext): PyType? {
        return getPydanticTypeForCallee(referenceExpression, context)
    }

    override fun getParameterType(param: PyNamedParameter, func: PyFunction, context: TypeEvalContext): Ref<PyType>? {
        if (!param.isPositionalContainer && !param.isKeywordContainer && param.annotationValue == null && func.name == "__init__") {
            val cls = func.containingClass
            val name = param.name

            if (cls != null && name != null) {
                cls
                        .findClassAttribute(name, false, context)
                        ?.let {
                            return Ref.create(getTypeForParameter(it, context))
                        }

                for (ancestor in cls.getAncestorClasses(context)) {
                    ancestor
                            .findClassAttribute(name, false, context)
                            ?.let { return Ref.create(getTypeForParameter(it, context)) }
                }
            }
        }

        return null
    }

    private fun getPydanticTypeForCallee(referenceExpression: PyReferenceExpression, context: TypeEvalContext): PyCallableType? {
        if (PyCallExpressionNavigator.getPyCallExpressionByCallee(referenceExpression) == null) return null

        val resolveResults = getResolveElements(referenceExpression, context)

        return PyUtil.filterTopPriorityResults(resolveResults)
                .asSequence()
                .map {
                    when {
                        it is PyClass -> getPydanticTypeForClass(it, context)
                        it is PyParameter && it.isSelf -> {
                            PsiTreeUtil.getParentOfType(it, PyFunction::class.java)
                                    ?.takeIf { it.modifier == PyFunction.Modifier.CLASSMETHOD }
                                    ?.let {
                                        it.containingClass?.let { getPydanticTypeForClass(it, context) }
                                    }
                        }
                        else -> null
                    }
                }
                .firstOrNull { it != null }
    }

    private fun getPydanticTypeForClass(cls: PyClass, context: TypeEvalContext): PyCallableType? {
        val clsType = (context.getType(cls) as? PyClassLikeType) ?: return null

        val resolveContext = PyResolveContext.noImplicits().withTypeEvalContext(context)
        val ellipsis = PyElementGenerator.getInstance(cls.project).createEllipsis()

        val collected = linkedMapOf<String, PyCallableParameter>()

        for (currentType in StreamEx.of(clsType).append(cls.getAncestorTypes(context))) {
            if (currentType == null ||
                    !currentType.resolveMember(PyNames.INIT, null, AccessDirection.READ, resolveContext, false).isNullOrEmpty() ||
                    !currentType.resolveMember(PyNames.NEW, null, AccessDirection.READ, resolveContext, false).isNullOrEmpty() ||
                    currentType !is PyClassType) {
                continue
            }

            val current = currentType.pyClass
            if (!current.isSubclass("pydantic.main.BaseModel", context)) return null

            current
                    .classAttributes
                    .asReversed()
                    .asSequence()
                    .filterNot { PyTypingTypeProvider.isClassVar(it, context) }
                    .mapNotNull { fieldToParameter(it, ellipsis, context, current) }
                    .forEach { parameter ->
                        parameter.name?.let {
                            if (!collected.containsKey(it)) {
                                collected[it] = parameter
                            }
                        }
                    }
        }

        return PyCallableTypeImpl(collected.values.reversed(), clsType.toInstance())
    }

    private fun fieldToParameter(field: PyTargetExpression,
                                 ellipsis: PyNoneLiteralExpression,
                                 context: TypeEvalContext,
                                 pyClass: PyClass): PyCallableParameter? {
        val stub = field.stub
        val fieldStub = if (stub == null) PydanticFieldStubImpl.create(field) else stub.getCustomStub(PydanticFieldStub::class.java)
        if (fieldStub != null && !fieldStub.initValue()) return null
        if (fieldStub == null && field.annotationValue == null) return null // skip fields that are not annotated

        val defaultValue = when {
            pyClass.isSubclass("pydantic.env_settings.BaseSettings", context) -> ellipsis
            else ->  getDefaultValueForParameter(field, fieldStub, ellipsis, context)
        }

        return PyCallableParameterImpl.nonPsi(field.name,
                getTypeForParameter(field, context),
                defaultValue)
    }

    private fun getTypeForParameter(field: PyTargetExpression,
                                    context: TypeEvalContext): PyType? {

        return context.getType(field)

    }

    private fun getDefaultValueForParameter(field: PyTargetExpression,
                                            fieldStub: PydanticFieldStub?,
                                            ellipsis: PyNoneLiteralExpression,
                                            context: TypeEvalContext): PyExpression? {
        if (fieldStub == null) {
            val value = field.findAssignedValue()
            when {
                value == null -> {
                    val annotation = (field.annotation?.value as? PySubscriptionExpressionImpl) ?: return null

                    when {
                        annotation.qualifier?.text == "Optional" -> return ellipsis
                        annotation.qualifier?.text == "Union" -> for (child in annotation.children) {
                            if (child is PyTupleExpression) {
                                for (type in child.children) {
                                    if (type is PyNoneLiteralExpression) {
                                        return ellipsis
                                    }
                                }
                            }
                        }
                    }
                    return value
                }
                field.hasAssignedValue() -> return getDefaultValueByAssignedValue(field, ellipsis, context)
                else -> return null
            }
        } else if (fieldStub.hasDefault() || fieldStub.hasDefaultFactory()) {
            return ellipsis
        }
        return null
    }

    private fun getResolveElements(referenceExpression: PyReferenceExpression, context: TypeEvalContext): Array<ResolveResult> {
        val resolveContext = PyResolveContext.noImplicits().withTypeEvalContext(context)
        return referenceExpression.getReference(resolveContext).multiResolve(false)

    }

    private fun getDefaultValueByAssignedValue(field: PyTargetExpression,
                                               ellipsis: PyNoneLiteralExpression,
                                               context: TypeEvalContext): PyExpression? {
        val assignedValue = field.findAssignedValue()!!

        if (assignedValue.text == "...") {
            return null
        }
        val callee = (assignedValue as? PyCallExpressionImpl)?.callee ?: return ellipsis
        val referenceExpression = callee.reference?.element as? PyReferenceExpression ?: return ellipsis

        val resolveResults = getResolveElements(referenceExpression, context)
        PyUtil.filterTopPriorityResults(resolveResults)
                .asSequence()
                .forEach { it ->
                    val pyClass = PsiTreeUtil.getContextOfType(it, PyClass::class.java)
                    if (pyClass != null && pyClass.isSubclass("pydantic.schema.Schema", context)) {
                        val defaultValue = assignedValue.getKeywordArgument("default")
                                ?: assignedValue.getArgument(0, PyExpression::class.java)
                        when {
                            defaultValue == null -> return null
                            defaultValue.text == "..." -> return null
                            defaultValue.text == null -> return ellipsis
                            defaultValue.text.isNotBlank() -> return ellipsis
                        }
                    }
                }
        return ellipsis
    }
}
