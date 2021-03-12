package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import kotlin.reflect.KClass


open class PydanticInspectionTest : PydanticInspectionBase() {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PydanticInspection::class as KClass<PyInspection>

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

    fun testAcceptsOnlyKeywordFunction() {
        doTest()
    }

    fun testValidatorSelf() {
        doTest()
    }

    fun testRootValidatorSelf() {
        doTest()
    }

    fun testOrmMode() {
        doTest()
    }

    fun testReadOnlyProperty() {
        doTest()
    }

    fun testWarnUntypedFieldsDisable() {
        doTest()
    }

    fun testWarnUntypedFields() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.warnUntypedFields = true
        doTest()
    }

    fun testCustomRoot() {
        doTest()
    }
    fun testAnnotated() {
        doTest()
    }
}
