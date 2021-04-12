package com.koxudaxi.pydantic;

import com.intellij.ide.BrowserUtil;
import com.intellij.ide.plugins.newui.EmptyCaret;
import com.intellij.openapi.project.Project;
import com.intellij.ui.ColorUtil;
import com.intellij.util.ui.JBUI;
import com.intellij.util.ui.UIUtil;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import javax.swing.text.EditorKit;
import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.StyleSheet;


public class PydanticConfigPanel {

    PydanticConfigPanel(Project project) {
        PydanticConfigService pydanticConfigService = PydanticConfigService.Companion.getInstance(project);

        this.initTypedCheckBox.setSelected(pydanticConfigService.getInitTyped());
        this.warnUntypedFieldsCheckBox.setSelected(pydanticConfigService.getWarnUntypedFields());

        boolean enableInitTypedCheckBox = pydanticConfigService.getMypyInitTyped() == null;
        this.initTypedCheckBox.setEnabled(enableInitTypedCheckBox);
        this.ifEnabledIncludeTheTextPane.setEnabled(enableInitTypedCheckBox);

        boolean warnUntypedFieldsCheckBox = pydanticConfigService.getMypyWarnUntypedFields() == null;
        this.warnUntypedFieldsCheckBox.setEnabled(warnUntypedFieldsCheckBox);
        this.ifEnabledRaiseATextPane.setEnabled(warnUntypedFieldsCheckBox);

        setHyperlinkHtml(this.textPane1, "See <a href=\"https://koxudaxi.github.io/pydantic-pycharm-plugin/\">documentation</a> for more details.</p>");
    }

    private JPanel configPanel;
    private JCheckBox initTypedCheckBox;
    private JTextPane ifEnabledIncludeTheTextPane;
    private JCheckBox warnUntypedFieldsCheckBox;
    private JTextPane ifEnabledRaiseATextPane;
    private JTextPane textPane1;

    public Boolean getInitTyped() {
        return initTypedCheckBox.isSelected();
    }

    public Boolean getWarnUntypedFields() {
        return warnUntypedFieldsCheckBox.isSelected();
    }

    public JPanel getConfigPanel() {
        return configPanel;
    }

    private void setHyperlinkHtml(JTextPane jTextPane, String html) {

        jTextPane.setContentType("text/html");
        jTextPane.setEditable(false);
        jTextPane.setEditorKit(UIUtil.getHTMLEditorKit());

        EditorKit kit = jTextPane.getEditorKit();
        if (kit instanceof HTMLEditorKit) {
            StyleSheet css = ((HTMLEditorKit) kit).getStyleSheet();

            css.addRule("a, a:link {color:#" + ColorUtil.toHex(JBUI.CurrentTheme.Link.Foreground.ENABLED) + ";}");
            css.addRule("a:visited {color:#" + ColorUtil.toHex(JBUI.CurrentTheme.Link.Foreground.VISITED) + ";}");
            css.addRule("a:hover {color:#" + ColorUtil.toHex(JBUI.CurrentTheme.Link.Foreground.HOVERED) + ";}");
            css.addRule("a:active {color:#" + ColorUtil.toHex(JBUI.CurrentTheme.Link.Foreground.PRESSED) + ";}");
            css.addRule("body {background-color:#" + ColorUtil.toHex(JBUI.CurrentTheme.DefaultTabs.background()) + ";}");
        }

        jTextPane.setText(html);
        jTextPane.setCaret(EmptyCaret.INSTANCE);
        jTextPane.setBackground(UIUtil.TRANSPARENT_COLOR);
        jTextPane.setOpaque(false);
        jTextPane.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(e.getURL());
            }
        });
    }
}
