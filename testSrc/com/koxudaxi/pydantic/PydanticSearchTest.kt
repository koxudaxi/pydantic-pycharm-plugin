package com.koxudaxi.pydantic

import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch


open class PydanticSearchTest : PydanticTestCase() {


    private fun searchByCaret(): MutableCollection<PsiReference> {
        configureByFile()
        val element = myFixture!!.elementAtCaret
        val query = ReferencesSearch.search(element)
        return query.findAll()
    }

    private fun assertMatch(count: Int) {
        val elements = searchByCaret()
        assertEquals(elements.size, count)
    }

    fun testField() {
        assertMatch(4)
    }

    fun testKeywordArgument() {
        assertMatch(1)
    }

    fun testChildField() {
        assertMatch(3)
    }

    fun testChildKeywordArgument() {
        assertMatch(1)
    }

    fun testChildrenField() {
        assertMatch(3)
    }

    fun testChildrenKeywordArgument() {
        assertMatch(1)
    }


    fun testGrandChildField() {
        assertMatch(3)
    }

    fun testGrandChildKeywordArgument() {
        assertMatch(1)
    }

    fun testGrandChildKeywordArgumentNotFound() {
        assertMatch(0)
    }

    fun testGrandChildKeywordArgumentWithPythonClass() {
        assertMatch(0)
    }

    fun testGrandChildFieldWithPythonClass() {
        assertMatch(2)
    }

    fun testMultipleInheritanceField() {
        assertMatch(3)
    }

    fun testMultipleInheritanceKeywordArgument() {
        assertMatch(1)
    }

    fun testPythonClassChildField() {
        assertMatch(2)
    }

    fun testPythonClassChildKeywordArgument() {
        assertMatch(0)
    }

    fun testUnResolve() {
        assertMatch(0)
    }
}
