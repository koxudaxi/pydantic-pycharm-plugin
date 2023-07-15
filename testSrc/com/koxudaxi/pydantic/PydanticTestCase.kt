package com.koxudaxi.pydantic

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.FilePropertyPusher
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.PsiTestUtil.addSourceRoot
import com.intellij.testFramework.PsiTestUtil.removeSourceRoot
import com.intellij.testFramework.UsefulTestCase
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import com.intellij.testFramework.fixtures.IdeaTestFixtureFactory
import com.intellij.testFramework.fixtures.impl.LightTempDirTestFixtureImpl
import com.jetbrains.python.PyNames
import com.jetbrains.python.fixtures.PyLightProjectDescriptor
import com.jetbrains.python.psi.LanguageLevel
import com.jetbrains.python.psi.impl.PythonLanguageLevelPusher
import com.jetbrains.python.sdk.PythonSdkUtil
import kotlin.test.Test

abstract class PydanticTestCase(val version: String = "v1") : UsefulTestCase() {

    protected var myFixture: CodeInsightTestFixture? = null

    private val projectDescriptor: PyLightProjectDescriptor = PyLightProjectDescriptor(LanguageLevel.getLatest())
    private val testDataPath: String = "testData"
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
        val fixtureBuilder = factory.createLightFixtureBuilder(projectDescriptor, "PyLightProject")
        val fixture = fixtureBuilder.fixture
        myFixture = IdeaTestFixtureFactory.getFixtureFactory().createCodeInsightFixture(fixture,
            LightTempDirTestFixtureImpl(true))
        myFixture!!.testDataPath = testDataPath

        myFixture!!.setUp()
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
    }


    @Throws(Exception::class)
    override fun tearDown() {
        try {
            setLanguageLevel(null)
            removeSourceRoot(myFixture!!.module, packageDir!!)
            myFixture?.tearDown()
            myFixture = null
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
}

