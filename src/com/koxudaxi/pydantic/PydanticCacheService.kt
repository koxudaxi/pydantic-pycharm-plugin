package com.koxudaxi.pydantic

import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.types.TypeEvalContext
import com.jetbrains.python.sdk.pythonSdk

class PydanticCacheService(val project: Project) {
    private var version: KotlinVersion? = null
    private var allowedConfigKwargs: Set<String>? = null
    private var configDictDefaults: Map<String, Any?>? = null

    private fun getConfigDictDefaults(project: Project, context: TypeEvalContext): Map<String, Any?>? {
        val configDictDefaults = getPydanticConfigDictDefaults(project, context) ?: return null
        return configDictDefaults.arguments.filter { CONFIG_TYPES.containsKey(it.name) }
            .mapNotNull {
                val name = it.name ?: return@mapNotNull null
                name to getConfigValue(name, configDictDefaults.getKeywordArgument(name), context)
            }.toMap()
    }

    private fun getAllowedConfigKwargs(context: TypeEvalContext): Set<String>? {
        val baseConfigAttributes = when {
            isV2 -> getPydanticConfigDictDefaults(project, context)?.arguments?.mapNotNull { it.name }
            else -> { getPydanticBaseConfig(project, context)?.classAttributes?.mapNotNull { it.name } }
        } ?: return null
        return baseConfigAttributes
                .filterNot { it.startsWith("__") && it.endsWith("__") }
                .toSet()
    }
    private fun getVersion(): KotlinVersion? {
        val sdk = project.pythonSdk ?: return null
        val versionString = sdk.pydanticVersion ?: return null
        return getOrPutVersionFromVersionCache(versionString)
    }

    private fun getOrPutVersionFromVersionCache(version: String): KotlinVersion? {
        return pydanticVersionCache.getOrPut(version) {
            val versionList = version.split(VERSION_SPLIT_PATTERN).map { it.toIntOrNull() ?: 0 }
            val pydanticVersion = when {
                versionList.size == 1 -> KotlinVersion(versionList[0], 0)
                versionList.size == 2 -> KotlinVersion(versionList[0], versionList[1])
                versionList.size >= 3 -> KotlinVersion(versionList[0], versionList[1], versionList[2])
                else -> null
            } ?: return null
            pydanticVersionCache[version] = pydanticVersion
            pydanticVersion
        }
    }

    internal fun getOrPutVersion(): KotlinVersion? {
        if (version != null) return version
        return getVersion().apply { version = this }
    }

    internal fun setVersion(version: String): KotlinVersion? {
        return getOrPutVersionFromVersionCache(version).also { this.version = it }
        }

    private fun getOrAllowedConfigKwargs(context: TypeEvalContext): Set<String>? {
        if (allowedConfigKwargs != null) return allowedConfigKwargs
        return getAllowedConfigKwargs(context).apply { allowedConfigKwargs = this }
    }

    private fun getOrConfigDictDefaults(project: Project, context: TypeEvalContext): Map<String, Any?>? {
        if ( configDictDefaults != null) return configDictDefaults
        return getConfigDictDefaults(project, context).apply { configDictDefaults = this }
    }

    private fun clear() {
        version = null
        allowedConfigKwargs = null
    }

    internal val isV2 get() =  this.getOrPutVersion().isV2

    companion object {
        fun getVersion(project: Project): KotlinVersion? {
            return getInstance(project).getOrPutVersion()
        }

        fun setVersion(project: Project, version: String): KotlinVersion? {
            return getInstance(project).setVersion(version)
        }
        fun getOrPutVersionFromVersionCache(project: Project, version: String): KotlinVersion? {
            return getInstance(project).getOrPutVersionFromVersionCache(version)
        }

        fun getAllowedConfigKwargs(project: Project, context: TypeEvalContext): Set<String>? {
            return getInstance(project).getOrAllowedConfigKwargs(context)
        }

        fun getConfigDictDefaults(project: Project, context: TypeEvalContext): Map<String, Any?>? {
            return getInstance(project).getOrConfigDictDefaults(project, context)
        }

        fun clear(project: Project) {
            return getInstance(project).clear()
        }

        internal fun getInstance(project: Project): PydanticCacheService {
            return project.getService(PydanticCacheService::class.java)
        }
    }

}
