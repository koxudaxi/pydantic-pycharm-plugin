package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyTypeCheckerInspection
import kotlin.reflect.KClass


open class PydanticTypeInspectionTest : PydanticInspectionBase() {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PyTypeCheckerInspection::class as KClass<PyInspection>

    fun testKeywordArgument() {
        doTest()
    }

    fun testKeywordArgumentUnion() {
        doTest()
    }

    fun testKeywordArgumentOptional() {
        doTest()
    }

    fun testKeywordArgumentInvalid() {
        doTest()
    }

    fun testKeywordArgumentUnionInvalid() {
        doTest()
    }

    fun testKeywordArgumentOptionalInvalid() {
        doTest()
    }
}
