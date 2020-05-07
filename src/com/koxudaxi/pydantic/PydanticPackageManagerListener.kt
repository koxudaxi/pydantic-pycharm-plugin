package com.koxudaxi.pydantic

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.vfs.VirtualFile
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.sdk.PythonSdkUtil

class PydanticPackageListener : PyPackageManager.Listener {
    override fun packagesRefreshed(sdk: Sdk) {
        ApplicationManager.getApplication().invokeLater {
            PythonSdkUtil.findSkeletonsDir(sdk)?.let { skeletons ->
                val pydanticStub = skeletons.findChild("pydantic")
                if (pydanticStub is VirtualFile) {
                    runWriteAction {
                        pydanticStub.delete(null)
                    }
                }
            }
        }
    }
}