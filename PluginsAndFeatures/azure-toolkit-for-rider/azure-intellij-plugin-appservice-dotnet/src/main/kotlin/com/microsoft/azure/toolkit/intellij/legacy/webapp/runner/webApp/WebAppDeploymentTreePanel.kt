/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.webapp.runner.webApp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentTreePanel
import com.microsoft.azure.toolkit.intellij.appservice.deployment.AppServiceDeploymentViewModel
import com.microsoft.azure.toolkit.lib.appservice.config.AppServiceConfig
import com.microsoft.azure.toolkit.lib.common.action.Action

internal class WebAppDeploymentTreePanel(private val project: Project, vm: AppServiceDeploymentViewModel<AppServiceConfig>) :
    AppServiceDeploymentTreePanel<AppServiceConfig>(
        vm,
        "Search web apps...",
        "No web apps found",
        { vm, panel ->
            val dialog = WebAppCreationDialog(project, false) //TODO: targetProjectOnNetFramework
            Disposer.register(panel, dialog)
            dialog.setOkAction(
                Action<AppServiceConfig>(Action.Id.of("user/webapp.create_app.app"))
                    .withLabel("Create")
                    .withIdParam(AppServiceConfig::appName)
                    .withSource { it }
                    .withAuthRequired(false)
                    .withHandler { config -> vm.addDraftAppService(config) }
            )
            //TODO: dialog.data = FunctionAppConfigProducer.getInstance().generateDefaultConfig()
            dialog.show()
        }
    )