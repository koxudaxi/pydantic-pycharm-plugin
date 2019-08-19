package com.koxudaxi.pydantic

import com.intellij.psi.util.QualifiedName
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.resolve.PyResolveUtil
import com.jetbrains.python.psi.types.PyCallableTypeImpl
import com.jetbrains.python.psi.types.TypeEvalContext


fun getPyClassByPyKeywordArgument(pyKeywordArgument: PyKeywordArgument): PyClass? {
    val pyCallExpression = pyKeywordArgument.parent?.parent as? PyCallExpression ?: return null
    return pyCallExpression.callee?.reference?.resolve() as? PyClass ?: return null
}

fun isPydanticModel(pyClass: PyClass, context: TypeEvalContext? = null): Boolean {
    return isSubClassOfPydanticBaseModel(pyClass, context) || isPydanticDataclass(pyClass)
}

fun isPydanticBaseModel(pyClass: PyClass): Boolean {
    return pyClass.qualifiedName == "pydantic.main.BaseModel"
}

fun isSubClassOfPydanticBaseModel(pyClass: PyClass, context: TypeEvalContext?): Boolean {
    return pyClass.isSubclass("pydantic.main.BaseModel", context)
}

fun isPydanticDataclass(pyClass: PyClass): Boolean {
    val decorators = pyClass.decoratorList?.decorators ?: return false
    for (decorator in decorators) {
        val callee = (decorator.callee as? PyReferenceExpression) ?: continue

        for (decoratorQualifiedName in PyResolveUtil.resolveImportedElementQNameLocally(callee)) {
            if (decoratorQualifiedName == QualifiedName.fromDottedString("pydantic.dataclasses.dataclass")) return true
        }
    }
    return false
}

fun isPydanticField(pyClass: PyClass, context: TypeEvalContext? = null): Boolean {
    return pyClass.isSubclass("pydantic.schema.Schema", context) || pyClass.isSubclass("pydantic.field.Field", context)
}

fun hasClassMethodDecorator(pyFunction: PyFunction, context: TypeEvalContext): Boolean {
    return pyFunction.decoratorList?.decorators?.firstOrNull { pyDecorator ->
        (context.getType(pyDecorator) as? PyCallableTypeImpl)?.getReturnType(context)?.name == "classmethod" && pyDecorator.name != "classmethod"
    } != null
}