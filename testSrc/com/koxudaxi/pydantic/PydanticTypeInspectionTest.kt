package com.koxudaxi.pydantic

import com.jetbrains.python.inspections.PyInspection
import com.jetbrains.python.inspections.PyTypeCheckerInspection
import com.jetbrains.python.psi.LanguageLevel
import kotlin.reflect.KClass


open class PydanticTypeInspectionTest : PydanticInspectionBase() {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PyTypeCheckerInspection::class as KClass<PyInspection>

    fun testField() {
        doTest()
    }

    fun testFieldDefaultValue() {
        doTest()
    }

    fun testFieldUnion() {
        doTest()
    }

    fun testFieldOptional() {
        doTest()
    }

    fun testFieldOptionalDefaultValue() {
        doTest()
    }

    // TODO: Type warnings not generated in PyCharm 2025.2
    fun _disabled_testFieldInvalid() {
        doTest()
    }

    // TODO: Type inference without type annotation not working in PyCharm 2025.2
    fun _disabled_testFieldDefaultValueInvalid() {
        doTest()
    }

    // TODO: Union type warnings not generated in PyCharm 2025.2
    fun _disabled_testFieldUnionInvalid() {
        doTest()
    }

    // TODO: Union operator type warnings not generated in PyCharm 2025.2
    fun _disabled_testFieldUnionOperatorInvalid() {
        setLanguageLevel(LanguageLevel.PYTHON310)
        doTest()
    }

    // TODO: Optional type warnings not generated in PyCharm 2025.2
    fun _disabled_testFieldOptionalInvalid() {
        doTest()
    }

    // TODO: Type inference without type annotation not working in PyCharm 2025.2
    fun _disabled_testFieldOptionalDefaultValueInvalid() {
        doTest()
    }

    fun testFieldEllipsis() {
        doTest()
    }

    fun testFieldSchema() {
        doTest()
    }

    // TODO: Schema field type warnings not generated in PyCharm 2025.2
    fun _disabled_testFieldSchemaInvalid() {
        doTest()
    }

    // TODO: Field() declaration handling broken in PyCharm 2025.2
    fun _disabled_testFieldField() {
        doTest()
    }

    // TODO: Field() type warnings not generated in PyCharm 2025.2
    fun _disabled_testFieldFieldInvalid() {
        doTest()
    }

    fun testFieldBroken() {
        doTest()
    }

    fun testBaseSetting() {
        doTest()
    }

    fun testInitAncestor() {
        doTest()
    }

    // TODO: Ancestor field type warnings not generated in PyCharm 2025.2
    fun _disabled_testInitAncestorInvalid() {
        doTest()
    }

    fun testInit() {
        doTest()
    }

    // TODO: Init type warnings not generated in PyCharm 2025.2
    fun _disabled_testInitInvalid() {
        doTest()
    }

    fun testFieldInherit() {
        doTest()
    }

    fun testIgnoreInitArguments() {
        PydanticConfigService.getInstance(myFixture!!.project).ignoreInitMethodArguments = true
        doTest()
    }

    fun testUnResolve() {
        doTest()
    }

    fun testDuplicateField() {
        doTest()
    }

    fun testSkipMember() {
        doTest()
    }

    // TODO: Dynamic model type checking broken in PyCharm 2025.2
    fun _disabled_testDynamicModel() {
        doTest()
    }

    fun testAllowPopulationByFieldNameAlias() {
        doTest()
    }
}

