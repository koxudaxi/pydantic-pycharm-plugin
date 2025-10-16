package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import java.io.File
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.createTempFile

@ExperimentalPathApi
open class PydanticInitializerTest : PydanticTestCase() {
    lateinit var pydanticConfigService: PydanticConfigService
    lateinit var testMethodName: String
    private fun setUpConfig() {
        this.pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        this.testMethodName = getTestName(true)
    }

    private fun initializeFileLoader() {
        PydanticInitializer().initializeFileLoader(myFixture!!.project)
    }

    private fun setUpPyProjectToml(runnable: () -> Unit)  = runTestRunnable {
        setUpConfig()
        val target = createTempFile(testMethodName).toFile()
        try {
            val source = File("${myFixture!!.testDataPath}/${testDataMethodPath.lowercase()}/pyproject.toml")
            pydanticConfigService.pyprojectToml = target.path
            target.writeText(source.bufferedReader().readText())
            runnable()

        } finally {
            target.deleteOnExit()
        }
    }

    private fun setUpMypyIni(runnable: () -> Unit) {
        setUpConfig()
        val target = createTempFile(testMethodName).toFile()
        try {
            val source = File("${myFixture!!.testDataPath}/${testDataMethodPath.lowercase()}/mypy.ini")
            pydanticConfigService.mypyIni = target.path
            target.writeText(source.bufferedReader().readText())
            runnable()
        } finally {
            target.deleteOnExit()
        }
    }

    fun testPyProjectToml() = runTestRunnable {
        setUpPyProjectToml {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf(
                "pydantic.networks.HttpUrl" to listOf("str"),
                "datetime.datetime" to listOf("int")
            ))
            assertEquals(this.pydanticConfigService.acceptableTypeMap,
                mutableMapOf("str" to listOf("int", "float")))
            assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
            assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WARNING)
            assertEquals(this.pydanticConfigService.ignoreInitMethodArguments, true)
            assertEquals(this.pydanticConfigService.ignoreInitMethodKeywordArguments, false)
        }
    }

//    fun testPyProjectTomlChange() {
//        setUpPyProjectToml {
//            initializeFileLoader()
//        }
//        invokeLater {
//            assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf(
//                    "pydantic.HttpUrl" to listOf("str"),
//                    "datetime.datetime" to listOf("int")
//            ))
//            assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf("str" to listOf("int", "float")))
//            assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
//            assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WARNING)
//        }
//    }

    fun testPyProjectTomlDisable() = runTestRunnable {
        setUpPyProjectToml {
            initializeFileLoader()
            suspend {
                assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.INFORMATION)
                assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.INFORMATION)
            }
        }
    }

    fun testPyProjectTomlDefault()  = runTestRunnable {
        setUpPyProjectToml {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
            assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
            assertEquals(this.pydanticConfigService.ignoreInitMethodArguments, false)
            assertEquals(this.pydanticConfigService.ignoreInitMethodKeywordArguments, true)
        }
    }

    fun testPyProjectTomlEmpty()  = runTestRunnable {
        setUpPyProjectToml {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf<String, List<String>>())
            assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf<String, List<String>>())
            assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
            assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
        }
    }

    fun testNothingPyProjectToml() = runTestRunnable {
        setUpConfig()
        assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf<String, List<String>>())
        assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf<String, List<String>>())
        assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
        assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
    }

    fun testMypyIni() = runTestRunnable {
        setUpMypyIni {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, true)
            assertEquals(this.pydanticConfigService.mypyInitTyped, false)
            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, true)
            assertEquals(this.pydanticConfigService.currentInitTyped, false)
        }
    }

//    fun testMypyIniChange() {
//        setUpMypyIni {
//            initializeFileLoader()
//        }
//        invokeLater {
//            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, true)
//            assertEquals(this.pydanticConfigService.mypyInitTyped, false)
//            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, true)
//            assertEquals(this.pydanticConfigService.currentInitTyped, false)
//        }
//    }

    fun testMypyIniEmpty() = runTestRunnable {
        setUpMypyIni {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
            assertEquals(this.pydanticConfigService.mypyInitTyped, null)
            assertEquals(this.pydanticConfigService.currentInitTyped, true)
            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
        }
    }

    fun testMypyIniBroken() = runTestRunnable {
        setUpMypyIni {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
            assertEquals(this.pydanticConfigService.mypyInitTyped, null)
            assertEquals(this.pydanticConfigService.currentInitTyped, true)
            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
        }
    }


    fun testNothingMypyIni() = runTestRunnable {
        setUpConfig()
        initializeFileLoader()
        assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
        assertEquals(this.pydanticConfigService.mypyInitTyped, null)
        assertEquals(this.pydanticConfigService.currentInitTyped, true)
        assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
    }
}
