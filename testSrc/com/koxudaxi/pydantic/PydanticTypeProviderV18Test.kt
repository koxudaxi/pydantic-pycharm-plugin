package com.koxudaxi.pydantic

import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyCallExpression
import com.jetbrains.python.psi.PyFile
import com.jetbrains.python.psi.types.PyCallableType
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticTypeProviderV18Test : PydanticTestCase(version = "v18") {
    fun testGenericConstructorReferenceCallee() {
        assertConstructorFieldType("B()", "B", "int")
    }

    fun testGenericConstructorSubscriptionCallee() {
        assertConstructorFieldType("A[str]()", "A", "str")
    }

    private fun assertConstructorFieldType(expression: String, className: String, expectedType: String) {
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
        val context = TypeEvalContext.codeAnalysis(myFixture!!.project, file)
        val pyClass = file.topLevelClasses.single { it.name == className }
        val call = PsiTreeUtil.collectElementsOfType(file, PyCallExpression::class.java)
            .single { it.text == expression }
        // PyCharm 263 supplies the callee (B or A[str]), rather than the enclosing call.
        val prepared = PydanticTypeProvider().prepareCalleeTypeForCall(
            context.getType(pyClass), call.callee!!, context
        )?.get() as? PyCallableType
        assertNotNull("Generic constructor signature must be prepared", prepared)
        val field = prepared!!.getParameters(context)!!.single { it.name == "a" }
        assertEquals(expectedType, field.getType(context)?.name)
    }
}
