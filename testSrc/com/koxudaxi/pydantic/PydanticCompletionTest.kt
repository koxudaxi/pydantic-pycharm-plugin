package com.koxudaxi.pydantic

import com.jetbrains.python.psi.PyTargetExpression


open class PydanticCompletionTest : PydanticTestCase() {


    private fun doFieldTest(fieldNames: List<String>) {
        configureByFile()


        val actual = myFixture!!.completeBasic().filter {
            it!!.psiElement is PyTargetExpression
        }.mapNotNull {
            it!!.lookupString
        }
        assertEquals(fieldNames, actual)
    }

    fun testKeywordArgument() {
        doFieldTest(
                listOf(
                        "abc=",
                        "cde=",
                        "efg="
                )
        )
    }

    fun testKeywordArgumentParent() {
        doFieldTest(
                listOf(
                        "abc=",
                        "cde=",
                        "efg="
                )
        )
    }

    fun testKeywordArgumentInserted() {
        doFieldTest(
                listOf(
                        "cde=",
                        "efg="
                )
        )
    }

    fun testKeywordArgumentAssignValue() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testKeywordArgumentPythonClass() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testKeywordArgumentPythonFunction() {
        doFieldTest(
                listOf(
                )
        )
    }
}
