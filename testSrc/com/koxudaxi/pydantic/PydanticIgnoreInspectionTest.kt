package com.koxudaxi.pydantic

import com.intellij.psi.PsiReference
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyElement
import com.jetbrains.python.psi.PyFunction
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.types.TypeEvalContext
import junit.framework.TestCase


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
        val psiElement = myFixture!!.file.findElementAt(myFixture!!.caretOffset)
        val pyStringLiteralExpression = PsiTreeUtil.getParentOfType(psiElement, PyStringLiteralExpression::class.java) as PyStringLiteralExpression
        val psiReference = pyStringLiteralExpression.reference
        if (psiReference == null){
            assertFalse(expected)
            return
        }
        val context = TypeEvalContext.codeInsightFallback(myFixture!!.project)
        val invalidElement = PydanticIgnoreInspection().ignoreUnresolvedReference(pyStringLiteralExpression.parent as PyElement, psiReference, context)
        assertFalse(invalidElement)

        val actual = PydanticIgnoreInspection().ignoreUnresolvedReference(pyStringLiteralExpression, psiReference, context)
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
