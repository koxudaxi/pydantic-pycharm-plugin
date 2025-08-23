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

    // TODO: Positional argument warning not working properly in PyCharm 2025.2
    fun _disabled_testAcceptsOnlyKeywordArgumentsInit() {
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
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.mypyWarnUntypedFields = false
        doTest()
    }

    fun testAnnotated() {
        doTest()
    }

    fun testConfigDuplicate() {
        doTest()
    }

    fun testKwargConfig() {
        doTest()
    }

    fun testExtra() {
        doTest()
    }

    fun testCallTypeWithSelf() {
        doTest()
    }
}
