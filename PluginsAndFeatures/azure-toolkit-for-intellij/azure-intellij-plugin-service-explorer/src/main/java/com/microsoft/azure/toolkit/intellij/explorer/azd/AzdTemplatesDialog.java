package com.microsoft.azure.toolkit.intellij.explorer.azd;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public class AzdTemplatesDialog extends DialogWrapper {
    private final @Nullable Project project;

    public AzdTemplatesDialog(@Nullable Project project) {
        super(project, true);
        this.project = project;
        setTitle("Available Azd Templates for Java");
        init();
        setSize(1400, 800);
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        return new AzdTemplatesLibrary(project, this);
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{};
    }
}
