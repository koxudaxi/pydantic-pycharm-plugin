package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import kotlin.reflect.KClass


open class PydanticTypeCheckerInspectionTest : PydanticInspectionBase() {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PydanticTypeCheckerInspection::class as KClass<PyInspection>

    fun testParsableType() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.parsableTypeMap["builtins.str"] = arrayListOf("builtins.int")
        doTest()
    }
    fun testParsableTypeInvalid() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.parsableTypeMap["builtins.str"] = arrayListOf("builtins.int")
        doTest()
    }
}
