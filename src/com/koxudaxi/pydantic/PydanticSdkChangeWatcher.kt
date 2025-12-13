package com.koxudaxi.pydantic

import com.intellij.ProjectTopics
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.jetbrains.python.sdk.pythonSdk
import com.jetbrains.python.statistics.modules

class PydanticSdkChangeWatcher(private val project: Project) {
    private var sdkFingerprint: String? = computeSdkFingerprint()

    init {
        project.messageBus.connect(project).subscribe(ProjectTopics.PROJECT_ROOTS, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                val current = computeSdkFingerprint()
                if (current != sdkFingerprint) {
                    sdkFingerprint = current
                    PydanticCacheService.clear(project)
                }
            }
        })
    }

    private fun computeSdkFingerprint(): String? {
        val sdkFingerprints = project.modules
            .mapNotNull { it.pythonSdk?.sdkFingerprint }
            .sorted()
        val fingerprint = when {
            sdkFingerprints.isNotEmpty() -> sdkFingerprints.joinToString("|")
            else -> project.sdk?.sdkFingerprint
        }
        return fingerprint
    }
}

private val Sdk.sdkFingerprint: String
    get() = homePath ?: name
