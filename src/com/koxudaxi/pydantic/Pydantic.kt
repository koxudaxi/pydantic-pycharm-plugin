package com.koxudaxi.pydantic

import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.documentation.PythonDocumentationProvider
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
    return isSubClassOfPydanticBaseModel(pyClass, context) || isPydanticDataclass(pyClass)
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
    pyDecoratable.decoratorList?.decorators?.mapNotNull { it.callee as? PyReferenceExpression }?.forEach {
        PyResolveUtil.resolveImportedElementQNameLocally(it).forEach { decoratorQualifiedName ->
            if (decoratorQualifiedName == QualifiedName.fromDottedString(refName)) return true
        }
    }
    return false
}

internal fun isPydanticDataclass(pyClass: PyClass): Boolean {
    return hasDecorator(pyClass, DATA_CLASS_Q_NAME)
}

internal fun isPydanticField(pyClass: PyClass, context: TypeEvalContext): Boolean {
    return pyClass.isSubclass(SCHEMA_Q_NAME, context) || pyClass.isSubclass(FIELD_Q_NAME, context)
}

internal fun validatorMethod(pyFunction: PyFunction): Boolean {
    return hasDecorator(pyFunction, VALIDATOR_Q_NAME)
}

internal fun getTypeHint(pyClass: PyClass, typeEvalContext: TypeEvalContext, pyTargetExpression: PyTargetExpression): String {
    val className = pyClass.qualifiedName ?: pyClass.name
    val defaultValue = pyTargetExpression.findAssignedValue()?.text?.let { "=$it" } ?: ""
    val typeHint = PythonDocumentationProvider.getTypeHint(typeEvalContext.getType(pyTargetExpression), typeEvalContext)
    return "${typeHint}$defaultValue $className"
}
