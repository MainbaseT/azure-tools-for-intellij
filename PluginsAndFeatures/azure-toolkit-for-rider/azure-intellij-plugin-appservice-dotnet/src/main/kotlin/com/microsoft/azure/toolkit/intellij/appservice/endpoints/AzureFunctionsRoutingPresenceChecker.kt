/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice.endpoints

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.jetbrains.rider.microservices.endpointsProviders.WebFrameworkChecker

@Service(Service.Level.PROJECT)
class AzureFunctionsRoutingPresenceChecker(project: Project) :
    WebFrameworkChecker(project, setOf("Microsoft.Azure.Functions.Worker.Extensions.Http"))