package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.writeChild
import com.jetbrains.python.sdk.PythonSdkUtil


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
        ApplicationManager.getApplication().executeOnPooledThread {
            setUpConfig()
            pydanticConfigService.pyprojectToml = "/src/${testMethodName}"
            val pyProjectToml = myFixture!!.copyFileToProject("${testDataMethodPath}/pyproject.toml", testMethodName)
            try {
                runnable()
            } finally {
                PsiTestUtil.removeSourceRoot(myFixture!!.module, pyProjectToml)
            }
        }
    }

    private fun setUpMypyIni(runnable: () -> Unit) {
        ApplicationManager.getApplication().executeOnPooledThread {
            setUpConfig()
            pydanticConfigService.mypyIni = "/src/${testMethodName}"
            val mypyIni = myFixture!!.copyFileToProject("${testDataMethodPath}/mypy.ini", testMethodName)
            try {
                runnable()
            } finally {
                PsiTestUtil.removeSourceRoot(myFixture!!.module, mypyIni)
            }
        }
    }

    fun testpyprojecttoml() {
        setUpPyProjectToml {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf(
                    "datetime.datetime" to listOf("int"),
                    "pydantic.networks.HttpUrl" to listOf("str")
            ))
            assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf("str" to listOf("int", "float")))
            assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
            assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WARNING)
        }
    }

    fun testpyprojecttomlchange() {
        initializeFileLoader()
        setUpPyProjectToml {

            assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf(
                    "datetime.datetime" to listOf("int"),
                    "pydantic.networks.HttpUrl" to listOf("str")
            ))
            assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf("str" to listOf("int", "float")))
            assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
            assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WARNING)
        }
    }

    fun testpyprojecttomldisable() {
        setUpPyProjectToml {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
            assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.INFORMATION)
        }
    }

    fun testpyprojecttomlempty() {
        setUpPyProjectToml {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf<String, List<String>>())
            assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf<String, List<String>>())
            assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
            assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
        }
    }

    fun testnothingpyprojecttoml() {
        setUpConfig()
        assertEquals(this.pydanticConfigService.parsableTypeMap, mutableMapOf<String, List<String>>())
        assertEquals(this.pydanticConfigService.acceptableTypeMap, mutableMapOf<String, List<String>>())
        assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
        assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.WEAK_WARNING)
    }

    fun testmypyini() {
        setUpMypyIni {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, true)
            assertEquals(this.pydanticConfigService.mypyInitTyped, false)
            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, true)
            assertEquals(this.pydanticConfigService.currentInitTyped, false)
        }
    }

    fun testmypyinichange() {
        initializeFileLoader()
        setUpMypyIni {
            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, true)
            assertEquals(this.pydanticConfigService.mypyInitTyped, false)
            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, true)
            assertEquals(this.pydanticConfigService.currentInitTyped, false)
        }
    }

    fun testmypyiniempty() {
        setUpMypyIni {
            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
            assertEquals(this.pydanticConfigService.mypyInitTyped, null)
            assertEquals(this.pydanticConfigService.currentInitTyped, true)
            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
        }
    }

    fun testmypyinibroken() {
        setUpMypyIni {
            initializeFileLoader()
            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
            assertEquals(this.pydanticConfigService.mypyInitTyped, null)
            assertEquals(this.pydanticConfigService.currentInitTyped, true)
            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
        }
    }

    fun testnothingmypyini() {
        setUpConfig()
        initializeFileLoader()
        assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
        assertEquals(this.pydanticConfigService.mypyInitTyped, null)
        assertEquals(this.pydanticConfigService.currentInitTyped, true)
        assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
    }

    fun testcopystubfile() {
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

    fun testcopystubfileafter() {
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
