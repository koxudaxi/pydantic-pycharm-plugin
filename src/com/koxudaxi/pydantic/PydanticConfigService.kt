package com.koxudaxi.pydantic

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(name = "PydanticConfigService", storages = [Storage("pydantic.xml")])
class PydanticConfigService : PersistentStateComponent<PydanticConfigService> {
    var initTyped = true
    var warnUntypedFields = false
    var mypyInitTyped: Boolean? = null
    var mypyWarnUntypedFields: Boolean? = null
    var pyprojectToml: String? = null
    var mypyIni: String? = null
    var parsableTypeMap = mapOf<String, List<String>>()
    var parsableTypeHighlightType: ProblemHighlightType = ProblemHighlightType.WARNING
    var acceptableTypeMap = mapOf<String, List<String>>()
    var acceptableTypeHighlightType: ProblemHighlightType = ProblemHighlightType.WEAK_WARNING
    var ignoreInitMethodArguments: Boolean = false
    val currentInitTyped: Boolean
        get() = this.mypyInitTyped ?: this.initTyped
    val currentWarnUntypedFields: Boolean
        get() = this.mypyWarnUntypedFields ?: this.warnUntypedFields

    override fun getState(): PydanticConfigService {
        return this
    }

    override fun loadState(config: PydanticConfigService) {
        XmlSerializerUtil.copyBean(config, this)
    }

    companion object {
        fun getInstance(project: Project): PydanticConfigService {
            return project.getService(PydanticConfigService::class.java)
        }
    }

}