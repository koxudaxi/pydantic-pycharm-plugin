package com.koxudaxi.pydantic

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent


class PydanticConfigurable internal constructor(project: Project,
                                            configPanel: PydanticConfigPanel) : Configurable {
    private val configPanel: PydanticConfigPanel
    private val pydanticConfigService: PydanticConfigService

    constructor(project: Project) : this(project, PydanticConfigPanel(project)) {}

    override fun getDisplayName(): String {
        return "pydantic"
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
        return true
    }

    override fun apply() {
    }

    override fun disposeUIResources() {
    }

    companion object {
        private val LOG = Logger.getInstance(PydanticConfigurable::class.java)
    }

    init {
        this.configPanel = configPanel
        pydanticConfigService = PydanticConfigService.getInstance(project)!!
    }
}