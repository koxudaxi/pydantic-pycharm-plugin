package com.koxudaxi.pydantic

import com.intellij.openapi.project.Project
import com.jetbrains.python.psi.PyStringLiteralExpression
import com.jetbrains.python.psi.PyTargetExpression
import com.jetbrains.python.psi.impl.PyStringLiteralExpressionImpl
import com.jetbrains.python.psi.types.TypeEvalContext

class PydanticCacheService(val project: Project) {
    private var version: KotlinVersion? = null
    private var allowedConfigKwargs: Set<String>? = null

    private fun getAllowedConfigKwargs(context: TypeEvalContext): Set<String>? {
        val baseConfig = getPydanticBaseConfig(project, context) ?: return null
        return baseConfig.classAttributes
            .mapNotNull { it.name }
            .filterNot { it.startsWith("__") && it.endsWith("__") }
            .toSet()
    }
    private fun getVersion(context: TypeEvalContext): KotlinVersion? {
        val version = getPsiElementByQualifiedName(VERSION_QUALIFIED_NAME, project, context) as? PyTargetExpression
            ?: return null
        val versionString =
            (version.findAssignedValue()?.lastChild?.firstChild?.nextSibling as? PyStringLiteralExpression)?.stringValue
                ?: (version.findAssignedValue() as? PyStringLiteralExpressionImpl)?.stringValue ?: return null
        return setVersion(versionString)
    }

    private fun setVersion(version: String): KotlinVersion {
        return pydanticVersionCache.getOrPut(version) {
            val versionList = version.split(VERSION_SPLIT_PATTERN).map { it.toIntOrNull() ?: 0 }
            val pydanticVersion = when {
                versionList.size == 1 -> KotlinVersion(versionList[0], 0)
                versionList.size == 2 -> KotlinVersion(versionList[0], versionList[1])
                versionList.size >= 3 -> KotlinVersion(versionList[0], versionList[1], versionList[2])
                else -> null
            } ?: KotlinVersion(0, 0)
            pydanticVersionCache[version] = pydanticVersion
            pydanticVersion
        }
    }

    private fun getOrPutVersion(context: TypeEvalContext): KotlinVersion? {
        if (version != null) return version
        return getVersion(context).apply { version = this }
    }

    private fun getOrAllowedConfigKwargs(context: TypeEvalContext): Set<String>? {
        if (allowedConfigKwargs != null) return allowedConfigKwargs
        return getAllowedConfigKwargs(context).apply { allowedConfigKwargs = this }
    }

    private fun clear() {
        version = null
        allowedConfigKwargs = null
    }

    internal fun isV2(typeEvalContext: TypeEvalContext) = this.getOrPutVersion(typeEvalContext).isV2

    companion object {
        fun getVersion(project: Project, context: TypeEvalContext): KotlinVersion? {
            return getInstance(project).getOrPutVersion(context)
        }

        fun setVersion(project: Project, version: String): KotlinVersion? {
            return getInstance(project).setVersion(version)
        }

        fun getAllowedConfigKwargs(project: Project, context: TypeEvalContext): Set<String>? {
            return getInstance(project).getOrAllowedConfigKwargs(context)
        }

        fun clear(project: Project) {
            return getInstance(project).clear()
        }

        internal fun getInstance(project: Project): PydanticCacheService {
            return project.getService(PydanticCacheService::class.java)
        }
    }

}
