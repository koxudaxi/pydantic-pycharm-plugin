package com.koxudaxi.pydantic

import com.koxudaxi.pydantic.PydanticConfigService.Companion.getInstance
import javax.swing.JPanel
import javax.swing.JCheckBox
import javax.swing.JTextPane
import javax.swing.text.html.HTMLEditorKit
import com.intellij.util.ui.JBUI
import com.intellij.ide.plugins.newui.EmptyCaret
import javax.swing.event.HyperlinkEvent
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.project.Project
import com.intellij.ui.ColorUtil
import com.intellij.util.ui.HTMLEditorKitBuilder
import com.intellij.util.ui.UIUtil

class PydanticConfigPanel(project: Project) {
    lateinit var configPanel: JPanel
    private lateinit var initTypedCheckBox: JCheckBox
    private lateinit var ifEnabledIncludeTheTextPane: JTextPane
    private lateinit var warnUntypedFieldsCheckBox: JCheckBox
    private lateinit var ifEnabledRaiseATextPane: JTextPane
    private lateinit var textPane1: JTextPane

    init {
        val pydanticConfigService = getInstance(project)
        initTypedCheckBox.isSelected = pydanticConfigService.initTyped
        warnUntypedFieldsCheckBox.isSelected = pydanticConfigService.warnUntypedFields
        val enableInitTypedCheckBox = pydanticConfigService.mypyInitTyped == null
        initTypedCheckBox.isEnabled = enableInitTypedCheckBox
        ifEnabledIncludeTheTextPane.isEnabled = enableInitTypedCheckBox
        val warnUntypedFieldsCheckBox = pydanticConfigService.mypyWarnUntypedFields == null
        this.warnUntypedFieldsCheckBox.isEnabled = warnUntypedFieldsCheckBox
        ifEnabledRaiseATextPane.isEnabled = warnUntypedFieldsCheckBox
        setHyperlinkHtml(
            textPane1,
            "See <a href=\"https://koxudaxi.github.io/pydantic-pycharm-plugin/\">documentation</a> for more details.</p>"
        )
    }

    val initTyped: Boolean
        get() = initTypedCheckBox.isSelected
    val warnUntypedFields: Boolean
        get() = warnUntypedFieldsCheckBox.isSelected

    private fun setHyperlinkHtml(jTextPane: JTextPane, html: String) {
        jTextPane.contentType = "text/html"
        jTextPane.isEditable = false
        jTextPane.editorKit = HTMLEditorKitBuilder.simple()
        val kit = jTextPane.editorKit
        if (kit is HTMLEditorKit) {
            val css = kit.styleSheet
            css.addRule("a, a:link {color:#" + ColorUtil.toHex(JBUI.CurrentTheme.Link.Foreground.ENABLED) + ";}")
            css.addRule("a:visited {color:#" + ColorUtil.toHex(JBUI.CurrentTheme.Link.Foreground.VISITED) + ";}")
            css.addRule("a:hover {color:#" + ColorUtil.toHex(JBUI.CurrentTheme.Link.Foreground.HOVERED) + ";}")
            css.addRule("a:active {color:#" + ColorUtil.toHex(JBUI.CurrentTheme.Link.Foreground.PRESSED) + ";}")
            css.addRule("body {background-color:#" + ColorUtil.toHex(JBUI.CurrentTheme.DefaultTabs.background()) + ";}")
        }
        jTextPane.text = html
        jTextPane.caret = EmptyCaret.INSTANCE
        jTextPane.background = UIUtil.TRANSPARENT_COLOR
        jTextPane.isOpaque = false
        jTextPane.addHyperlinkListener { e: HyperlinkEvent ->
            if (e.eventType == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(e.url)
            }
        }
    }
}