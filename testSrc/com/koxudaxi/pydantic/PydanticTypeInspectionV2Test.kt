package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyTypeCheckerInspection
import kotlin.reflect.KClass


open class PydanticTypeInspectionV2Test : PydanticInspectionBase("v2") {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PyTypeCheckerInspection::class as KClass<PyInspection>

    fun testPopulateByNameAlias() {
        doTest()
    }

    fun testPopulateByNameAliasEdge() {
        doTest()
    }
}
