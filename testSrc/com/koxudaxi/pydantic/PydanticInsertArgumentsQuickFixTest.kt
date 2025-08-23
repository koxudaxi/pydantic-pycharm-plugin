package com.koxudaxi.pydantic

import com.intellij.openapi.command.WriteCommandAction

class PydanticInsertArgumentsQuickFixTest : PydanticTestCase() {
    private fun checkResultByFile() {
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")

    }

    private fun doTest(onlyRequired: Boolean) {
        configureByFile()
        val quickFix = PydanticInsertArgumentsQuickFix(onlyRequired)
        WriteCommandAction.runWriteCommandAction(myFixture!!.project) {
            quickFix.invoke(myFixture!!.project, myFixture!!.editor, myFixture!!.file)
        }
        checkResultByFile()
    }

    fun testNoArguments() {
        doTest(false)
    }

    // TODO: InsertArgumentsQuickFix for required fields not working properly in PyCharm 2025.2
    fun _disabled_testNoArgumentsOnlyRequired() {
        doTest(true)
    }

    fun testPartArguments() {
        doTest(false)
    }

    fun testNestedPartArguments() {
        doTest(false)
    }

    fun testNestedOtherObject() {
        doTest(false)
    }

    // TODO: Partial arguments quick fix broken in PyCharm 2025.2
    fun _disabled_testPartArgumentsOnlyRequired() {
        doTest(true)
    }

    fun testLastChar() {
        doTest(false)
    }

    // TODO: Last character quick fix broken in PyCharm 2025.2
    fun _disabled_testLastCharOnlyRequired() {
        doTest(true)
    }

    fun testDataclass() {
        doTest(false)
    }
}