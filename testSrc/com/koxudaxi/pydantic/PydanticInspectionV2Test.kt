package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import kotlin.reflect.KClass


open class PydanticInspectionV2Test : PydanticInspectionBase(version = "v2") {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PydanticInspection::class as KClass<PyInspection>

    fun testCustomRoot() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.mypyWarnUntypedFields = false
        doTest()
    }
}
