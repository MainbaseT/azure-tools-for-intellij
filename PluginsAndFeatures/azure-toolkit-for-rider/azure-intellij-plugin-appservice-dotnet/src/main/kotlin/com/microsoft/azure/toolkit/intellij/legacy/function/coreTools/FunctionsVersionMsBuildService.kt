/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.util.concurrency.annotations.RequiresEdt
import com.jetbrains.rider.azure.model.AzureFunctionsVersionRequest
import com.jetbrains.rider.azure.model.functionAppDaemonModel
import com.jetbrains.rider.projectView.solution

@Service(Service.Level.PROJECT)
class FunctionsVersionMsBuildService(private val project: Project) {
    companion object {
        fun getInstance(project: Project) = project.service<FunctionsVersionMsBuildService>()
        const val PROPERTY_AZURE_FUNCTIONS_VERSION = "AzureFunctionsVersion"
    }

    /**
     * Requests the version of Azure Functions runtime for a given project.
     *
     * @param projectFilePath The file path of the project for which the Azure Functions version is requested.
     *
     * @return The value of `AzureFunctionsVersion` MSBuild property.
     */
    @RequiresEdt
    suspend fun requestAzureFunctionsVersion(projectFilePath: String) =
        project.solution
            .functionAppDaemonModel
            .getAzureFunctionsVersion
            .startSuspending(AzureFunctionsVersionRequest(projectFilePath))
}