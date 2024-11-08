package com.koxudaxi.pydantic

import com.intellij.openapi.application.invokeLater
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.sdk.PythonSdkUtil


open class PydanticPythonPackageManagementListenerTest : PydanticTestCase() {
//    fun testDeleteStubFile() {
//        val sdk = PythonSdkUtil.findPythonSdk(myFixture!!.module)!!
//        val skeleton = PythonSdkUtil.findSkeletonsDir(sdk)!!
//        var pydanticStubDir: VirtualFile? = null
//        runWriteAction {
//            pydanticStubDir = skeleton.createChildDirectory(null, "pydantic")
//            assertTrue(pydanticStubDir!!.exists())
//        }
//        PydanticPackageManagerListener().packagesRefreshed(sdk)
//        invokeLater {
//            assertFalse(pydanticStubDir!!.exists())
//        }
//    }

    fun testClearVersion() {
        val project = myFixture!!.project
        val context = TypeEvalContext.userInitiated(project, null)
        val sdk = PythonSdkUtil.findPythonSdk(myFixture!!.module)!!

        PydanticCacheService.setVersion(project, "1.0.1")
        val pydanticVersion = PydanticCacheService.getVersion(project)
        assertEquals(KotlinVersion(1, 0, 1), pydanticVersion)

        PydanticPythonPackageManagementListener().packagesChanged(sdk)

        invokeLater {
            val privateVersionField = PydanticCacheService::class.java.getDeclaredField("version")
            privateVersionField.trySetAccessible()
            val pydanticVersionService = project.getService(PydanticCacheService::class.java)
            val actual = privateVersionField.get(pydanticVersionService)
            assertNull(actual)
        }
    }
}
