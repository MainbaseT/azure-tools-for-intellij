/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.functionapp

import com.azure.resourcemanager.resources.fluentcore.utils.ResourceManagerUtils
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntime
import com.microsoft.azure.toolkit.intellij.appservice.DotNetRuntimeConfig
import com.microsoft.azure.toolkit.intellij.common.RiderRunProcessHandlerMessager
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.AzureAppService
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.appservice.function.*
import com.microsoft.azure.toolkit.lib.appservice.model.DockerConfiguration
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem
import com.microsoft.azure.toolkit.lib.appservice.model.PricingTier
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlan
import com.microsoft.azure.toolkit.lib.appservice.plan.AppServicePlanDraft
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException
import com.microsoft.azure.toolkit.lib.common.model.Region
import com.microsoft.azure.toolkit.lib.common.operation.Operation
import com.microsoft.azure.toolkit.lib.common.operation.OperationContext
import com.microsoft.azure.toolkit.lib.common.task.AzureTask
import com.microsoft.azure.toolkit.lib.resource.ResourceGroup
import com.microsoft.azure.toolkit.lib.resource.task.CreateResourceGroupTask
import com.microsoft.azure.toolkit.lib.storage.AzureStorageAccount
import com.microsoft.azure.toolkit.lib.storage.StorageAccount
import com.microsoft.azure.toolkit.lib.storage.StorageAccountDraft
import com.microsoft.azure.toolkit.lib.storage.model.Kind
import com.microsoft.azure.toolkit.lib.storage.model.Redundancy
import org.apache.commons.lang3.StringUtils
import java.util.concurrent.Callable

