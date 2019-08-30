package com.koxudaxi.pydantic

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.PyResolveUtil
import com.jetbrains.python.psi.types.TypeEvalContext

const val BASE_MODEL_Q_NAME = "pydantic.main.BaseModel"
const val DATA_CLASS_Q_NAME = "pydantic.dataclasses.dataclass"
const val VALIDATOR_Q_NAME = "pydantic.validator"
const val SCHEMA_Q_NAME = "pydantic.schema.Schema"
const val FIELD_Q_NAME = "pydantic.field.Field"
const val BASE_SETTINGS_Q_NAME = "pydantic.env_settings.BaseSettings"

internal fun getPyClassByPyCallExpression(pyCallExpression: PyCallExpression): PyClass? {
    return pyCallExpression.callee?.reference?.resolve() as? PyClass
}

internal fun getPyClassByPyKeywordArgument(pyKeywordArgument: PyKeywordArgument): PyClass? {
    val pyCallExpression = PsiTreeUtil.getParentOfType(pyKeywordArgument, PyCallExpression::class.java) ?: return null
    return getPyClassByPyCallExpression(pyCallExpression)
}

internal fun isPydanticModel(pyClass: PyClass, context: TypeEvalContext? = null): Boolean {
    return (isSubClassOfPydanticBaseModel(pyClass, context) || isPydanticDataclass(pyClass)) && !isPydanticBaseModel(pyClass)
}

internal fun isPydanticBaseModel(pyClass: PyClass): Boolean {
    return pyClass.qualifiedName == BASE_MODEL_Q_NAME
}

internal fun isSubClassOfPydanticBaseModel(pyClass: PyClass, context: TypeEvalContext?): Boolean {
    return pyClass.isSubclass(BASE_MODEL_Q_NAME, context)
}

internal fun isBaseSetting(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(BASE_SETTINGS_Q_NAME, context)
}

internal fun hasDecorator(pyDecoratable: PyDecoratable, refName: String): Boolean {
    return pyDecoratable.decoratorList?.decorators?.let { decorators ->
        decorators.mapNotNull { it.callee as? PyReferenceExpression }.any {
            PyResolveUtil.resolveImportedElementQNameLocally(it).any { decoratorQualifiedName ->
                decoratorQualifiedName == QualifiedName.fromDottedString(refName)
            }
        }
    } ?: false
}

internal fun isPydanticDataclass(pyClass: PyClass): Boolean {
    return hasDecorator(pyClass, DATA_CLASS_Q_NAME)
}

internal fun isPydanticField(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(SCHEMA_Q_NAME, context) || pyClass.isSubclass(FIELD_Q_NAME, context)
}

internal fun isValidatorMethod(pyFunction: PyFunction): Boolean {
    return hasDecorator(pyFunction, VALIDATOR_Q_NAME)
}

internal fun getClassVariables(pyClass: PyClass, context: TypeEvalContext): Sequence<PyTargetExpression> {
    return pyClass.classAttributes
            .asReversed()
            .asSequence()
            .filterNot { PyTypingTypeProvider.isClassVar(it, context) }
}
