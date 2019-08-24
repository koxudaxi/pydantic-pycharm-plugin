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
}
