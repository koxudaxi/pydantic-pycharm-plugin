package com.koxudaxi.pydantic

import com.intellij.codeInspection.ex.InspectionProfileImpl
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.PsiTestUtil.addSourceRoot
import com.intellij.testFramework.PsiTestUtil.removeSourceRoot
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.PyNames
import com.jetbrains.python.codeInsight.completion.PyModuleNameCompletionContributor
import com.jetbrains.python.fixtures.PyLightProjectDescriptor
import com.jetbrains.python.namespacePackages.PyNamespacePackagesService
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.impl.PythonLanguageLevelPusher
import com.jetbrains.python.sdk.PythonSdkUtil
import kotlin.test.Test

abstract class PydanticTestCase(val version: String = "v1") : UsefulTestCase() {

    protected var myFixture: CodeInsightTestFixture? = null

    private val testDataPath: String = "testData"

    /**
     * Common list of Python built-in names to exclude from completion tests.
     * These are standard Python builtins that appear in completion results
     * but are not relevant to Pydantic-specific testing.
     */
    companion object {
        protected val ourPyLatestDescriptor = PyLightProjectDescriptor(LanguageLevel.getLatest())

        /**
         * Base excludes for all completion tests - Python dunder attributes and builtins
         */
        val BASE_COMPLETION_EXCLUDES = listOf(
            // Dunder attributes
            "__annotations__",
            "__base__",
            "__bases__",
            "__basicsize__",
            "__dict__",
            "__dictoffset__",
            "__flags__",
            "__itemsize__",
            "__mro__",
            "__name__",
            "__qualname__",
            "__slots__",
            "__text_signature__",
            "__weakrefoffset__",
            "__type_params__",
            // Python builtins
            "Ellipsis",
            "EnvironmentError",
            "IOError",
            "NotImplemented",
            "WindowsError",
            // Python 2025.3+ interactive shell builtins
            "copyright",
            "credits",
            "exit",
            "help",
            "license",
            "quit",
            // Typing module
            "List",
            "Type",
            "Annotated",
            // Dataclasses
            "MISSING",
        )
    }
    private val mockPath: String = "mock"
    private val pydanticMockPath: String = "$mockPath/pydantic$version"
    private val pythonStubPath: String = "$mockPath/stub"

    private var packageDir: VirtualFile? = null
    private val defaultPythonLanguageLevel = LanguageLevel.PYTHON37

    protected val testClassName: String
        get() {
            return this.javaClass.simpleName.replace("Pydantic", "").replace("Test", "").lowercase()
        }

    protected val testDataMethodPath: String
        get() {
            return "$testClassName/${getTestName(true)}"
        }

    protected fun configureByFile(additionalFileNames: List<String>? = null) {
        configureByFileName("${testDataMethodPath}.py")

        additionalFileNames?.forEach {
            configureByFileName("${testClassName}/${it}.py")
        }
    }

    private fun configureByFileName(fileName: String) {
        myFixture!!.configureByFile(fileName)
    }

    @Throws(Exception::class)
    override fun setUp() {
        super.setUp()
        val factory = IdeaTestFixtureFactory.getFixtureFactory()
        val fixtureBuilder = factory.createLightFixtureBuilder(getProjectDescriptor(), getTestName(false))
        val fixture = fixtureBuilder.fixture
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(
            fixture,
            LightTempDirTestFixtureImpl(true)
        )
        myFixture!!.testDataPath = testDataPath

        myFixture!!.setUp()
        myFixture!!.tempDirFixture.getFile("package")?.let { it.delete(this)}

        myFixture!!.copyDirectoryToProject(pythonStubPath, "package")
        myFixture!!.copyDirectoryToProject(pydanticMockPath, "package/pydantic")

        packageDir = myFixture!!.findFileInTempDir("package")


        addSourceRoot(myFixture!!.module, packageDir!!)

        runWriteAction {
            val sdk = PythonSdkUtil.findPythonSdk(myFixture!!.module)!!
            myFixture!!.tempDirFixture.findOrCreateDir(PythonSdkUtil.SKELETON_DIR_NAME)
                .also { sdk.sdkModificator.addRoot(it, OrderRootType.CLASSES) }
            val libDir = myFixture!!.tempDirFixture.findOrCreateDir("Lib")
                .also { sdk.sdkModificator.addRoot(it, OrderRootType.CLASSES) }
            libDir.createChildDirectory(null, PyNames.SITE_PACKAGES)
        }
        val parsedVersion = version.split("v")[1].let {
            when (it) {
                "18" -> "1.8"
                else -> it
            }
        }
        PydanticCacheService.setVersion(myFixture!!.project, parsedVersion)
        setLanguageLevel(defaultPythonLanguageLevel)
        InspectionProfileImpl.INIT_INSPECTIONS = true
        
        // Wait for indexing to complete after setting up the project
        IndexingTestUtil.waitUntilIndexesAreReady(myFixture!!.project)
        DumbService.getInstance(myFixture!!.project).waitForSmartMode()
    }



    @Throws(Exception::class)
    override fun tearDown() {
        try {
            myFixture?.let { fixture ->
                fixture.module?.let { module ->
                    PyNamespacePackagesService.getInstance(module).resetAllNamespacePackages()
                    packageDir?.let { dir ->
                        removeSourceRoot(module, dir)
                    }
                }
                PyModuleNameCompletionContributor.ENABLED = true
                setLanguageLevel(null)
                fixture.tearDown()
                myFixture = null
            }
            FilePropertyPusher.EP_NAME.findExtensionOrFail(PythonLanguageLevelPusher::class.java)
                .flushLanguageLevelCache()
        } catch (e: Throwable) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
            clearFields(this)
        }
    }

    protected fun setLanguageLevel(languageLevel: LanguageLevel?) {
        PythonLanguageLevelPusher.setForcedLanguageLevel(myFixture!!.project, languageLevel)
    }

    @Test
    private fun dummyTest() {
    }

    protected open fun getProjectDescriptor(): LightProjectDescriptor? {
        return ourPyLatestDescriptor
    }
}

