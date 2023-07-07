package com.koxudaxi.pydantic

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.util.Disposer
import com.jetbrains.python.packaging.PyPackageManager
import com.jetbrains.python.packaging.PyPackageManagers
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.statistics.sdks

class PydanticPackageManagerListener : PyPackageManager.Listener {
    private fun updateVersion(sdk: Sdk) {
        val version = PyPackageManagers.getInstance()
            .forSdk(sdk).packages?.find { it.name == "pydantic" }?.version
        ProjectManager.getInstance().openProjects
            .filter { it.sdks.contains(sdk) }
            .forEach {
                when (version) {
                    is String -> PydanticCacheService.setVersion(it, version)
                    else -> PydanticCacheService.clear(it)
                }
            }
    }

    override fun packagesRefreshed(sdk: Sdk) {
        ApplicationManager.getApplication().invokeLater {
            if (sdk is Disposable && Disposer.isDisposed(sdk)) {
                return@invokeLater
            }

            val skeletons = PythonSdkUtil.findSkeletonsDir(sdk)
            val pydanticStub = skeletons?.findChild("pydantic")
            if (pydanticStub == null) {
                updateVersion(sdk)
            } else {
                runWriteAction {
                    if (sdk is Disposable && Disposer.isDisposed(sdk)) {
                        return@runWriteAction
                    }
                    try {
                        pydanticStub.delete(this)
                    } catch (_: java.io.IOException) {
                    } finally {
                        pydanticStub.refresh(true, true) {
                            updateVersion(sdk)
                        }
                    }
                }
            }
        }
    }
}