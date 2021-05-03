package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import kotlin.reflect.KClass


open class PydanticInspectionV18Test : PydanticInspectionBase(version = "v18") {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PydanticInspection::class as KClass<PyInspection>

    fun testConfigDuplicate() {
        doTest()
    }

    fun testKwargConfig() {
        doTest()
    }

    fun testReadOnlyProperty() {
        doTest()
    }

    fun testReadOnlyPropertyFrozen() {
        doTest()
    }
}
