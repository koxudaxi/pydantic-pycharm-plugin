package com.koxudaxi.pydantic

import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.types.TypeEvalContext


open class PydanticIgnoreInspectionTest : PydanticTestCase() {


    private fun doTest(expected: Boolean) {
        configureByFile()
        val pyFunction = myFixture!!.elementAtCaret as PyFunction
        val context = TypeEvalContext.codeInsightFallback(myFixture!!.project)
        val actual = PydanticIgnoreInspection().ignoreMethodParameters(pyFunction, context)
        assertEquals(expected, actual)
    }

    fun testValidator() {
        doTest(true)
    }

    fun testValidatorFullPath() {
        doTest(true)
    }

    fun testValidatorDataclass() {
        doTest(true)
    }


    fun testBaseModel() {
        doTest(false)
    }

    fun testPythonFunction() {
        doTest(false)
    }

    fun testPythonMethod() {
        doTest(false)
    }

    fun testPythonDecorator() {
        doTest(false)
    }
}
