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
    var pyprojectToml: String? = null
    var parsableTypeMap = mutableMapOf<String, List<String>>()
    var parsableTypeHighlightType: ProblemHighlightType = ProblemHighlightType.WARNING
    override fun getState(): PydanticConfigService {
        return this
    }

    override fun loadState(config: PydanticConfigService) {
        XmlSerializerUtil.copyBean(config, this)
    }

    companion object {
        fun getInstance(project: Project?): PydanticConfigService {
            return ServiceManager.getService(project!!, PydanticConfigService::class.java)
        }
    }

}