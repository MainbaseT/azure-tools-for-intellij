/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentTreePanel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentViewModel
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.common.action.Action

internal class FunctionAppDeploymentTreePanel(private val project: Project, vm: AppServiceDeploymentViewModel<FunctionAppConfig>) :
    AppServiceDeploymentTreePanel<FunctionAppConfig>(
        vm,
        "Search function apps...",
        "No function apps found",
        { vm, panel ->
            val dialog = FunctionAppCreationDialog(project, false) //TODO: targetProjectOnNetFramework
            Disposer.register(panel, dialog)
            dialog.setOkAction(
                Action<FunctionAppConfig>(Action.Id.of("user/function.create_app.app"))
                    .withLabel("Create")
                    .withIdParam(FunctionAppConfig::appName)
                    .withSource { it }
                    .withAuthRequired(false)
                    .withHandler { config -> vm.addDraftAppService(config) }
            )
            //TODO: dialog.data = FunctionAppConfigProducer.getInstance().generateDefaultConfig()
            dialog.show()
        }
    ) {
}