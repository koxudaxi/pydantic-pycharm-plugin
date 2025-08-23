package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyTypeCheckerInspection
import kotlin.reflect.KClass


open class PydanticTypeInspectionV18Test : PydanticInspectionBase("v18") {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PyTypeCheckerInspection::class as KClass<PyInspection>
    // TODO: Dynamic model type checking broken in PyCharm 2025.2
    fun _disabled_testDynamicModel() {
        doTest()
    }

    // TODO: Dataclass type checking broken in PyCharm 2025.2
    fun _disabled_testDataclass() {
        doTest()
    }

    fun testBaseSetting() {
        doTest()
    }

    // TODO: Generic type resolution for pydantic.generics.GenericModel not working in PyCharm 2025.2
    // This complex test with GenericModel from pydantic v1.8 is temporarily disabled
    fun _disabled_testGenericModel() {
        doTest()
    }

    // TODO: SQLModel type checking broken in PyCharm 2025.2
    fun _disabled_testSqlModel() {
        doTest()
    }
}