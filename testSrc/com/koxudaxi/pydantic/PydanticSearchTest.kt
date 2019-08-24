package com.koxudaxi.pydantic

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.search.searches.ReferencesSearch
import com.jetbrains.python.psi.PyKeywordArgument
import com.jetbrains.python.psi.PyTargetExpression
import org.jetbrains.annotations.NotNull


open class PydanticSearchTest : PydanticTestCase() {


    private fun searchByCaret(): MutableCollection<PsiReference> {
        configureByFile()
        val element = myFixture!!.elementAtCaret
        val query = ReferencesSearch.search(element)
        return query.findAll()
//        return query.mapNotNull {
//            it.resolve()
//        }.filter { it != element }
    }

    fun testField() {
        val elements = searchByCaret()
        assertEquals(elements.size, 4)
    }

    fun testKeywordArgument() {
        val elements = searchByCaret()
        assertEquals(elements.size, 1)
    }

    fun testChildField() {
        val elements = searchByCaret()
        assertEquals(elements.size, 3)
    }

    fun testChildKeywordArgument() {
        val elements = searchByCaret()
        assertEquals(elements.size, 1)
    }

    fun testChildrenField() {
        val elements = searchByCaret()
        assertEquals(elements.size, 4)
    }

    fun testChildrenKeywordArgument() {
        val elements = searchByCaret()
        assertEquals(elements.size, 1)
    }
}
