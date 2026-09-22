/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.common.component.resourcegroup;

import com.intellij.ui.components.JBLabel;
import com.microsoft.azure.toolkit.intellij.common.AzureDialog;
import com.microsoft.azure.toolkit.intellij.common.SwingUtils;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.form.AzureForm;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessageBundle;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.resource.AzureResources;
import com.microsoft.azure.toolkit.lib.resource.ResourceGroupDraft;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Collections;
import java.util.List;

public class ResourceGroupCreationDialog extends AzureDialog<ResourceGroupDraft>
    implements AzureForm<ResourceGroupDraft> {
    private Subscription subscription;
    private JBLabel labelDescription;
    private JPanel contentPanel;
    private ResourceGroupNameTextField textName;

    public ResourceGroupCreationDialog(Subscription subscription) {
        super();
        this.init();
        this.subscription = subscription;
        this.textName.setSubscription(subscription);
        SwingUtils.setTextAndEnableAutoWrap(this.labelDescription, AzureMessageBundle.message("common.resourceGroup.description").toString());
        this.pack();
    }

    @Override
    public AzureForm<ResourceGroupDraft> getForm() {
        return this;
    }

    @Override
    protected String getDialogTitle() {
        return AzureMessageBundle.message("common.resourceGroup.create.title").toString();
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return this.contentPanel;
    }

    @Override
    public ResourceGroupDraft getValue() {
        final String rgName = this.textName.getValue();
        return Azure.az(AzureResources.class).groups(this.subscription.getId()).create(rgName, rgName);
    }

    @Override
    public void setValue(final ResourceGroupDraft data) {
        this.subscription = data.getSubscription();
        this.textName.setValue(data.getName());
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        return Collections.singletonList(this.textName);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.textName = new ResourceGroupNameTextField();
        this.textName.setRequired(true);
    }
}
