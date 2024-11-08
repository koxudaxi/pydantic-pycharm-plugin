package com.koxudaxi.pydantic

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.jetbrains.python.packaging.common.PythonPackageManagementListener
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.PythonSdkUtil.isDisposed
import com.jetbrains.python.statistics.sdks

class PydanticPythonPackageManagementListener : PythonPackageManagementListener {
    private fun updateVersion(sdk: Sdk) {
        ProjectManager.getInstance().openProjects
            .filter { it.sdks.contains(sdk) }
            .forEach {
                PydanticCacheService.clear(it)
                val version = getPydanticVersion(it, sdk)
                if (version is String) {
                    PydanticCacheService.getOrPutVersionFromVersionCache(it, version)
                }
            }
    }

    override fun packagesChanged(sdk: Sdk) {
        ApplicationManager.getApplication().invokeLater {
            if (isDisposed(sdk)) {
                return@invokeLater
            }

            val skeletons = PythonSdkUtil.findSkeletonsDir(sdk)
            val pydanticStub = skeletons?.findChild("pydantic")
            if (pydanticStub == null) {
                updateVersion(sdk)
            } else {
                runWriteAction {
                    if (isDisposed(sdk)) {
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