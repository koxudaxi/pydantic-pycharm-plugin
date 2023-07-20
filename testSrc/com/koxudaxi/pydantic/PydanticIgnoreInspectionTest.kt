package com.koxudaxi.pydantic

import com.intellij.psi.PsiReference
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.types.TypeEvalContext


open class PydanticIgnoreInspectionTest : PydanticTestCase() {


    private fun doIgnoreMethodParametersTest(expected: Boolean) {
        configureByFile()
        val pyFunction = myFixture!!.elementAtCaret as PyFunction
        val context = TypeEvalContext.codeInsightFallback(myFixture!!.project)
        val actual = PydanticIgnoreInspection().ignoreMethodParameters(pyFunction, context)
        assertEquals(expected, actual)
    }

    private fun doIgnoreUnresolvedReference(expected: Boolean) {
        configureByFile()
        val pyElement = myFixture!!.elementAtCaret as PyElement
        val psiReference = pyElement.reference as PsiReference
        val context = TypeEvalContext.codeInsightFallback(myFixture!!.project)
        val actual = PydanticIgnoreInspection().ignoreUnresolvedReference(pyElement, psiReference, context)
        assertEquals(expected, actual)
    }

    fun testValidator() {
        doIgnoreMethodParametersTest(true)
    }

    fun testValidatorFullPath() {
        doIgnoreMethodParametersTest(true)
    }

    fun testValidatorDataclass() {
        doIgnoreMethodParametersTest(true)
    }


    fun testBaseModel() {
        doIgnoreMethodParametersTest(false)
    }

    fun testPythonFunction() {
        doIgnoreMethodParametersTest(false)
    }

    fun testPythonMethod() {
        doIgnoreMethodParametersTest(false)
    }

    fun testPythonDecorator() {
        doIgnoreMethodParametersTest(false)
    }
    fun testValidatorField() {
        doIgnoreUnresolvedReference(true)
    }

    fun testDecoratorField() {
        doIgnoreUnresolvedReference(false)
    }
}
