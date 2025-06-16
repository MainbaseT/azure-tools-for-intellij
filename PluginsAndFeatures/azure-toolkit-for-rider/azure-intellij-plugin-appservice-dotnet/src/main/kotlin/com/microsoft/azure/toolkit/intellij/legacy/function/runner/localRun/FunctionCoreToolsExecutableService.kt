/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.ide.actions.OpenFileAction
import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.jetbrains.rider.azure.model.AzureFunctionWorkerModel
import com.jetbrains.rider.azure.model.AzureFunctionWorkerModelRequest
import com.jetbrains.rider.azure.model.functionAppDaemonModel
import com.jetbrains.rider.projectView.solution
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsManager
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionsVersionMsBuildService
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionsVersionMsBuildService.Companion.PROPERTY_AZURE_FUNCTIONS_VERSION
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.resolveFunctionCoreToolsExecutable
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettingsService
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionWorkerRuntime
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.getWorkerRuntime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.nio.file.Path
import kotlin.io.path.absolutePathString

@Service(Service.Level.PROJECT)
class FunctionCoreToolsExecutableService(private val project: Project) {
    companion object {
        fun getInstance(project: Project): FunctionCoreToolsExecutableService = project.service()
        private val LOG = logger<FunctionCoreToolsExecutableService>()
    }

    data class FunctionCoreToolsExecutable(
        val executablePath: Path,
        val functionsRuntimeVersion: String,
        val functionRuntime: FunctionWorkerRuntime,
        val localSettings: FunctionLocalSettings?
    )

    suspend fun getCoreToolsExecutable(
        projectFilePath: Path,
        projectTfm: String?
    ): FunctionCoreToolsExecutable? {
        val msBuildVersionProperty = withContext(Dispatchers.EDT) {
            FunctionsVersionMsBuildService
                .getInstance(project)
                .requestAzureFunctionsVersion(projectFilePath.absolutePathString())
        }
        if (msBuildVersionProperty == null) {
            LOG.warn("Could not determine project MSBuild property '${PROPERTY_AZURE_FUNCTIONS_VERSION}'")
            return null
        }
        LOG.debug { "Function version project property: $msBuildVersionProperty" }

        val functionLocalSettings = withContext(Dispatchers.Default) {
            FunctionLocalSettingsService
                .getInstance(project)
                .getFunctionLocalSettings(projectFilePath)
        }

        val workerRuntime = functionLocalSettings?.getWorkerRuntime()
            ?: getFunctionWorkerRuntimeFromBackendOrDefault(projectFilePath)
        LOG.debug { "Worker runtime: $workerRuntime" }

        val functionsRuntimeVersion = calculateFunctionsRuntimeVersion(msBuildVersionProperty, workerRuntime)
        LOG.debug { "Functions runtime version: $functionsRuntimeVersion" }

        val functionsTfm = if (workerRuntime == FunctionWorkerRuntime.DOTNET) projectTfm else null
        LOG.debug { "Functions target framework: $functionsTfm" }

        val functionCoreToolsPath = withContext(Dispatchers.Default) {
            withBackgroundProgress(project, "Getting Azure Functions core tools") {
                FunctionCoreToolsManager
                    .getInstance()
                    .getFunctionCoreToolsPathOrDownloadForVersion(functionsRuntimeVersion, functionsTfm)
            }
        }
        if (functionCoreToolsPath == null) {
            LOG.warn("Unable to find or download Function core tools for the project '$projectFilePath'")
            return null
        }

        val functionCoreToolsExecutablePath = functionCoreToolsPath.resolveFunctionCoreToolsExecutable()
        LOG.trace { "Function core tools executable path: $functionCoreToolsExecutablePath" }

        return FunctionCoreToolsExecutable(
            functionCoreToolsExecutablePath,
            functionsRuntimeVersion,
            workerRuntime,
            functionLocalSettings
        )
    }

    private suspend fun getFunctionWorkerRuntimeFromBackendOrDefault(projectFilePath: Path): FunctionWorkerRuntime {
        val functionWorkerModel = project.solution
            .functionAppDaemonModel
            .getAzureFunctionWorkerModel
            .startSuspending(AzureFunctionWorkerModelRequest(projectFilePath.absolutePathString()))

        return when (functionWorkerModel) {
            AzureFunctionWorkerModel.Default -> FunctionWorkerRuntime.DOTNET
            AzureFunctionWorkerModel.Isolated -> FunctionWorkerRuntime.DOTNET_ISOLATED
            AzureFunctionWorkerModel.Unknown -> {
                showNotificationAboutDefaultRuntime(projectFilePath)
                FunctionWorkerRuntime.DOTNET
            }
        }
    }

    private fun showNotificationAboutDefaultRuntime(projectPath: Path) {
        val settingsFilePath = FunctionLocalSettingsService.getInstance(project).getLocalSettingFilePath(projectPath)

        Notification(
            "Azure AppServices",
            "Unable to find the Function worker runtime",
            "Unable to find the `FUNCTIONS_WORKER_RUNTIME` variable in the `local.settings.json` file. The Default worker model will be applied",
            NotificationType.WARNING
        )
            .addAction(NotificationAction.createSimple("Open local.settings.json") {
                OpenFileAction.openFile(settingsFilePath.absolutePathString(), project)
            })
            .notify(project)
    }

    private fun calculateFunctionsRuntimeVersion(msBuildVersionProperty: String, workerRuntime: FunctionWorkerRuntime) =
        if (msBuildVersionProperty.equals("v0", ignoreCase = true)) msBuildVersionProperty
        else if (msBuildVersionProperty.equals("v1", ignoreCase = true)) msBuildVersionProperty
        else if (msBuildVersionProperty.equals("v2", ignoreCase = true)) msBuildVersionProperty
        else if (msBuildVersionProperty.equals("v3", ignoreCase = true)) msBuildVersionProperty
        else if (msBuildVersionProperty.equals("v4", ignoreCase = true)) {
            when (workerRuntime) {
                FunctionWorkerRuntime.DOTNET -> "v0"
                FunctionWorkerRuntime.DOTNET_ISOLATED -> "v4"
            }
        } else {
            error("Function runtime version not supported: $workerRuntime")
        }
}
