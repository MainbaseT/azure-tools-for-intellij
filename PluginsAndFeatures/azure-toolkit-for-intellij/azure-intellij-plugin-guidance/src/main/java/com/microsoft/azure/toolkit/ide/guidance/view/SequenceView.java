package com.microsoft.azure.toolkit.ide.guidance.view;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.project.Project;
import com.intellij.ui.HyperlinkLabel;
import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.util.ui.JBFont;
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons;
import com.microsoft.azure.toolkit.ide.guidance.Guidance;
import com.microsoft.azure.toolkit.ide.guidance.GuidanceViewManager;
import com.microsoft.azure.toolkit.ide.guidance.Phase;
import com.microsoft.azure.toolkit.ide.guidance.phase.PhaseManager;
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons;

import javax.annotation.Nonnull;
import javax.swing.*;
import java.util.List;

public class SequenceView {
    private JPanel pnlRoot;
    private JLabel guidanceIcon;
    private JLabel titleLabel;
    private JPanel phasesPanel;
    private JPanel docPanel;
    private HyperlinkLabel closeButton;
    private JPanel bodyPanel;

    private final Project project;

    public SequenceView(@Nonnull final Project project) {
        this.project = project;
        $$$setupUI$$$();
        init();
    }

    private void init() {
        this.titleLabel.setFont(JBFont.h2().asBold());
        this.closeButton.setIcon(AllIcons.Actions.Cancel);
        this.closeButton.setHyperlinkText("Abort");
        this.closeButton.setHyperlinkTarget(null);
        this.closeButton.addHyperlinkListener(e -> GuidanceViewManager.getInstance().showGuidanceWelcome(project));
    }

    public void showProcess(@Nonnull Guidance guidance) {
        this.guidanceIcon.setIcon(IntelliJAzureIcons.getIcon(AzureIcons.Common.AZURE));
        this.titleLabel.setText(guidance.getTitle());
        fillPhase(guidance);
    }

    private void fillPhase(@Nonnull Guidance guidance) {
        this.phasesPanel.removeAll();
        final List<Phase> phases = guidance.getPhases();
        this.phasesPanel.setLayout(new GridLayoutManager(phases.size(), 1));
        for (int i = 0; i < phases.size(); i++) {
            final Phase phase = phases.get(i);
            final JPanel phasePanel = PhaseManager.createPhasePanel(phase);
            final GridConstraints gridConstraints = new GridConstraints(i, 0, 1, 1, 0, 3, 3, 3, null, null, null, 0);
            this.phasesPanel.add(phasePanel, gridConstraints);
        }
    }

    // CHECKSTYLE IGNORE check FOR NEXT 1 LINES
    void $$$setupUI$$$() {
    }

    public void setVisible(boolean visible) {
        this.pnlRoot.setVisible(visible);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        //noinspection DialogTitleCapitalization
    }
}
