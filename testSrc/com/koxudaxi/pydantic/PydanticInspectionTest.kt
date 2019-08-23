package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import kotlin.reflect.KClass


open class PydanticInspectionTest : PydanticTestCase() {

    @Suppress("UNCHECKED_CAST")
    protected open val inspectionClass: KClass<PyInspection> = PydanticInspection::class as KClass<PyInspection>

    private fun configureInspection() {
        myFixture!!.enableInspections(inspectionClass.java)
        myFixture!!.checkHighlighting(true, false, true)

    }

    private fun doTest() {
        configureByFile()
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
