package com.koxudaxi.pydantic

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.sdk.PythonSdkUtil

class PydanticPackageManagerListener : PyPackageManager.Listener {
    override fun packagesRefreshed(sdk: Sdk) {
        ApplicationManager.getApplication().invokeLater {
            PythonSdkUtil.findSkeletonsDir(sdk)?.let { skeletons ->
                skeletons.findChild("pydantic")?.let { pydanticStub ->
                    runWriteAction {
                        try {
                            pydanticStub.delete(null)
                        } catch (e: java.io.IOException) {
                        }
                    }
                }
            }
        }
    }
}