class CreateOrUpdateDotNetFunctionAppTask(
    private val config: DotNetFunctionAppConfig,
    private val processHandlerMessager: RiderRunProcessHandlerMessager?
) : AzureTask<FunctionAppBase<*, *, *>>() {
    companion object {
        private const val FUNCTION_APP_NAME_PATTERN = "[^a-zA-Z0-9]"

        private const val SCM_DO_BUILD_DURING_DEPLOYMENT = "SCM_DO_BUILD_DURING_DEPLOYMENT"
        private const val WEBSITE_RUN_FROM_PACKAGE = "WEBSITE_RUN_FROM_PACKAGE"
        private const val FUNCTIONS_INPROC_NET8_ENABLED = "FUNCTIONS_INPROC_NET8_ENABLED"
    }

    private val functionAppRegex = Regex(FUNCTION_APP_NAME_PATTERN)

    private val subTasks: MutableList<AzureTask<*>> = mutableListOf()

    private var appServicePlan: AppServicePlan? = null
    private var storageAccount: StorageAccount? = null
    private var functionApp: FunctionAppBase<*, *, *>? = null

    init {
        registerSubTask(getResourceGroupTask()) {}
        registerSubTask(getServicePlanTask()) { appServicePlan = it }

        val appDraft = Azure.az(AzureFunctions::class.java)
            .functionApps(config.subscriptionId())
            .updateOrCreate<FunctionAppDraft>(config.appName(), config.resourceGroup())
            .toDotNetFunctionAppDraft()

        if (appDraft.isDraftForCreating) {
            registerSubTask(getStorageAccountTask()) { storageAccount = it }
        }

        if (config.deploymentSlotName().isNullOrEmpty()) {
            val functionTask =
                if (appDraft.exists()) getUpdateFunctionAppTask(appDraft)
                else getCreateFunctionAppTask(appDraft)
            registerSubTask(functionTask) { functionApp = it }
        } else {
            val slotDraft = getFunctionDeploymentSlot(appDraft)
            val slotTask =
                if (slotDraft.exists()) getUpdateFunctionSlotTask(slotDraft)
                else getCreateFunctionSlotTask(slotDraft)
            registerSubTask(slotTask) { functionApp = it }
        }
    }

    private fun FunctionAppDraft.toDotNetFunctionAppDraft(): DotNetFunctionAppDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetFunctionAppDraft(draftOrigin)
            else DotNetFunctionAppDraft(name, resourceGroupName, module as FunctionAppModule)

        return draft
    }

    private fun FunctionAppDeploymentSlotDraft.toDotNetFunctionAppDeploymentSlotDraft(): DotNetFunctionAppDeploymentSlotDraft {
        val draftOrigin = origin
        val draft =
            if (draftOrigin != null) DotNetFunctionAppDeploymentSlotDraft(draftOrigin)
            else DotNetFunctionAppDeploymentSlotDraft(name, module as FunctionAppDeploymentSlotModule)

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

    private fun getStorageAccountTask(): AzureTask<StorageAccount> =
        AzureTask<StorageAccount>(Callable {
            val storageAccountName = config.storageAccountName() ?: getDefaultStorageAccountName(config.appName())
            val storageResourceGroup = config.storageAccountResourceGroup() ?: config.resourceGroup()
            val accounts = Azure.az(AzureStorageAccount::class.java).accounts(config.subscriptionId())
            val existingAccount = accounts.get(storageAccountName, storageResourceGroup)
            if (existingAccount != null && existingAccount.exists()) {
                return@Callable existingAccount
            }

            with(accounts.create<StorageAccountDraft>(storageAccountName, storageResourceGroup)) {
                setRegion(getNonStageRegion(config.region()))
                setKind(Kind.STORAGE_V2)
                setRedundancy(Redundancy.STANDARD_LRS)
                commit()
            }
        })

    private fun getDefaultStorageAccountName(functionAppName: String): String {
        val context = ResourceManagerUtils.InternalRuntimeContext()
        return context.randomResourceName(functionAppName.replace(functionAppRegex, ""), 20)
    }

    private fun getNonStageRegion(region: Region): Region {
        val regionName = region.name
        if (!regionName.contains("stage", true)) {
            return region
        }

        return regionName.let {
            var name = StringUtils.removeIgnoreCase(it, "(stage)")
            name = StringUtils.removeIgnoreCase(name, "stage")
            name = name.trim()
            return@let Region.fromName(name)
        }
    }

    private fun getCreateFunctionAppTask(draft: DotNetFunctionAppDraft) =
        AzureTask("Create new app(${config.appName()}) on subscription(${config.subscriptionId()})",
            Callable {
                with(draft) {
                    appServicePlan = this@CreateOrUpdateDotNetFunctionAppTask.appServicePlan
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()
                    flexConsumptionConfiguration = config.flexConsumptionConfiguration()
                    storageAccount = this@CreateOrUpdateDotNetFunctionAppTask.storageAccount
                    appSettings = (config.appSettings() ?: mutableMapOf()).apply {

                        //Controls remote build behavior during deployment.
                        //see: https://learn.microsoft.com/en-us/azure/azure-functions/functions-app-settings#scm_do_build_during_deployment
                        if (config.pricingTier == PricingTier.CONSUMPTION && config.runtime.os == OperatingSystem.LINUX) {
                            put(SCM_DO_BUILD_DURING_DEPLOYMENT, "0")
                        }

                        //Enables your function app to run from a package file, which can be locally mounted or deployed to an external URL.
                        //see: https://learn.microsoft.com/en-us/azure/azure-functions/run-functions-from-deployment-package
                        if (config.runtime.os == OperatingSystem.WINDOWS ||
                            (config.runtime.os == OperatingSystem.LINUX && config.pricingTier != PricingTier.CONSUMPTION)
                        ) {
                            put(WEBSITE_RUN_FROM_PACKAGE, "1")
                        }

                        //Indicates whether to an app can use .NET 8 on the in-process model.
                        //see: https://learn.microsoft.com/en-us/azure/azure-functions/functions-dotnet-class-library?tabs=v4%2Ccmd#updating-to-target-net-8
                        if (config.dotnetRuntime?.functionStack?.runtime() == "DOTNET" &&
                            (config.dotnetRuntime?.stack?.version() == "8.0" || config.dotnetRuntime?.frameworkVersion?.toString() == "v8.0")
                        ) {
                            put(FUNCTIONS_INPROC_NET8_ENABLED, "1")
                        }
                    }

                    createIfNotExist()
                }
            })

    private fun getUpdateFunctionAppTask(draft: DotNetFunctionAppDraft) =
        AzureTask<FunctionApp>("Update function app(${config.appName()})",
            Callable {
                with(draft) {
                    appServicePlan = this@CreateOrUpdateDotNetFunctionAppTask.appServicePlan
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()
                    flexConsumptionConfiguration = config.flexConsumptionConfiguration()
                    storageAccount = this@CreateOrUpdateDotNetFunctionAppTask.storageAccount

                    updateIfExist()
                }
            })

    private fun getFunctionDeploymentSlot(functionApp: FunctionApp): DotNetFunctionAppDeploymentSlotDraft {
        if (!functionApp.exists()) {
            throw AzureToolkitRuntimeException("The Function App does not exist. Please make sure the Function App name is correct")
        }
        if (requireNotNull(functionApp.appServicePlan).pricingTier.isFlexConsumption) {
            throw AzureToolkitRuntimeException("Deployment slot is not supported for function app with consumption plan")
        }

        return functionApp
            .slots()
            .updateOrCreate<FunctionAppDeploymentSlotDraft>(config.deploymentSlotName(), config.resourceGroup())
            .toDotNetFunctionAppDeploymentSlotDraft()
    }

    private fun getCreateFunctionSlotTask(draft: DotNetFunctionAppDeploymentSlotDraft) =
        AzureTask("Create new slot(${config.deploymentSlotName()}) on function app (${config.appName()})",
            Callable {
                with(draft) {
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()
                    configurationSource = config.deploymentSlotConfigurationSource()
                    appSettings = config.appSettings()

                    commit()
                }
            })

    private fun getUpdateFunctionSlotTask(draft: DotNetFunctionAppDeploymentSlotDraft) =
        AzureTask("Update function deployment slot(${config.deploymentSlotName()})",
            Callable {
                with(draft) {
                    dotNetRuntime = getRuntime(config.dotnetRuntime)
                    dockerConfiguration = getDockerConfiguration(config.dotnetRuntime)
                    diagnosticConfig = config.diagnosticConfig()

                    commit()
                }
            })

    private fun getRuntime(runtimeConfig: DotNetRuntimeConfig?): DotNetRuntime? {
        if (runtimeConfig == null) return null
        if (runtimeConfig.isDocker) return null

        return DotNetRuntime(
            runtimeConfig.os(),
            runtimeConfig.stack,
            runtimeConfig.frameworkVersion,
            runtimeConfig.functionStack,
            false
        )
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

    override fun doExecute(): FunctionAppBase<*, *, *> {
        Operation.execute(AzureString.fromString("Creating or updating Function App"), {
            processHandlerMessager?.let { OperationContext.current().messager = it }

            for (task in subTasks) {
                task.body.call()
            }
        }, null)

        return requireNotNull(functionApp)
    }
}