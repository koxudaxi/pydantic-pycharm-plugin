package com.koxudaxi.pydantic

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.CustomImlComponentService
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.testFramework.fixtures.CodeInsightTestUtil
import com.jetbrains.python.psi.PyCallExpression

class PydanticAnnotatorTest : PydanticTestCase() {
    fun testInvalidDocStringFormat() {
        setDocStringFormat("EPYTEXT")
        try {
            configureByFile()

            val callExpression = assertOneElement(
                PsiTreeUtil.findChildrenOfType(myFixture!!.file, PyCallExpression::class.java)
                    .filter { it.text == "factory()" },
            )
            assertEmpty(CodeInsightTestUtil.testAnnotator(PydanticAnnotator(), callExpression))
        } finally {
            setDocStringFormat("REST")
        }
    }

    @Suppress("UnstableApiUsage")
    private fun setDocStringFormat(format: String) {
        val module = myFixture!!.module
        val project = myFixture!!.project
        runWriteAction {
            CustomImlComponentService.getInstance(project).setComponentValueBlocking(
                module,
                "PyDocumentationSettings",
                LegacyDocStringFormatState(format),
            )
        }
    }

    private class LegacyDocStringFormatState(var format: String = "")
}
