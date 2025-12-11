package com.koxudaxi.pydantic

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.python.packaging.common.PythonPackageManagementListener
import com.jetbrains.python.sdk.PythonSdkUtil
import com.jetbrains.python.sdk.PythonSdkUtil.isDisposed
import com.jetbrains.python.statistics.sdks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PydanticPythonPackageManagementListener : PythonPackageManagementListener {
    private val scope = CoroutineScope(Dispatchers.Default)

    private fun updateVersion(sdk: Sdk) {
        val projects = ProjectManager.getInstance().openProjects.filter { it.sdks.contains(sdk) }
        for (project in projects) {
            updateVersionForProject(project, sdk)
        }
    }

    private fun updateVersionForProject(project: Project, sdk: Sdk) {
        PydanticCacheService.clear(project)
        scope.launch {
            withBackgroundProgress(project, "Updating Pydantic version cache") {
                val version = getPydanticVersion(project, sdk)
                if (version is String) {
                    PydanticCacheService.getOrPutVersionFromVersionCache(project, version)
                }
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