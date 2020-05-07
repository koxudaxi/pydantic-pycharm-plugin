package com.koxudaxi.pydantic

import com.intellij.openapi.application.invokeLater
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.progress.util.BackgroundTaskUtil
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.sdk.PythonSdkUtil
import junit.framework.Assert


open class PydanticPackageManagerListenerTest : PydanticTestCase() {
    fun testDeleteStubFile() {
        val sdk = PythonSdkUtil.findPythonSdk(myFixture!!.module)!!
        val skeleton = PythonSdkUtil.findSkeletonsDir(sdk)!!
        var pydanticStubDir: VirtualFile? = null
        runWriteAction {
            pydanticStubDir = skeleton.createChildDirectory(null, "pydantic")
            Assert.assertTrue(pydanticStubDir!!.exists())
        }
        BackgroundTaskUtil.syncPublisher(myFixture!!.project, PyPackageManager.PACKAGE_MANAGER_TOPIC).packagesRefreshed(sdk)
        invokeLater {
            Assert.assertFalse(pydanticStubDir!!.exists())
        }
    }
}
