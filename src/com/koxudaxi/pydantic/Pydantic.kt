package com.koxudaxi.pydantic

import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.codeInsight.typing.PyTypingTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionImpl
import com.jetbrains.python.psi.impl.PyTargetExpressionImpl
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.resolve.PyResolveUtil
import com.jetbrains.python.psi.types.*

const val BASE_MODEL_Q_NAME = "pydantic.main.BaseModel"
const val DATA_CLASS_Q_NAME = "pydantic.dataclasses.dataclass"
const val VALIDATOR_Q_NAME = "pydantic.validator"
const val SCHEMA_Q_NAME = "pydantic.schema.Schema"
const val FIELD_Q_NAME = "pydantic.fields.Field"
const val BASE_SETTINGS_Q_NAME = "pydantic.env_settings.BaseSettings"

internal fun getPyClassByPyKeywordArgument(pyKeywordArgument: PyKeywordArgument, context: TypeEvalContext): PyClass? {
    val pyCallExpression = PsiTreeUtil.getParentOfType(pyKeywordArgument, PyCallExpression::class.java) ?: return null
    val callee = pyCallExpression.callee ?: return null
    val pyType = when (val type = context.getType(callee)) {
        is PyClass -> return type
        is PyClassType -> type
        else -> (callee.reference?.resolve() as? PyTypedElement)?.let { context.getType(it) } ?: return null
    }
    return getPyClassTypeByPyTypes(pyType).firstOrNull { isPydanticModel(it.pyClass) }?.pyClass
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

internal fun isValidatorMethod(pyFunction: PyFunction): Boolean {
    return hasDecorator(pyFunction, VALIDATOR_Q_NAME)
}

internal fun getClassVariables(pyClass: PyClass, context: TypeEvalContext): Sequence<PyTargetExpression> {
    return pyClass.classAttributes
            .asReversed()
            .asSequence()
            .filterNot { PyTypingTypeProvider.isClassVar(it, context) }
}

internal fun getAliasedFieldName(field: PyTargetExpression, context: TypeEvalContext): String? {
    val fieldName = field.name
    val assignedValue = field.findAssignedValue() ?: return fieldName
    val callee = (assignedValue as? PyCallExpressionImpl)?.callee ?: return fieldName
    val referenceExpression = callee.reference?.element as? PyReferenceExpression ?: return fieldName


    val resolveResults = getResolveElements(referenceExpression, context)
    return PyUtil.filterTopPriorityResults(resolveResults)
            .mapNotNull { PsiTreeUtil.getContextOfType(it, PyClass::class.java) }
            .filter { isPydanticField(it, context) }
            .mapNotNull {
                when (val alias = assignedValue.getKeywordArgument("alias")) {
                    is StringLiteralExpression -> alias.stringValue
                    is PyReferenceExpression -> ((alias.reference.resolve() as? PyTargetExpressionImpl)
                            ?.findAssignedValue() as? StringLiteralExpression)?.stringValue
                    //TODO Support dynamic assigned Value. eg:  Schema(..., alias=get_alias_name(field_name))
                    else -> null
                }
            }
            .firstOrNull() ?: fieldName
}


internal fun getResolveElements(referenceExpression: PyReferenceExpression, context: TypeEvalContext): Array<ResolveResult> {
    val resolveContext = PyResolveContext.noImplicits().withTypeEvalContext(context)
    return referenceExpression.getReference(resolveContext).multiResolve(false)

}

internal fun getPyClassTypeByPyTypes(pyType: PyType): List<PyClassType> {
    return when (pyType) {
        is PyUnionType ->
            pyType.members
                    .mapNotNull { it }
                    .flatMap {
                        getPyClassTypeByPyTypes(it)
                    }

        is PyCollectionType ->
            pyType.elementTypes
                    .mapNotNull { it }
                    .flatMap {
                        getPyClassTypeByPyTypes(it)
                    }
        is PyClassType -> listOf(pyType)
        else -> listOf()
    }
}