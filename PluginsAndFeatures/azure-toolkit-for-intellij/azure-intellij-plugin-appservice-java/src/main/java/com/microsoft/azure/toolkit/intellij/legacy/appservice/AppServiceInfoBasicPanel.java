/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.intellij.legacy.appservice;

import com.intellij.openapi.project.Project;
import com.intellij.ui.TitledSeparator;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifact;
import com.microsoft.azure.toolkit.intellij.common.AzureArtifactComboBox;
import com.microsoft.azure.toolkit.intellij.common.AzureFormPanel;
import com.microsoft.azure.toolkit.intellij.legacy.appservice.platform.RuntimeComboBox;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig;
import com.microsoft.azure.toolkit.lib.appservice.config.RuntimeConfig;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AzureAccount;
import com.microsoft.azure.toolkit.lib.common.form.AzureFormInput;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.apache.commons.compress.utils.FileNameUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.swing.*;
import java.util.*;
import java.util.function.Supplier;

import static com.microsoft.azuretools.utils.WebAppUtils.isSupportedArtifactType;

public class AppServiceInfoBasicPanel<T extends AppServiceConfig> extends JPanel implements AzureFormPanel<T> {
    private final Project project;
    private final Supplier<? extends T> supplier;
    private T config;

    private JPanel contentPanel;
    private AppNameInput textName;
    private RuntimeComboBox selectorRuntime;
    private AzureArtifactComboBox selectorApplication;
    private TitledSeparator deploymentTitle;
    private JLabel lblArtifact;
    private JLabel lblName;
    private JLabel lblPlatform;
    @Setter
    @Getter
    private String defaultImage;

    public AppServiceInfoBasicPanel(final Project project, final Supplier<? extends T> defaultConfigSupplier) {
        super();
        this.project = project;
        this.supplier = defaultConfigSupplier;
        $$$setupUI$$$(); // tell IntelliJ to call createUIComponents() here.
        this.init();
    }

    private void init() {
        this.textName.setRequired(true);
        this.selectorRuntime.setRequired(true);

        this.selectorApplication.setFileFilter(virtualFile -> {
            final String ext = FileNameUtils.getExtension(virtualFile.getPath());
            final Runtime platform = this.selectorRuntime.getValue();
            return StringUtils.isNotBlank(ext) && isSupportedArtifactType(platform, ext);
        });
        this.setDeploymentVisible(false);

        this.lblName.setLabelFor(textName);
        this.lblPlatform.setLabelFor(selectorRuntime);
        this.lblArtifact.setLabelFor(selectorApplication);
    }

    @SneakyThrows
    @Override
    public T getValue() {
        final String name = this.textName.getValue();
        final Runtime platform = this.selectorRuntime.getValue();
        final AzureArtifact artifact = this.selectorApplication.getValue();
        final Account account = Azure.az(AzureAccount.class).account();
        final Subscription subscription = Optional.ofNullable(config).map(AppServiceConfig::subscriptionId)
                                                  .map(account::getSubscription)
                                                  .orElseGet(() -> account.getSelectedSubscriptions().stream().findFirst().orElse(null));
        final T result = this.config == null ? supplier.get() : this.config;
        result.appName(name);
        Optional.ofNullable(subscription).map(Subscription::getId).ifPresent(result::subscriptionId);
        Optional.ofNullable(platform).filter(r -> !r.isDocker()).map(RuntimeConfig::fromRuntime).ifPresent(result::runtime);
        if (Objects.nonNull(platform) && platform.isDocker()) {
            final RuntimeConfig runtime = Optional.ofNullable(result.runtime()).orElseGet(RuntimeConfig::new);
            runtime.os(OperatingSystem.DOCKER);
            runtime.image(StringUtils.firstNonBlank(runtime.image(), defaultImage));
            result.runtime(runtime);
        }
        result.appSettings(ObjectUtils.firstNonNull(this.config.appSettings(), new HashMap<>()));
        this.config = result;
        return config;
    }

    @Override
    public void setValue(final T config) {
        this.config = config;
        final Subscription subscription = Optional.ofNullable(config.subscriptionId())
                                                  .map(Azure.az(AzureAccount.class).account()::getSubscription)
                                                  .orElse(null);
        this.textName.setValue(config.appName());
        this.textName.setSubscription(subscription);
        Optional.ofNullable(config.runtime())
                .map(c -> config instanceof FunctionAppConfig ? RuntimeConfig.toFunctionAppRuntime(c) : RuntimeConfig.toWebAppRuntime(c))
                .ifPresent(this.selectorRuntime::setValue);
    }

    @Override
    public List<AzureFormInput<?>> getInputs() {
        final AzureFormInput<?>[] inputs = {
            this.textName,
            this.selectorRuntime
        };
        return Arrays.asList(inputs);
    }

    @Override
    public void setVisible(final boolean visible) {
        this.contentPanel.setVisible(visible);
        super.setVisible(visible);
    }

    public RuntimeComboBox getSelectorRuntime() {
        return selectorRuntime;
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
        this.selectorApplication = new AzureArtifactComboBox(project, true);
        this.selectorRuntime = new RuntimeComboBox();
        this.textName = new AppNameInput();
        this.textName.setRequired(true);
    }

    public void setDeploymentVisible(boolean visible) {
        this.deploymentTitle.setVisible(visible);
        this.lblArtifact.setVisible(visible);
        this.selectorApplication.setVisible(visible);
    }

    public void setFixedRuntime(final Runtime runtime) {
        selectorRuntime.setPlatformList(Collections.singletonList(runtime));
    }
}
