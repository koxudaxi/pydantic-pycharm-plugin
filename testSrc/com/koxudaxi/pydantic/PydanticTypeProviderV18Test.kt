package com.koxudaxi.pydantic

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.impl.PyCallExpressionHelper
import com.jetbrains.python.psi.resolve.PyResolveContext
import com.jetbrains.python.psi.types.PyCallableType
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticTypeProviderV18Test : PydanticTestCase(version = "v18") {
    fun testGenericConstructorReferenceCallee() {
        assertConstructorFieldType("B()", "B", "int")
    }

    fun testGenericConstructorSubscriptionCallee() {
        assertConstructorFieldType("A[str]()", "A", "str")
    }

    fun testGenericConstructorReferenceThroughSdk() {
        assertSdkConstructorFieldType("B()", "int")
    }

    fun testGenericConstructorSubscriptionThroughSdk() {
        assertSdkConstructorFieldType("A[str]()", "str")
    }

    private fun assertSdkConstructorFieldType(expression: String, expectedType: String) {
        val call = configureConstructor(expression)
        val context = TypeEvalContext.codeAnalysis(myFixture!!.project, call.containingFile)
        // Use the SDK entry point used by ordinary call evaluation, without invoking our provider directly.
        val callable = PyCallExpressionHelper.getCalleeType(
            call.callee!!, PyResolveContext.defaultContext(context)
        ) as? PyCallableType
        assertNotNull("SDK must resolve the generic constructor signature", callable)
        val field = callable!!.getParameters(context)!!.single { it.name == "a" }
        assertEquals(expectedType, field.getType(context)?.name)
    }

    private fun configureConstructor(expression: String): PyCallExpression {
        val file = myFixture!!.configureByText("model.py", """
            from typing import Generic, TypeVar
            from pydantic.generics import GenericModel

            T = TypeVar("T")

            class A(GenericModel, Generic[T]):
                a: T

            class B(A[int]):
                pass

            result = $expression
        """.trimIndent()) as PyFile
        return PsiTreeUtil.collectElementsOfType(file, PyCallExpression::class.java)
            .single { it.text == expression }
    }

    private fun assertConstructorFieldType(expression: String, className: String, expectedType: String) {
        val call = configureConstructor(expression)
        val file = call.containingFile as PyFile
        val context = TypeEvalContext.codeAnalysis(myFixture!!.project, file)
        val pyClass = file.topLevelClasses.single { it.name == className }
        // PyCharm 263 supplies the callee (B or A[str]), rather than the enclosing call.
        val prepared = PydanticTypeProvider().prepareCalleeTypeForCall(
            context.getType(pyClass), call.callee!!, context
        )?.get() as? PyCallableType
        assertNotNull("Generic constructor signature must be prepared", prepared)
        val field = prepared!!.getParameters(context)!!.single { it.name == "a" }
        assertEquals(expectedType, field.getType(context)?.name)
    }
}
