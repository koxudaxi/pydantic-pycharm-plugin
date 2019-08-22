package com.koxudaxi.pydantic


open class PydanticInspectionTest : PydanticTestCase() {

    private fun configureInspection() {
        myFixture!!.enableInspections(PydanticInspection::class.java)
        myFixture!!.checkHighlighting(true, false, true)

    }

    private fun doTest() {
        myFixture!!.configureByFile("testData/inspection/" + getTestName(true) + ".py")
        configureInspection()
    }

    fun testPythonClass() {
        doTest()
    }

    fun testAcceptsOnlyKeywordArguments() {
        doTest()
    }

    fun testAcceptsOnlyKeywordArgumentsSingleStarArgument() {
        doTest()
    }

    fun testAcceptsOnlyKeywordArgumentsDoubleStarArgument() {
        doTest()
    }


    fun testAcceptsOnlyKeywordArgumentsKeywordArgument() {
        doTest()
    }
    fun testValidatorSelf() {
        doTest()
    }
}
