package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.jetbrains.python.inspections.PyInspection
import kotlin.reflect.KClass


open class PydanticTypeCheckerInspectionTest : PydanticInspectionBase() {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PydanticTypeCheckerInspection::class as KClass<PyInspection>

    fun testParsableType() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.parsableTypeMap["builtins.str"] = arrayListOf("builtins.int")
        doTest()
    }
    fun testParsableTypeWeakWarning() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.parsableTypeMap["builtins.str"] = arrayListOf("builtins.int")
        pydanticConfigService.parsableTypeHighlightType = ProblemHighlightType.WEAK_WARNING
        doTest()
    }
    fun testParsableTypeDisable() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.parsableTypeMap["builtins.str"] = arrayListOf("builtins.int")
        pydanticConfigService.parsableTypeHighlightType = ProblemHighlightType.INFORMATION
        doTest()
    }

    fun testParsableTypeInvalid() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.parsableTypeMap["builtins.str"] = arrayListOf("int")
        doTest()
    }

    fun testAcceptableType() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.acceptableTypeMap["builtins.str"] = arrayListOf("builtins.int")
        doTest()
    }
    fun testAcceptableTypeWarning() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.acceptableTypeMap["builtins.str"] = arrayListOf("builtins.int")
        pydanticConfigService.acceptableTypeHighlightType = ProblemHighlightType.WARNING
        doTest()
    }
    fun testAcceptableTypeDisable() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.acceptableTypeMap["builtins.str"] = arrayListOf("builtins.int")
        pydanticConfigService.acceptableTypeHighlightType = ProblemHighlightType.INFORMATION
        doTest()
    }

    fun testAcceptableTypeInvalid() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.acceptableTypeMap["builtins.str"] = arrayListOf("int")
        doTest()
    }
    fun testField() {
        doTest()
    }

    fun testClass() {
        doTest()
    }
}
