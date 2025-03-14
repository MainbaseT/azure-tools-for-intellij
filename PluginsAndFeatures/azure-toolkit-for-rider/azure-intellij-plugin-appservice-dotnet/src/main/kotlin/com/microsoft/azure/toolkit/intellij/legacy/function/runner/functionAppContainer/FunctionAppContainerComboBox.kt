/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("DuplicatedCode")

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionAppContainer

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.Row
import com.microsoft.azure.toolkit.intellij.appservice.components.AppServiceComboBoxDotNetRender
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.functionApp.FunctionAppComboBox
import com.microsoft.azure.toolkit.lib.Azure
import com.microsoft.azure.toolkit.lib.appservice.config.FunctionAppConfig
import com.microsoft.azure.toolkit.lib.appservice.function.AzureFunctions
import com.microsoft.azure.toolkit.lib.auth.AzureAccount
import com.microsoft.azure.toolkit.lib.common.action.Action
import java.util.stream.Collectors

class FunctionAppContainerComboBox(project: Project) : FunctionAppComboBox(project) {
    init {
        setRenderer(AppServiceComboBoxDotNetRender())
    }

    override fun loadAppServiceModels(): MutableList<FunctionAppConfig> {
        val account = Azure.az(AzureAccount::class.java).account()
        if (!account.isLoggedIn) {
            return mutableListOf()
        }

        return Azure.az(AzureFunctions::class.java)
            .functionApps()
            .parallelStream()
            .filter { a -> a.runtime != null && a.runtime?.isWindows == false }
            .map { functionApp -> convertAppServiceToConfig({ FunctionAppConfig() }, functionApp) }
            .filter { a -> a.subscriptionId != null }
            .sorted { a, b -> a.appName().compareTo(b.appName(), true) }
            .collect(Collectors.toList())
    }

    override fun createResource() {
        val dialog = FunctionAppContainerCreationDialog(project)
        Disposer.register(this, dialog)
        val actionId: Action.Id<FunctionAppConfig> = Action.Id.of("user/function.create_app.app")
        dialog.setOkAction(Action(actionId)
            .withLabel("Create")
            .withIdParam(FunctionAppConfig::appName)
            .withSource { it }
            .withAuthRequired(false)
            .withHandler(this::setValue)
        )
        dialog.show()
    }
}

fun Row.functionAppContainerComboBox(project: Project): Cell<FunctionAppContainerComboBox> {
    val component = FunctionAppContainerComboBox(project)
    return cell(component)
}