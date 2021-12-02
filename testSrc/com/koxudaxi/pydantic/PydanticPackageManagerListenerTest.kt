package com.koxudaxi.pydantic

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.sdk.PythonSdkUtil


open class PydanticPackageManagerListenerTest : PydanticTestCase() {
    fun testDeleteStubFile() {
        val sdk = PythonSdkUtil.findPythonSdk(myFixture!!.module)!!
        val skeleton = PythonSdkUtil.findSkeletonsDir(sdk)!!
        var pydanticStubDir: VirtualFile? = null
        runWriteAction {
            pydanticStubDir = skeleton.createChildDirectory(null, "pydantic")
            assertTrue(pydanticStubDir!!.exists())
        }
        PydanticPackageManagerListener().packagesRefreshed(sdk)
        invokeLater {
            assertFalse(pydanticStubDir!!.exists())
        }
    }

    fun testClearVersion() {
        val project = myFixture!!.project
        val context = TypeEvalContext.userInitiated(project, null)
        val sdk = PythonSdkUtil.findPythonSdk(myFixture!!.module)!!

        val pydanticVersion = PydanticCacheService.getVersion(project, context)
        assertEquals(KotlinVersion(1, 0, 1), pydanticVersion)

        PydanticPackageManagerListener().packagesRefreshed(sdk)

        invokeLater {
            val privateVersionField = PydanticCacheService::class.java.getDeclaredField("version")
            privateVersionField.trySetAccessible()
            val pydanticVersionService = project.getService(PydanticCacheService::class.java)
            val actual = privateVersionField.get(pydanticVersionService)
            assertNull(actual)
        }
    }
}
