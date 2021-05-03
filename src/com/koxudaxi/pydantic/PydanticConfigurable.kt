package com.koxudaxi.pydantic

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent


class PydanticConfigurable internal constructor(project: Project) : Configurable {
    private val pydanticConfigService: PydanticConfigService = PydanticConfigService.getInstance(project)
    private val configPanel: PydanticConfigPanel = PydanticConfigPanel(project)
    override fun getDisplayName(): String {
        return "Pydantic"
    }

    override fun getHelpTopic(): String? {
        return null
    }

    override fun createComponent(): JComponent? {
        reset()
        return configPanel.configPanel
    }

    override fun reset() {}

    override fun isModified(): Boolean {
        if (configPanel.initTyped == null || configPanel.warnUntypedFields == null) return false
        return (pydanticConfigService.initTyped != configPanel.initTyped) ||
                (pydanticConfigService.warnUntypedFields != configPanel.warnUntypedFields)
    }

    override fun apply() {
        pydanticConfigService.initTyped = configPanel.initTyped
        pydanticConfigService.warnUntypedFields = configPanel.warnUntypedFields
    }

    override fun disposeUIResources() {
    }
}