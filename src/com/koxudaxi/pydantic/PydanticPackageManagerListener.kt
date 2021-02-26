package com.koxudaxi.pydantic

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Disposer
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.sdk.PythonSdkUtil

class PydanticPackageManagerListener : PyPackageManager.Listener {
    override fun packagesRefreshed(sdk: Sdk) {
        ApplicationManager.getApplication().invokeLater {
            if (sdk is Disposable && Disposer.isDisposed(sdk)) {
                return@invokeLater
            }
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