package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.testFramework.PsiTestUtil

open class PydanticInitializerTest : PydanticTestCase() {
    lateinit var pydanticConfigService: PydanticConfigService
    lateinit var testMethodName: String
    private fun setUpConfig() {
        this.pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        this.testMethodName = getTestName(true)
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
            assertEquals(this.pydanticConfigService.parsableTypeHighlightType, ProblemHighlightType.WARNING)
            assertEquals(this.pydanticConfigService.acceptableTypeHighlightType, ProblemHighlightType.INFORMATION)
        }
    }

    fun testpyprojecttomlempty() {
        setUpPyProjectToml {
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
            assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
            assertEquals(this.pydanticConfigService.mypyInitTyped, null)
            assertEquals(this.pydanticConfigService.currentInitTyped, true)
            assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
        }
    }

    fun testnothingmypyini() {
        setUpConfig()
        assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
        assertEquals(this.pydanticConfigService.mypyInitTyped, null)
        assertEquals(this.pydanticConfigService.currentInitTyped, true)
        assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
    }
}
