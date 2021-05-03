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

    fun testNoArgumentsOnlyRequired() {
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

    fun testPartArgumentsOnlyRequired() {
        doTest(true)
    }

    fun testLastChar() {
        doTest(false)
    }

    fun testLastCharOnlyRequired() {
        doTest(true)
    }
}