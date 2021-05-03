package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import kotlin.reflect.KClass


abstract class PydanticInspectionBase(version: String = "v1") : PydanticTestCase(version) {

    @Suppress("UNCHECKED_CAST")
    protected open val inspectionClass: KClass<PyInspection> = PydanticInspection::class as KClass<PyInspection>

    private fun configureInspection() {
        myFixture!!.enableInspections(inspectionClass.java)
        myFixture!!.checkHighlighting(true, false, true)

    }

    protected fun doTest() {
        configureByFile()
        configureInspection()
    }
}
