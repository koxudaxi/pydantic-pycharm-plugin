package com.koxudaxi.pydantic

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.impl.PyStringLiteralExpressionImpl
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticCacheService {
    private var version: KotlinVersion? = null
    private var allowedConfigKwargs: Set<String>? = null

    private fun getAllowedConfigKwargs(project: Project, context: TypeEvalContext): Set<String>? {
        val baseConfig = getPydanticBaseConfig(project, context) ?: return null
        return baseConfig.classAttributes
            .mapNotNull { it.name }
            .filterNot { it.startsWith("__") && it.endsWith("__") }
            .toSet()
    }
    private fun getVersion(project: Project, context: TypeEvalContext): KotlinVersion? {
        val version = getPsiElementByQualifiedName(VERSION_QUALIFIED_NAME, project, context) as? PyTargetExpression
            ?: return null
        val versionString =
            (version.findAssignedValue()?.lastChild?.firstChild?.nextSibling as? PyStringLiteralExpression)?.stringValue
                ?: (version.findAssignedValue() as? PyStringLiteralExpressionImpl)?.stringValue ?: return null
        return pydanticVersionCache.getOrPut(versionString) {
            val versionList = versionString.split(VERSION_SPLIT_PATTERN).map { it.toIntOrNull() ?: 0 }
            val pydanticVersion = when {
                versionList.size == 1 -> KotlinVersion(versionList[0], 0)
                versionList.size == 2 -> KotlinVersion(versionList[0], versionList[1])
                versionList.size >= 3 -> KotlinVersion(versionList[0], versionList[1], versionList[2])
                else -> null
            } ?: KotlinVersion(0, 0)
            pydanticVersionCache[versionString] = pydanticVersion
            pydanticVersion
        }
    }

    private fun getOrPutVersion(project: Project, context: TypeEvalContext): KotlinVersion? {
        if (version != null) return version
        return getVersion(project, context).apply { version = this }
    }

    private fun getOrAllowedConfigKwargs(project: Project, context: TypeEvalContext): Set<String>? {
        if (allowedConfigKwargs != null) return allowedConfigKwargs
        return getAllowedConfigKwargs(project, context).apply { allowedConfigKwargs = this }
    }

    private fun clear() {
        version = null
        allowedConfigKwargs = null
    }

    companion object {
        fun getVersion(project: Project, context: TypeEvalContext): KotlinVersion? {
            return getInstance(project).getOrPutVersion(project, context)
        }

        fun getAllowedConfigKwargs(project: Project, context: TypeEvalContext): Set<String>? {
            return getInstance(project).getOrAllowedConfigKwargs(project, context)
        }

        fun clear(project: Project) {
            return getInstance(project).clear()
        }

        private fun getInstance(project: Project): PydanticCacheService {
            return ServiceManager.getService(project, PydanticCacheService::class.java)
        }
    }

}
