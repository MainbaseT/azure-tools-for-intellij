/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.webapp

import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.common.RiderRunProcessHandlerMessager
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft
import com.microsoft.azure.toolkit.lib.appservice.webapp.*
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.operation.Operation
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask
import java.util.concurrent.Callable

class CreateOrUpdateDotNetWebAppTask(
    private val config: DotNetAppServiceConfig,
    private val processHandlerMessager: RiderRunProcessHandlerMessager?
) : AzureTask<WebAppBase<*, *, *>>() {
    private val subTasks: MutableList<AzureTask<*>> = mutableListOf()

    private var appServicePlan: AppServicePlan? = null
    private var webApp: WebAppBase<*, *, *>? = null

    init {
        registerSubTask(getResourceGroupTask()) {}
        registerSubTask(getServicePlanTask()) { appServicePlan = it }

        val appDraft = Azure.az(AzureWebApp::class.java)
            .webApps(config.subscriptionId())
            .updateOrCreate<WebAppDraft>(config.appName(), config.resourceGroup())
            .toDotNetWebAppDraft()

        if (config.deploymentSlotName().isNullOrEmpty()) {
            val webAppTask =
                if (appDraft.exists()) getUpdateWebAppTask(appDraft)
                else getCreateWebAppTask(appDraft)
            registerSubTask(webAppTask) { webApp = it }
        } else {
            val slotDraft = getWebAppDeploymentSlot(appDraft)
            val slotTask =
                if (slotDraft.exists()) getUpdateWebAppSlotTask(slotDraft)
                else getCreateWebAppSlotTask(slotDraft)
            registerSubTask(slotTask) { webApp = it }
        }
    }

    private fun WebAppDraft.toDotNetWebAppDraft(): DotNetWebAppDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetWebAppDraft(draftOrigin)
            else DotNetWebAppDraft(name, resourceGroupName, module as WebAppModule)

        return draft
    }

    private fun WebAppDeploymentSlotDraft.toDotNetWebAppDeploymentSlotDraft(): DotNetWebAppDeploymentSlotDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetWebAppDeploymentSlotDraft(draftOrigin)
            else DotNetWebAppDeploymentSlotDraft(name, module as WebAppDeploymentSlotModule)

        return draft
    }

    private fun getResourceGroupTask(): AzureTask<ResourceGroup> =
        CreateResourceGroupTask(config.subscriptionId(), config.resourceGroup(), config.region())

    private fun getServicePlanTask(): AzureTask<AppServicePlan>? {
        if (!config.deploymentSlotName().isNullOrEmpty()) {
            processHandlerMessager?.info("Skip updating app service plan for deployment slot")
            return null
        }

        return AzureTask<AppServicePlan>(Callable {
            val planConfig = AppServiceConfig.getServicePlanConfig(config)
            val draft = Azure.az(AzureAppService::class.java)
                .plans(planConfig.subscriptionId)
                .updateOrCreate<AppServicePlanDraft>(planConfig.name, planConfig.resourceGroupName)
                .apply {
                    operatingSystem = planConfig.os
                    region = planConfig.region
                    pricingTier = planConfig.pricingTier
                }

            return@Callable draft.commit()
        })
    }

    private fun getCreateWebAppTask(draft: DotNetWebAppDraft) =
        AzureTask("Create new app(${config.appName()}) on subscription(${config.subscriptionId()})",
            Callable {
                with(draft) {
                    appServicePlan = this@CreateOrUpdateDotNetWebAppTask.appServicePlan
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()

                    createIfNotExist()
                }
            }
        )

    private fun getUpdateWebAppTask(draft: DotNetWebAppDraft) =
        AzureTask<WebApp>("Update function app(${config.appName()})",
            Callable {
                with(draft) {
                    appServicePlan = this@CreateOrUpdateDotNetWebAppTask.appServicePlan
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()

                    updateIfExist()
                }
            }
        )

    private fun getWebAppDeploymentSlot(webApp: WebApp): DotNetWebAppDeploymentSlotDraft {
        if (!webApp.exists()) {
            throw AzureToolkitRuntimeException("The Web App does not exist. Please make sure the Web App name is correct")
        }

        return webApp
            .slots()
            .updateOrCreate<WebAppDeploymentSlotDraft>(config.deploymentSlotName(), config.resourceGroup())
            .toDotNetWebAppDeploymentSlotDraft()
    }

    private fun getCreateWebAppSlotTask(draft: DotNetWebAppDeploymentSlotDraft) =
        AzureTask("Create new slot(${config.deploymentSlotName()}) on web app (${config.appName()})",
            Callable {
                with(draft) {
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()
                    configurationSource = config.deploymentSlotConfigurationSource()

                    commit()
                }
            }
        )

    private fun getUpdateWebAppSlotTask(draft: DotNetWebAppDeploymentSlotDraft) =
        AzureTask("Update function deployment slot(${config.deploymentSlotName()})",
            Callable {
                with(draft) {
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()

                    commit()
                }
            }
        )

    private fun getRuntime(runtimeConfig: DotNetRuntimeConfig?): DotNetRuntime? {
        if (runtimeConfig == null) return null

        return if (!runtimeConfig.isDocker) {
            DotNetRuntime(
                runtimeConfig.os(),
                runtimeConfig.stack,
                runtimeConfig.frameworkVersion,
                null,
                false
            )
        } else {
            DotNetRuntime(
                runtimeConfig.os(),
                null,
                null,
                null,
                true
            )
        }
    }

    private fun getDockerConfiguration(runtimeConfig: DotNetRuntimeConfig?): DockerConfiguration? {
        if (runtimeConfig == null || !runtimeConfig.isDocker) return null

        return DockerConfiguration.builder()
            .userName(runtimeConfig.username())
            .password(runtimeConfig.password())
            .registryUrl(runtimeConfig.registryUrl())
            .image(runtimeConfig.image())
            .startUpCommand(runtimeConfig.startUpCommand())
            .build()
    }

    private fun <T> registerSubTask(task: AzureTask<T>?, consumer: (result: T) -> Unit) {
        if (task != null) {
            subTasks.add(AzureTask<T>(Callable {
                val result = task.body.call()
                consumer(result)
                return@Callable result
            }))
        }
    }

    override fun doExecute(): WebAppBase<*, *, *> {
        Operation.execute(AzureString.fromString("Creating or updating Web App"), {
            processHandlerMessager?.let { OperationContext.current().messager = it }

            for (task in subTasks) {
                task.body.call()
            }
        }, null)

        return requireNotNull(webApp)
    }
}