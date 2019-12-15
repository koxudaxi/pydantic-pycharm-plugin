package com.koxudaxi.pydantic;

import com.intellij.openapi.project.Project;

import javax.swing.*;

public class PydanticConfigPanel {

    PydanticConfigPanel(Project project) {
        PydanticConfigService pydanticConfigService = PydanticConfigService.Companion.getInstance(project);

        this.initTypedCheckBox.setSelected(pydanticConfigService.getInitTyped());
        this.warnUntypedFieldsCheckBox.setSelected(pydanticConfigService.getWarnUntypedFields());

    }

    private JPanel configPanel;
    private JCheckBox initTypedCheckBox;
    private JTextPane ifEnabledIncludeTheTextPane;
    private JCheckBox warnUntypedFieldsCheckBox;
    private JTextPane ifEnabledRaiseATextPane;

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
