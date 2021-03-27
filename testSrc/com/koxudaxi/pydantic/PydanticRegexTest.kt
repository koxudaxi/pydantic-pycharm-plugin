package com.koxudaxi.pydantic

import com.intellij.lang.injection.InjectedLanguageManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.util.PsiUtilBase.getElementAtCaret

class PydanticRegexTest : PydanticTestCase() {
    fun testConstr() {
        doTestInjectedText( "[^a-zA-Z]+")
    }

    fun testField() {
        doTestInjectedText( "[^a-zA-Z]+")
    }
    fun testConstrMinLength() {
        doTestInjectedText( null)
    }

    fun testFieldTitle() {
        doTestInjectedText( null)
    }

    private fun doTestInjectedText(expected: String?): PsiElement? {
        configureByFile()
        val languageManager = InjectedLanguageManager.getInstance(myFixture!!.project)
        val host: PsiLanguageInjectionHost? = languageManager.getInjectionHost(getElementAtCaret(myFixture!!.editor)!!)
        if (expected == null) {
            assertNull(host)
            return null
        }
        assertNotNull(host)
        val files = languageManager.getInjectedPsiFiles(host!!)
        assertNotNull(files)
        assertFalse(files!!.isEmpty())
        val injected = files[0].getFirst()
        assertEquals(expected, injected.text)
        return injected
    }
}