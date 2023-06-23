package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.jetbrains.python.inspections.PyInspection
import kotlin.reflect.KClass


open class PydanticTypeCheckerInspectionTest : PydanticInspectionBase() {

    @Suppress("UNCHECKED_CAST")
    override val inspectionClass: KClass<PyInspection> = PydanticTypeCheckerInspection::class as KClass<PyInspection>

    fun testParsableType() {
        val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        pydanticConfigService.parsableTypeMap = mapOf(Pair("str", listOf("int")))
        doTest()
    }

    fun testParsableTypeCollection() = runTestRunnable {
        suspend {
            val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
            pydanticConfigService.parsableTypeMap = mapOf(Pair("str", listOf("int")))
            doTest()
        }
    }

    fun testParsableTypeWeakWarning() = runTestRunnable {
        suspend {
            val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
            pydanticConfigService.parsableTypeMap = mapOf(Pair("str", listOf("int")))
            pydanticConfigService.parsableTypeHighlightType = ProblemHighlightType.WEAK_WARNING
            doTest()
        }
    }

    fun testParsableTypeDisable() = runTestRunnable {
        suspend {
            val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
            pydanticConfigService.parsableTypeMap = mapOf(Pair("str", listOf("int")))
            pydanticConfigService.parsableTypeHighlightType = ProblemHighlightType.INFORMATION
            doTest()
        }
    }

    fun testParsableTypeInvalid() = runTestRunnable {
        suspend {
            val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
            pydanticConfigService.parsableTypeMap = mapOf(Pair("str", listOf("int")))
            doTest()
        }
    }



    fun testAcceptableType()  = runTestRunnable {
        suspend {
            val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
            pydanticConfigService.acceptableTypeMap = mapOf(Pair("str", listOf("int")))
            doTest()
        }
    }

    fun testAcceptableTypeWarning() = runTestRunnable {
        suspend {
            val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
            pydanticConfigService.acceptableTypeMap = mapOf(Pair("str", listOf("int")))
            pydanticConfigService.acceptableTypeHighlightType = ProblemHighlightType.WARNING
            doTest()
        }
    }

    fun testAcceptableTypeDisable()  = runTestRunnable {
        suspend {
            val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
            pydanticConfigService.acceptableTypeMap = mapOf(Pair("str", listOf("int")))
            pydanticConfigService.acceptableTypeHighlightType = ProblemHighlightType.INFORMATION
            doTest()
        }
    }

    fun testAcceptableTypeInvalid() = runTestRunnable {
        suspend {
            val pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
            pydanticConfigService.acceptableTypeMap = mapOf(Pair("str", listOf("int")))
            doTest()
        }
    }

    fun testField() = runTestRunnable {
        suspend {
            doTest()
        }
    }

    fun testClass() = runTestRunnable {
        suspend {
            doTest()
        }
    }

    fun testFieldImportTyping() = runTestRunnable {
        suspend {
            doTest()
        }
    }

    fun testFieldImportTypingInvalid() = runTestRunnable {
        suspend {
            doTest()
        }
    }
}
