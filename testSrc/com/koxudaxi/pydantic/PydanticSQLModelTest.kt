package com.koxudaxi.pydantic

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.jetbrains.python.psi.PyTargetExpression

class PydanticSQLModelTest : PydanticInspectionBase() {

    companion object {
        // Additional excludes specific to SQLModel
        private val SQLMODEL_ADDITIONAL_EXCLUDES = listOf(
            "metadata",
            "__sqlmodel_relationships__",
            "__tablename__",
            "__config__",
            "__fields__"
        )
    }

    private fun doFieldTest(fieldNames: List<Pair<String, String>>, additionalModules: List<String>? = null) {
        configureByFile(additionalModules)
        val completions = myFixture!!.completeBasic() ?: emptyArray()
        val excludes = BASE_COMPLETION_EXCLUDES + SQLMODEL_ADDITIONAL_EXCLUDES
        val actual = completions
            .filter { it!!.psiElement is PyTargetExpression }
            .filterNot { excludes.contains(it!!.lookupString) }
            .map { Pair(it!!.lookupString, LookupElementPresentation.renderElement(it).typeText ?: "null") }
        assertEquals(fieldNames, actual)
    }

    fun testTableModelClassCompletion() {
        doFieldTest(
            listOf(
                Pair("id", "TableModel"),
                Pair("name", "TableModel")
            )
        )
    }

    fun testTableModelInstanceCompletion() {
        doFieldTest(
            listOf(
                Pair("id", "int TableModel"),
                Pair("name", "str TableModel")
            )
        )
    }

    fun testNonTableModelClassCompletion() {
        doFieldTest(listOf())
    }

    fun testNonTableModelInstanceCompletion() {
        doFieldTest(
            listOf(
                Pair("id", "int NonTableModel"),
                Pair("name", "str NonTableModel")
            )
        )
    }

    fun testInspection() {
        doTest()
    }

    fun testTypeInspection() {
        doTypeInspectionTest()
    }
}
