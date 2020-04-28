package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.vfs.newvfs.impl.VirtualFileSystemEntry
import com.intellij.testFramework.PsiTestUtil
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.testFramework.waitForProjectLeakingThreads
import java.util.concurrent.TimeUnit

open class PydanticInitializerTest : PydanticTestCase() {
    lateinit var pydanticConfigService: PydanticConfigService
    lateinit var testMethodName: String
    override fun setUp() {
        super.setUp()
        pydanticConfigService = PydanticConfigService.getInstance(myFixture!!.project)
        testMethodName = getTestName(true)
    }

    private fun setUpPyProjectToml(runnable: () -> Unit) {
        pydanticConfigService.pyprojectToml = "/src/${testMethodName}"
        val pyProjectToml = myFixture!!.copyFileToProject("${testDataMethodPath}/pyproject.toml", testMethodName)
        try {
            runnable()
        } finally {
            PsiTestUtil.removeSourceRoot(myFixture!!.module, pyProjectToml)
        }
    }

    private fun setUpMypyIni(runnable: () -> Unit) {
        pydanticConfigService.mypyIni = "/src/${testMethodName}"
        val mypyIni = myFixture!!.copyFileToProject("${testDataMethodPath}/mypy.ini", testMethodName)
        try {
            runnable()
        } finally {
            PsiTestUtil.removeSourceRoot(myFixture!!.module, mypyIni)
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
        assertEquals(this.pydanticConfigService.mypyWarnUntypedFields, null)
        assertEquals(this.pydanticConfigService.mypyInitTyped, null)
        assertEquals(this.pydanticConfigService.currentInitTyped, true)
        assertEquals(this.pydanticConfigService.currentWarnUntypedFields, false)
    }
}
