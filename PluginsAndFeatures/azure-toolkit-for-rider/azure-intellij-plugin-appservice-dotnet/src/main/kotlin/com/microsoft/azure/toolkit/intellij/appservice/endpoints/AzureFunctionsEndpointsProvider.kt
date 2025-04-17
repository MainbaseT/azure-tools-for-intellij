/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.endpoints

import com.intellij.microservices.endpoints.FrameworkPresentation
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.jetbrains.rider.microservices.endpointsProviders.RiderHttpEndpointsProvider
import com.jetbrains.rider.microservices.endpointsProviders.WebFrameworkChecker
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.debugger.asIntellij

class AzureFunctionsEndpointsProvider : RiderHttpEndpointsProvider() {
    override val presentation: FrameworkPresentation
        get() = FrameworkPresentation(
            "Azure-Function",
            "Azure Function",
            AzureIcons.FunctionApp.MODULE.asIntellij()
        )
    override val endpointsProviderName: String
        get() = "AzureFunctions"

    override fun getWebFrameworkChecker(project: Project): WebFrameworkChecker {
        return project.service<AzureFunctionsRoutingPresenceChecker>()
    }
}

