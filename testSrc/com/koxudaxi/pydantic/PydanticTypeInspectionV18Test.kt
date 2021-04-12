package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyTypeCheckerInspection
import kotlin.reflect.KClass


open class PydanticTypeInspectionV18Test : PydanticInspectionBase("v18") {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PyTypeCheckerInspection::class as KClass<PyInspection>
    fun testDynamicModel() {
        doTest()
    }
}