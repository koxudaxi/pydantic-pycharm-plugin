package com.koxudaxi.pydantic

import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.openapi.application.runWriteAction
import com.intellij.psi.util.PsiTreeUtil
import com.jetbrains.python.psi.PyReferenceExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.types.TypeEvalContext

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

    private fun getReferenceExpressionNearCaret(): PyReferenceExpression {
        val element = myFixture!!.file.findElementAt((myFixture!!.caretOffset - 1).coerceAtLeast(0))
        return PsiTreeUtil.getParentOfType(element, PyReferenceExpression::class.java, false)!!
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

    fun testIncompleteTableModelClassAttributeType() {
        configureByFile()
        val referenceExpression = getReferenceExpressionNearCaret()
        val context = TypeEvalContext.codeInsightFallback(myFixture!!.project)

        assertNull(PydanticTypeProvider().getReferenceExpressionType(referenceExpression, context))
    }

    fun testTableModelClassAttributeWithoutInstrumentedAttributeStub() {
        runWriteAction {
            myFixture!!.findFileInTempDir("package/sqlalchemy/orm/attributes.py")!!.delete(this)
        }
        configureByFile()
        val referenceExpression = getReferenceExpressionNearCaret()
        val context = TypeEvalContext.codeInsightFallback(myFixture!!.project)

        assertNull(PydanticTypeProvider().getReferenceExpressionType(referenceExpression, context))
    }
}
