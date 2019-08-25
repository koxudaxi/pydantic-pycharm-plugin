package com.koxudaxi.pydantic

import com.jetbrains.python.codeInsight.typing.PyTypingInspectionExtension
import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyTypeCheckerInspection
import com.jetbrains.python.inspections.PyTypeHintsInspection
import kotlin.reflect.KClass


abstract class PydanticInspectionBase : PydanticTestCase() {

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
