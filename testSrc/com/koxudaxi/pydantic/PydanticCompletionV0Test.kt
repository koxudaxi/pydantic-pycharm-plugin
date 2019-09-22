package com.koxudaxi.pydantic

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.jetbrains.python.psi.PyTargetExpression


open class PydanticCompletionV0Test : PydanticTestCase(version = "v0") {


    private fun doFieldTest(fieldNames: List<Pair<String, String>>, additionalModules: List<String>? = null) {
        configureByFile(additionalModules)

        val actual = myFixture!!.completeBasic().filter {
            it!!.psiElement is PyTargetExpression
        }.mapNotNull {

            Pair(it!!.lookupString, LookupElementPresentation.renderElement(it).typeText ?: "null")
        }
        assertEquals(fieldNames, actual)
    }

    fun testFieldSchema() {
        doFieldTest(
                listOf(
                        Pair("a_id","str A"),
                        Pair("abc", "str A"),
                        Pair("b_id","str A"),
                        Pair("c_id","str A"),
                        Pair("cde", "str=str('abc') A"),
                        Pair("d_id", "str A"),
                        Pair("e_id", "str A"),
                        Pair("efg", "str=str('abc') A"),
                        Pair("f_id", "str A"),
                        Pair("g_id", "str=get_alias() A"),
                        Pair("hij", "Any A"),
                        Pair("___slots__", "BaseModel")
                )
        )
    }


    fun testKeywordArgumentSchema() {
        doFieldTest(
                listOf(
                        Pair("abc=", "str A"),
                        Pair("alias_a_id=", "str A"),
                        Pair("alias_b_id=", "str A"),
                        Pair("c_id=", "str A"),
                        Pair("cde=", "str=str('abc') A"),
                        Pair("d_id=", "str A"),
                        Pair("e_id=", "str A"),
                        Pair("efg=", "str=str('abc') A"),
                        Pair("f_id=", "str A"),
                        Pair("g_id=", "str=get_alias() A"),
                        Pair("hij=", "Any A"),
                        Pair("b_id", "null")
                )
        )
    }

}
