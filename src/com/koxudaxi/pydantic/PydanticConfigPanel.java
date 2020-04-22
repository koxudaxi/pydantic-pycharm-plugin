package com.koxudaxi.pydantic;

import com.intellij.ide.BrowserUtil;
import com.intellij.openapi.project.Project;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

public class PydanticConfigPanel {

    PydanticConfigPanel(Project project) {
        PydanticConfigService pydanticConfigService = PydanticConfigService.Companion.getInstance(project);

        this.initTypedCheckBox.setSelected(pydanticConfigService.getInitTyped());
        this.warnUntypedFieldsCheckBox.setSelected(pydanticConfigService.getWarnUntypedFields());

        this.textPane1.setText("<p style=\"font-family:Arial, Helvetica, sans-serif;font-size:130%;\">" +
                "See <a href=\"https://koxudaxi.github.io/pydantic-pycharm-plugin/\">documentation</a> for more details.</p>");
        this.textPane1.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                BrowserUtil.browse(e.getURL());
            }
        });
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
}
