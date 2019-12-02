package com.koxudaxi.pydantic;

import com.intellij.openapi.project.Project;

import javax.swing.*;

public class PydanticConfigPanel {
    private Project project;
    PydanticConfigPanel(Project project) {
        this.project = project;
    }
    private JPanel configPanel;

    public JPanel getConfigPanel() {
        return configPanel;
    }
}
