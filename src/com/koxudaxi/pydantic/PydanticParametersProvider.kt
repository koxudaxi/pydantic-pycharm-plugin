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
        val ellipsis = generator.createEllipsis()
        val parameters = mutableListOf(PyCallableParameterImpl.psi(generator.createSingleStarParameter()))
        parameters.addAll(DATACLASS_ARGUMENTS.map { name -> PyCallableParameterImpl.nonPsi(name, null, ellipsis) })
        return Triple(DATACLASS_QUALIFIED_NAME, PydanticType, parameters)
    }
    override fun getDataclassParameters(cls: PyClass, context: TypeEvalContext?): PyDataclassParameters? = null

    private object PydanticType : PyDataclassParameters.Type {
        override val name: String = "pydantic"
        override val asPredefinedType: PyDataclassParameters.PredefinedType = PyDataclassParameters.PredefinedType.STD
    }

    companion object {
        private val DATACLASS_QUALIFIED_NAME = QualifiedName.fromDottedString(DATA_CLASS_Q_NAME)
        private val DATACLASS_ARGUMENTS = listOf("init", "repr", "eq", "order", "unsafe_hash", "frozen", "config")
    }
}