package com.koxudaxi.pydantic

import com.intellij.psi.PsiElement
import com.jetbrains.python.codeInsight.stdlib.PyDataclassTypeProvider
import com.jetbrains.python.psi.*
import com.jetbrains.python.psi.impl.PyCallExpressionImpl
import com.jetbrains.python.psi.impl.PyCallExpressionNavigator
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
    private val pydanticTypeProvider = PydanticTypeProvider()
    override fun getReferenceExpressionType(
        referenceExpression: PyReferenceExpression,
        context: TypeEvalContext,
    ): PyType? {
        return getPydanticDataclass(
            referenceExpression,
            TypeEvalContext.codeInsightFallback(referenceExpression.project)
        )
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

    private fun getPydanticDataclassType(
        referenceTarget: PsiElement,
        context: TypeEvalContext,
        pyReferenceExpression: PyReferenceExpression,
        definition: Boolean,
    ): PyType? {
        val callSite = PyCallExpressionNavigator.getPyCallExpressionByCallee(pyReferenceExpression)
        val dataclassCallableType = getDataclassCallableType(referenceTarget, context, callSite) ?: return null

        val dataclassType = (dataclassCallableType).getReturnType(context) as? PyClassType ?: return null
        if (!dataclassType.pyClass.isPydanticDataclass) return null
        val ellipsis = PyElementGenerator.getInstance(referenceTarget.project).createEllipsis()
        val injectedPyCallableType = PyCallableTypeImpl(
            dataclassCallableType.getParameters(context)?.map {
                when {
                    it.defaultValueText == "..." && it.defaultValue is PyNoneLiteralExpression ->
                        pydanticTypeProvider.injectDefaultValue(dataclassType.pyClass, it, ellipsis, null, context)
                            ?: it

                    else -> it
                }
            }, dataclassType
        )
        val injectedDataclassType = (injectedPyCallableType).getReturnType(context) as? PyClassType ?: return null
        return when {
            callSite is PyCallExpression && definition -> injectedPyCallableType
            definition -> injectedDataclassType.toClass()
            else -> injectedDataclassType
        }
    }

    private fun getPydanticDataclass(referenceExpression: PyReferenceExpression, context: TypeEvalContext): PyType? {
        return getResolvedPsiElements(referenceExpression, context)
            .asSequence()
            .mapNotNull {
                when {
                    it is PyClass && it.isPydanticDataclass ->
                        getPydanticDataclassType(it, context, referenceExpression, true)

                    it is PyTargetExpression -> (it as? PyTypedElement)
                        ?.getType(context)?.pyClassTypes
                        ?.filter { pyClassType -> pyClassType.pyClass.isPydanticDataclass }
                        ?.firstNotNullOfOrNull { pyClassType ->
                            getPydanticDataclassType(
                                pyClassType.pyClass,
                                context,
                                referenceExpression,
                                pyClassType.isDefinition
                            )
                        }

                    else -> null
                }
            }.firstOrNull()
    }
}
