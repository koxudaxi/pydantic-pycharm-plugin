package com.koxudaxi.pydantic

import com.intellij.openapi.project.Project
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.codeInsight.PyDataclassParameters
import com.jetbrains.python.codeInsight.PyDataclassParametersProvider
import com.jetbrains.python.psi.PyClass
import com.jetbrains.python.psi.PyElementGenerator
import com.jetbrains.python.psi.types.PyCallableParameter
import com.jetbrains.python.psi.types.PyCallableParameterImpl
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticParametersProvider : PyDataclassParametersProvider {

    override fun getType(): PyDataclassParameters.Type = PydanticType

    override fun getDecoratorAndTypeAndParameters(project: Project): Triple<QualifiedName, PyDataclassParameters.Type, List<PyCallableParameter>> {
        val generator = PyElementGenerator.getInstance(project)
        val parameters = mutableListOf(PyCallableParameterImpl.psi(generator.createSingleStarParameter()))
        parameters.addAll(DATACLASS_ARGUMENTS.map { name -> PyCallableParameterImpl.nonPsi(name, null, null) })
        return Triple(DATACLASS_QUALIFIED_NAME, PydanticType, parameters)
    }

    override fun getDataclassParameters(cls: PyClass, context: TypeEvalContext?): PyDataclassParameters? {
        if (!isPydanticBaseClass(cls)) return null
        return PYDANTIC_DATACLASS_BYPASS_PARAMETERS
    }

    private fun isPydanticBaseClass(pyClass: PyClass): Boolean {
        pyClass.qualifiedName?.let { qualifiedName ->
            if (qualifiedName in PYDANTIC_BASE_QUALIFIED_NAMES) return true
        }

        if (pyClass.isPydanticBaseModel || pyClass.isPydanticGenericModel || pyClass.isBaseSettings || pyClass.isPydanticCustomBaseModel) {
            return true
        }

        return false
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
            (CUSTOM_BASE_MODEL_Q_NAMES + listOf(BASE_MODEL_Q_NAME, GENERIC_MODEL_Q_NAME, ROOT_MODEL_Q_NAME) + BASE_SETTINGS_Q_NAMES)
                .toSet()

        private val PYDANTIC_DATACLASS_BYPASS_PARAMETERS: PyDataclassParameters by lazy {
            // Use reflection to handle constructor differences between 2025.3 and 2026.1:
            // - 2025.3: 20 params, frozen: boolean, order: ...kwOnly, initArg...kwOnlyArg, type, slots, slotsArg, others
            // - 2026.1: 21 params, frozen: Boolean?, order: ...kwOnly, slots, initArg...slotsArg, type, others, fieldSpecifiers
            val constructors = PyDataclassParameters::class.java.constructors
            val primaryCtor = constructors
                .filter { it.parameterCount in 20..22 }
                .minByOrNull { it.parameterCount }!!
            when (primaryCtor.parameterCount) {
                20 -> {
                    // 2025.3: init, repr, eq, order, unsafeHash, frozen(boolean), matchArgs, kwOnly,
                    //         initArg...kwOnlyArg, type, slots, slotsArg, others
                    @Suppress("UNCHECKED_CAST")
                    primaryCtor.newInstance(
                        true, true, true, false, false, false, true, false,
                        null, null, null, null, null, null, null, null,
                        PydanticDataclassBypassType, false, null, emptyMap<String, Any>(),
                    ) as PyDataclassParameters
                }
                21 -> {
                    // 2026.1+: init, repr, eq, order, unsafeHash, frozen(Boolean?), matchArgs, kwOnly, slots,
                    //          initArg...slotsArg, type, others, fieldSpecifiers
                    @Suppress("UNCHECKED_CAST")
                    primaryCtor.newInstance(
                        true, true, true, false, false, java.lang.Boolean.FALSE, true, false, false,
                        null, null, null, null, null, null, null, null, null,
                        PydanticDataclassBypassType, emptyMap<String, Any>(), emptyList<Any>(),
                    ) as PyDataclassParameters
                }
                else -> throw IllegalStateException(
                    "Unsupported PyDataclassParameters constructor arity: ${primaryCtor.parameterCount}"
                )
            }
        }
    }
}
