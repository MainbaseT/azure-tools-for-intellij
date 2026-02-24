package com.microsoft.azure.toolkit.intellij.explorer.azd;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.util.Objects;

public class ConfirmAndRunDialog extends DialogWrapper {

    private final Project project;
    private String label;
    private String command;
    private String eventName;
    private Runnable onOkAction;

    public ConfirmAndRunDialog(Project project, String title) {
        super(true);
        this.project = project;
        setSize(300, 150);
        setTitle(Objects.requireNonNull(title, "Title must not be null"));
        setOKButtonText("Run");
    }

    public ConfirmAndRunDialog setLabel(String label) {
        this.label = Objects.requireNonNull(label, "Label must not be null");
        return this;
    }

    public ConfirmAndRunDialog setEventName(String eventName) {
        this.eventName = Objects.requireNonNull(eventName, "Event name must not be null");
        return this;
    }

    public ConfirmAndRunDialog setOnOkAction(Runnable onOkAction) {
        this.onOkAction = onOkAction;
        return this;
    }

    public ConfirmAndRunDialog setOkButtonText(String okButtonText) {
        setOKButtonText(okButtonText);
        return this;
    }

    @Override
    public void show() {
        init();
        super.show();
    }

    @Override
    protected @Nullable JComponent createCenterPanel() {
        JPanel panel = new JPanel(new BorderLayout());
        JLabel label = new JLabel(this.label);
        label.setHorizontalAlignment(SwingConstants.CENTER);
        panel.add(label, BorderLayout.CENTER);
        return panel;
    }

    @Override
    protected Action @NotNull [] createActions() {
        return new Action[]{getOKAction(), getCancelAction()};
    }

    @Override
    protected void doOKAction() {
        AzdUtils.logTelemetryEvent("azd-" + eventName + "-ok");
        super.doOKAction();
        if(onOkAction != null) {
            onOkAction.run();
        }
    }

    @Override
    public void doCancelAction() {
        AzdUtils.logTelemetryEvent("azd-" + eventName + "-cancel");
        super.doCancelAction();
    }
}
