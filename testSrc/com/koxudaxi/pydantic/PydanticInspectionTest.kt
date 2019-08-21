package com.koxudaxi.pydantic


open class PydanticInspectionTest : PydanticTestCase() {

    private fun configureInspection() {
        myFixture!!.enableInspections(PydanticInspection::class.java)
        myFixture!!.checkHighlighting(true, false, true)

    }

    private fun doTest(fileName: String) {
        myFixture!!.configureByFile("testData/inspection/$fileName.py")
        configureInspection()
    }

    fun testAcceptsOnlyKeywordArguments() {
        doTest("accepts_only_keyword_arguments")
    }
    fun testAcceptsOnlyKeywordArgumentsDoubleStarArgument() {
        doTest("accepts_only_keyword_arguments_double_star_argument")
    }

    fun testAcceptsOnlyKeywordArgumentsKeywordArgument() {
        doTest("accepts_only_keyword_arguments_keyword_argument")
    }
}
