package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.testFramework.writeChild
import com.jetbrains.python.sdk.PythonSdkUtil
import org.jetbrains.kotlin.konan.file.File


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
        val target = createTempFile(testMethodName)
        try {
            val source = File("${myFixture!!.testDataPath}/${testDataMethodPath.toLowerCase()}/pyproject.toml")
            target.writeText(source.bufferedReader().readText())
            pydanticConfigService.pyprojectToml = target.path
            runnable()
        } finally {
            target.deleteOnExit()
        }
    }

    private fun setUpMypyIni(runnable: () -> Unit) {
        setUpConfig()
        val target = createTempFile(testMethodName)
        try {
            val source = File("${myFixture!!.testDataPath}/${testDataMethodPath.toLowerCase()}/mypy.ini")
            target.writeText(source.bufferedReader().readText())
            pydanticConfigService.mypyIni = target.path
            runnable()
        } finally {
            target.deleteOnExit()
        }
    }

    fun testPyProjectToml() {
        setUpPyProjectToml {
            initializeFileLoader()
            ApplicationManager.getApplication().invokeLater {
                assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf(
                        "pydantic.HttpUrl" to listOf("str"),
                        "datetime.datetime" to listOf("int")
                ))
                assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf("str" to listOf("int", "float")))
                assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
                assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WARNING)
            }
        }
    }

    fun testPyProjectTomlChange() {
        initializeFileLoader()
        setUpPyProjectToml {
            invokeLater {
                assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf(
                        "pydantic.HttpUrl" to listOf("str"),
                        "datetime.datetime" to listOf("int")
                ))
                assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf("str" to listOf("int", "float")))
                assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
                assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WARNING)
            }
        }
    }

    fun testPyProjectTomlDisable() {
        setUpPyProjectToml {
            initializeFileLoader()
            invokeLater {
                assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.INFORMATION)
                assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.INFORMATION)
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

    fun testMypyIniChange() {
        initializeFileLoader()
        setUpMypyIni {
            invokeLater {
                assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, true)
                assertEquals(this.pydanticConfigService.mypyInitTyped, false)
                assertEquals(this.pydanticConfigService.currentWarnUntypedFields, true)
                assertEquals(this.pydanticConfigService.currentInitTyped, false)
            }
        }
    }

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

    fun testCopyStubFile() {
        val sdk = PythonSdkUtil.findPythonSdk(myFixture!!.module)!!
        val sitePackage = PythonSdkUtil.getSitePackagesDirectory(sdk)!!
        runWriteAction {
            val pydanticStubDir = sitePackage.findChild("pydantic")
                    ?: sitePackage.createChildDirectory(null, "pydantic")
            pydanticStubDir.writeChild("main.py", "test")
        }
        PydanticInitializer().initializeFileLoader(myFixture!!.project)
        invokeLater {
            val skeleton = PythonSdkUtil.findSkeletonsDir(sdk)!!
            skeleton.findFileByRelativePath("pydantic/main.pyi")!!.inputStream.bufferedReader().use {
                assertEquals(it.readText(), "test")
            }
        }
    }

    fun testCopyStubFileAfter() {
        PydanticInitializer().initializeFileLoader(myFixture!!.project)
        val sdk = PythonSdkUtil.findPythonSdk(myFixture!!.module)!!
        val sitePackage = PythonSdkUtil.getSitePackagesDirectory(sdk)!!
        runWriteAction {
            val pydanticStubDir = sitePackage.findChild("pydantic")
                    ?: sitePackage.createChildDirectory(null, "pydantic")
            pydanticStubDir.writeChild("main.py", "test")
        }
        invokeLater {
            val skeleton = PythonSdkUtil.findSkeletonsDir(sdk)!!
            skeleton.findFileByRelativePath("pydantic/main.pyi")!!.inputStream.bufferedReader().use {
                assertEquals(it.readText(), "test")
            }
        }
    }
}
