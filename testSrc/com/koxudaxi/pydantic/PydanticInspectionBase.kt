package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import kotlin.reflect.KClass


abstract class PydanticInspectionBase(version: String = "v1") : PydanticTestCase(version) {

    @Suppress("UNCHECKED_CAST")
    protected open val inspectionClass: KClass<PyInspection> = PydanticInspection::class as KClass<PyInspection>

    @Suppress("UNCHECKED_CAST")
    protected open val typeInspectionClass: KClass<PyInspection> =
        PydanticTypeCheckerInspection::class as KClass<PyInspection>

    private fun configureInspection(inspection: KClass<PyInspection> = inspectionClass) {
        myFixture!!.enableInspections(inspection.java)
        myFixture!!.checkHighlighting(true, false, true)

    }

    protected fun doTest(inspection: KClass<PyInspection> = inspectionClass) {
        configureByFile()
        configureInspection(inspection)
    }

    protected fun doTypeInspectionTest() {
        doTest(typeInspectionClass)
    }
}
