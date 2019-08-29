package com.koxudaxi.pydantic

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.jetbrains.python.psi.PyTargetExpression


open class PydanticCompletionTest : PydanticTestCase() {


    private fun doFieldTest(fieldNames: List<Pair<String, String>>) {
        configureByFile()

        val actual = myFixture!!.completeBasic().filter {
            it!!.psiElement is PyTargetExpression
        }.mapNotNull {

            Pair(it!!.lookupString, LookupElementPresentation.renderElement(it).typeText)
        }
        assertEquals(fieldNames, actual)
    }

    fun testKeywordArgument() {
        doFieldTest(
                listOf(
                        Pair("abc=", "str A"),
                        Pair("cde=", "str A"),
                        Pair("efg=", "str A")
                )
        )
    }

    fun testKeywordArgumentParent() {
        doFieldTest(
                listOf(
                        Pair("abc=", "str A"),
                        Pair("cde=", "str A"),
                        Pair("efg=", "str B")
                )
        )
    }

    fun testKeywordArgumentInserted() {
        doFieldTest(
                listOf(
                        Pair("cde=", "str A"),
                        Pair("efg=", "str A")
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

    fun testInstance() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("efg", "str=str('abc') A")
                )
        )
    }

    fun testInstanceParent() {
        doFieldTest(
                listOf(
                        Pair("abc", "str A"),
                        Pair("cde", "str A"),
                        Pair("efg", "str B")
                )
        )
    }

    fun testInstancePythonClass() {
        doFieldTest(
                listOf(
                        Pair("abc", "A"),
                        Pair("cde", "A"),
                        Pair("efg", "A")
                )
        )
    }

    fun testInstancePythonFunction() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testInstanceBroken() {
        doFieldTest(
                listOf(
                    Pair("abc", "str A"),
                    Pair("cde", "str=s A"),
                    Pair("efg", "Any A")
                )
        )
    }

    fun testInstanceParentIsPythonClass() {
        doFieldTest(
                listOf(
                        Pair("hij", "str B"),
                        Pair("abc", "A"),
                        Pair("cde", "A"),
                        Pair("efg", "A")
                )
        )
    }

    fun testInstanceUnResolve() {
        doFieldTest(
                listOf(
                )
        )
    }

    fun testString() {
        doFieldTest(
                listOf(
                )
        )
    }
}
