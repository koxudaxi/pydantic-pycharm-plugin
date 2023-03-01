package com.koxudaxi.pydantic

import com.intellij.psi.PsiElement
import com.jetbrains.python.codeInsight.stdlib.PyDataclassTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionImpl
import com.jetbrains.python.psi.types.*

/**
 *  `PydanticDataclassTypeProvider` gets actual pydantic dataclass types
 *
 *  PyCharm 2021.1 detects decorated object type by type-hint of decorators.
 *  Unfortunately, pydantic.dataclasses.dataclass returns `Dataclass` type.
 *  `Dataclass` is not an actual model, which is Stub type for static-type checking.
 *  But, PyCharm can detect actual dataclass type by parsing the type definition.
 *  `PydanticDataclassTypeProvider` ignore `Dataclass` and get actual dataclass type using `PyDataclassTypeProvider`
 *
 */
class PydanticDataclassTypeProvider : PyTypeProviderBase() {
    private val pyDataclassTypeProvider = PyDataclassTypeProvider()

    override fun getCallableType(callable: PyCallable, context: TypeEvalContext): PyType? {
        if (callable is PyFunction && callable.isPydanticDataclass) {
            // Drop fake dataclass return type
            return PyCallableTypeImpl(callable.getParameters(context), null)
        }
        return super.getCallableType(callable, context)
    }

    internal fun getDataclassCallableType(
        referenceTarget: PsiElement,
        context: TypeEvalContext,
        callSite: PyCallExpression?,
    ): PyCallableType? {
        return pyDataclassTypeProvider.getReferenceType(
            referenceTarget,
            context,
            callSite ?: PyCallExpressionImpl(referenceTarget.node)
        )?.get() as? PyCallableType
    }
}
