package com.koxudaxi.pydantic

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.packaging.PyPackageManager
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
        BackgroundTaskUtil.syncPublisher(myFixture!!.project, PyPackageManager.PACKAGE_MANAGER_TOPIC)
            .packagesRefreshed(sdk)
        invokeLater {
            assertFalse(pydanticStubDir!!.exists())
        }
    }

    fun testClearVersion() {
        val project = myFixture!!.project
        val context = TypeEvalContext.userInitiated(project, null)
        val sdk = PythonSdkUtil.findPythonSdk(myFixture!!.module)!!

        val pydanticVersion = PydanticVersionService.getVersion(project, context)
        assertEquals(KotlinVersion(1, 0, 1), pydanticVersion)

        BackgroundTaskUtil.syncPublisher(project, PyPackageManager.PACKAGE_MANAGER_TOPIC).packagesRefreshed(sdk)
        invokeLater {
            val privateVersionField = PydanticVersionService::class.java.getDeclaredField("version")
            privateVersionField.trySetAccessible()
            val pydanticVersionService = ServiceManager.getService(project, PydanticVersionService::class.java)
            val actual = privateVersionField.get(pydanticVersionService)
            assertNull(actual)
        }
    }
}
