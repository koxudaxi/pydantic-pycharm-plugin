package com.koxudaxi.pydantic

import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.PyResolveUtil
import com.jetbrains.python.psi.types.TypeEvalContext

const val BASE_MODEL_Q_NAME = "pydantic.main.BaseModel"
const val DATA_CLASS_Q_NAME = "pydantic.dataclasses.dataclass"
const val VALIDATOR_Q_NAME = "pydantic.validator"
const val SCHEMA_Q_NAME = "pydantic.schema.Schema"
const val FIELD_Q_NAME = "pydantic.field.Field"

fun getPyClassByPyKeywordArgument(pyKeywordArgument: PyKeywordArgument): PyClass? {
    val pyCallExpression = pyKeywordArgument.parent?.parent as? PyCallExpression ?: return null
    return pyCallExpression.callee?.reference?.resolve() as? PyClass ?: return null
}

fun isPydanticModel(pyClass: PyClass, context: TypeEvalContext? = null): Boolean {
    return isSubClassOfPydanticBaseModel(pyClass, context) || isPydanticDataclass(pyClass)
}

fun isPydanticBaseModel(pyClass: PyClass): Boolean {
    return pyClass.qualifiedName == BASE_MODEL_Q_NAME
}

fun isSubClassOfPydanticBaseModel(pyClass: PyClass, context: TypeEvalContext?): Boolean {
    return pyClass.isSubclass(BASE_MODEL_Q_NAME, context)
}

fun hasDecorator(pyElement: PyElement, refName: String): Boolean {
        if (pyElement is PyDecoratable) {
            pyElement.decoratorList?.decorators?.mapNotNull { it.callee as? PyReferenceExpression }?.forEach {
                PyResolveUtil.resolveImportedElementQNameLocally(it).forEach {
                    decoratorQualifiedName -> if (decoratorQualifiedName == QualifiedName.fromDottedString(refName)) return true
                }
            }
        }
    return false
}

fun isPydanticDataclass(pyClass: PyClass): Boolean {
    return hasDecorator(pyClass, DATA_CLASS_Q_NAME)
}

fun isPydanticField(pyClass: PyClass, context: TypeEvalContext? = null): Boolean {
    return pyClass.isSubclass(SCHEMA_Q_NAME, context) || pyClass.isSubclass(FIELD_Q_NAME, context)
}

fun validatorMethod(pyFunction: PyFunction): Boolean {
    return hasDecorator(pyFunction, VALIDATOR_Q_NAME)
}