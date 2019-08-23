package com.koxudaxi.pydantic


open class PydanticRefactorTest : PydanticTestCase() {


    private fun checkResultByFile() {
        myFixture!!.checkResultByFile("${testDataMethodPath}_after.py")

    }

    private fun doRefactorTest(newName: String = "cde", isApplicable: Boolean = true) {
        configureByFile()
        val pydanticFieldRenameFactory = PydanticFieldRenameFactory()
        assertEquals(pydanticFieldRenameFactory.isApplicable(myFixture!!.elementAtCaret), isApplicable)

        if (!isApplicable) return

        pydanticFieldRenameFactory.createRenamer(myFixture!!.elementAtCaret, newName, ArrayList()).renames.forEach { (t, u) ->
            if (t.name != newName) {
                myFixture!!.renameElement(t, u)
            }
        }
        checkResultByFile()
    }

    fun testRenameField() {
        doRefactorTest()
    }

    fun testRenameKeywordArgument() {
        doRefactorTest()
    }

    fun testRenameFieldDataclass() {
        doRefactorTest()
    }

    fun testRenameKeywordArgumentDataclass() {
        doRefactorTest()
    }

    fun testRenamePythonClass() {
        doRefactorTest(isApplicable = false)
    }

    fun testRenamePythonClassKeywordArgument() {
        doRefactorTest(isApplicable = false)
    }

    fun testRenamePythonFunction() {
        doRefactorTest(isApplicable = false)
    }

    fun testRenamePythonFunctionKeywordArgument() {
        doRefactorTest(isApplicable = false)
    }

    fun testGetOptionName() {
        assertEquals(PydanticFieldRenameFactory().optionName, "Rename fields in hierarchy")
    }

    fun testIsEnabled() {
        assertTrue(PydanticFieldRenameFactory().isEnabled)
    }

    fun testSetEnabled() {
        val pydanticFieldRenameFactory = PydanticFieldRenameFactory()
        pydanticFieldRenameFactory.isEnabled = false
        assertFalse(pydanticFieldRenameFactory.isEnabled)
    }

    fun testRenamer() {
        configureByFile()
        val pydanticFieldRenameFactory = PydanticFieldRenameFactory()
        val renamer = pydanticFieldRenameFactory.createRenamer(myFixture!!.elementAtCaret, "newName", ArrayList())
        assertEquals(renamer.dialogTitle, "Rename Fields")
        assertEquals(renamer.dialogDescription, "Rename field in hierarchy to:")
        assertEquals(renamer.entityName(), "Field")
        assertEquals(renamer.isSelectedByDefault, true)
    }
}
