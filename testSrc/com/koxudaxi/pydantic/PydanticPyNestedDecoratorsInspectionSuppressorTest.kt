package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyNestedDecoratorsInspection
import kotlin.reflect.KClass


open class PydanticPyNestedDecoratorsInspectionSuppressorTest : PydanticInspectionBase(version = "v2") {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PyNestedDecoratorsInspection::class as KClass<PyInspection>

    fun testModelValidator() {
        doTest()
    }
}
