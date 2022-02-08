package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.invokeLater
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

    private fun setUpPyProjectToml(runnable: () -> Unit) {
        setUpConfig()
        val target = createTempFile(testMethodName).toFile()
        try {
            val source = File("${myFixture!!.testDataPath}/${testDataMethodPath.lowercase()}/pyproject.toml")
            pydanticConfigService.pyprojectToml = target.path
            invokeLater {
                target.writeText(source.bufferedReader().readText())
            }
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

    fun testPyProjectToml() {
        setUpPyProjectToml {
            initializeFileLoader()
            invokeLater {
                assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf(
                    "pydantic.HttpUrl" to listOf("str"),
                    "datetime.datetime" to listOf("int")
                ))
                assertEquals(this.pydanticConfigService.acceptableTypeMap,
                    mutableMapOf("str" to listOf("int", "float")))
                assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
                assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WARNING)
                assertEquals(this.pydanticConfigService.ignoreInitMethodArguments, true)
            }
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

    fun testPyProjectTomlDisable() {
        setUpPyProjectToml {
            initializeFileLoader()
            invokeLater {
                assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.INFORMATION)
                assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.INFORMATION)
            }
        }
    }

    fun testPyProjectTomlDefault() {
        setUpPyProjectToml {
            initializeFileLoader()
            invokeLater {
                assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
                assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
                assertEquals(this.pydanticConfigService.ignoreInitMethodArguments, false)
           }
        }
    }

    fun testPyProjectTomlEmpty() {
        setUpPyProjectToml {
            initializeFileLoader()
            invokeLater {
                assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf<String, List<String>>())
                assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf<String, List<String>>())
                assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
                assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
            }
        }
    }

    fun testNothingPyProjectToml() {
        setUpConfig()
        assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf<String, List<String>>())
        assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf<String, List<String>>())
        assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
        assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
    }

    fun testMypyIni() {
        setUpMypyIni {
            initializeFileLoader()
            invokeLater {
                assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, true)
                assertEquals(this.pydanticConfigService.mypyInitTyped, false)
                assertEquals(this.pydanticConfigService.currentWarnUntypedFields, true)
                assertEquals(this.pydanticConfigService.currentInitTyped, false)
            }
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

    fun testMypyIniEmpty() {
        setUpMypyIni {
            initializeFileLoader()
            invokeLater {
                assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
                assertEquals(this.pydanticConfigService.mypyInitTyped, null)
                assertEquals(this.pydanticConfigService.currentInitTyped, true)
                assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
            }
        }
    }

    fun testMypyIniBroken() {
        setUpMypyIni {

            initializeFileLoader()
            invokeLater {
                assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
                assertEquals(this.pydanticConfigService.mypyInitTyped, null)
                assertEquals(this.pydanticConfigService.currentInitTyped, true)
                assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
            }
        }
    }

    fun testNothingMypyIni() {
        setUpConfig()
        initializeFileLoader()
        invokeLater {
            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
            assertEquals(this.pydanticConfigService.mypyInitTyped, null)
            assertEquals(this.pydanticConfigService.currentInitTyped, true)
            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
        }
    }
}
