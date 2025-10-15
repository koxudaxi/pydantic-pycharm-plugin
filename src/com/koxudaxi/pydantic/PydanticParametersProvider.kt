package com.koxudaxi.pydantic

import com.intellij.openapi.project.Project
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.codeInsight.PyDataclassParameters
import com.jetbrains.python.codeInsight.PyDataclassParametersProvider
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.PyExpression
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PySubscriptionExpression
import com.jetbrains.python.psi.types.PyClassLikeType
import com.jetbrains.python.psi.types.PyCallableParameter
import com.jetbrains.python.psi.types.PyCallableParameterImpl
import com.jetbrains.python.psi.types.TypeEvalContext
import java.util.LinkedHashSet

class PydanticParametersProvider : PyDataclassParametersProvider {

    override fun getType(): PyDataclassParameters.Type = PydanticType

    override fun getDecoratorAndTypeAndParameters(project: Project): Triple<QualifiedName, PyDataclassParameters.Type, List<PyCallableParameter>> {
        val generator = PyElementGenerator.getInstance(project)
        val parameters = mutableListOf(PyCallableParameterImpl.psi(generator.createSingleStarParameter()))
        parameters.addAll(DATACLASS_ARGUMENTS.map { name -> PyCallableParameterImpl.nonPsi(name, null, null) })
        return Triple(DATACLASS_QUALIFIED_NAME, PydanticType, parameters)
    }

    override fun getDataclassParameters(cls: PyClass, context: TypeEvalContext?): PyDataclassParameters? {
        val evalContext = context ?: TypeEvalContext.codeInsightFallback(cls.project)
        if (!shouldBypassDataclassTransform(cls, evalContext)) return null
        return PYDANTIC_DATACLASS_BYPASS_PARAMETERS
    }

    private fun shouldBypassDataclassTransform(cls: PyClass, context: TypeEvalContext): Boolean {
        if (cls.isPydanticDataclass) return false

        cls.qualifiedName?.let { qualifiedName ->
            if (qualifiedName in PYDANTIC_BASE_QUALIFIED_NAMES) return true
        }

        if (cls.isPydanticBaseModel || cls.isPydanticGenericModel || cls.isBaseSettings || cls.isPydanticCustomBaseModel) {
            return true
        }

        if (isPydanticModel(cls, includeDataclass = false, context = context)) return true
        if (isSubClassOfPydanticRootModel(cls, context)) return true
        if (isSubClassOfBaseSetting(cls, context)) return true

        val resolvedSuperClassMatch = cls.superClassExpressions
            .asSequence()
            .flatMap { expression -> resolveSuperClassExpressions(expression, context) }
            .any { resolvedClass ->
                resolvedClass.qualifiedName?.let { qualifiedName ->
                    if (qualifiedName in PYDANTIC_BASE_QUALIFIED_NAMES) return@any true
                }

                if (resolvedClass.isPydanticBaseModel || resolvedClass.isPydanticGenericModel || resolvedClass.isBaseSettings || resolvedClass.isPydanticCustomBaseModel) {
                    return@any true
                }

                if (isPydanticModel(resolvedClass, includeDataclass = false, context = context)) return@any true
                if (isSubClassOfPydanticRootModel(resolvedClass, context)) return@any true
                if (isSubClassOfBaseSetting(resolvedClass, context)) return@any true

                false
            }
        if (resolvedSuperClassMatch) return true

        return false
    }

    private fun resolveSuperClassExpressions(
        expression: PyExpression,
        context: TypeEvalContext,
    ): Sequence<PyClass> {
        val resolvedClasses = LinkedHashSet<PyClass>()
        when (expression) {
            is PyReferenceExpression -> {
                expression.reference
                    .multiResolve(false)
                    .asSequence()
                    .mapNotNull { it.element as? PyClass }
                    .forEach { resolvedClasses.add(it) }

                val type = context.getType(expression) as? PyClassLikeType
                type?.pyClassTypes
                    ?.asSequence()
                    ?.map { it.pyClass }
                    ?.forEach { resolvedClasses.add(it) }
            }

            is PySubscriptionExpression -> {
                val rootOperand = expression.rootOperand as? PyReferenceExpression
                if (rootOperand != null) {
                    resolveSuperClassExpressions(rootOperand, context).forEach { resolvedClasses.add(it) }
                }
            }
        }
        return resolvedClasses.asSequence()
    }

    private object PydanticType : PyDataclassParameters.Type {
        override val name: String = "pydantic"
        override val asPredefinedType: PyDataclassParameters.PredefinedType = PyDataclassParameters.PredefinedType.STD
    }

    private object PydanticDataclassBypassType : PyDataclassParameters.Type {
        override val name: String = "pydantic-base-model"
        override val asPredefinedType: PyDataclassParameters.PredefinedType? = null
    }

    private companion object {
        private val DATACLASS_QUALIFIED_NAME = QualifiedName.fromDottedString(DATA_CLASS_Q_NAME)
        private val DATACLASS_ARGUMENTS = listOf("init", "repr", "eq", "order", "unsafe_hash", "frozen", "config")

        private val PYDANTIC_BASE_QUALIFIED_NAMES =
            (CUSTOM_BASE_MODEL_Q_NAMES + listOf(BASE_MODEL_Q_NAME, GENERIC_MODEL_Q_NAME, ROOT_MODEL_Q_NAME, BASE_SETTINGS_Q_NAME))
                .toSet()

        private val PYDANTIC_DATACLASS_BYPASS_PARAMETERS = PyDataclassParameters(
            init = true,
            repr = true,
            eq = true,
            order = false,
            unsafeHash = false,
            frozen = false,
            matchArgs = true,
            kwOnly = false,
            initArgument = null,
            reprArgument = null,
            eqArgument = null,
            orderArgument = null,
            unsafeHashArgument = null,
            frozenArgument = null,
            matchArgsArgument = null,
            kwOnlyArgument = null,
            type = PydanticDataclassBypassType,
            others = emptyMap(),
        )
    }
}
