/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice

import com.azure.resourcemanager.appservice.models.FunctionRuntimeStack
import com.azure.resourcemanager.appservice.models.OperatingSystem
import com.azure.resourcemanager.appservice.models.RuntimeStack
import com.azure.resourcemanager.appservice.models.WebAppBase
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase

private const val FUNCTIONS_WORKER_RUNTIME = "FUNCTIONS_WORKER_RUNTIME"
private const val FUNCTIONS_EXTENSION_VERSION = "FUNCTIONS_EXTENSION_VERSION"

fun WebAppBase.getDotNetRuntime(): DotNetRuntime {
    val os = operatingSystem()
    if (os == OperatingSystem.LINUX) {
        val linuxFxVersion = linuxFxVersion()
        if (linuxFxVersion.startsWith("docker", true)) {
            return DotNetRuntime(
                com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.LINUX,
                null,
                null,
                null,
                true
            )
        } else {
            if (appSettings.containsKey(FUNCTIONS_WORKER_RUNTIME)) {
                val runtime = requireNotNull(appSettings[FUNCTIONS_WORKER_RUNTIME]).value()
                val version = requireNotNull(appSettings[FUNCTIONS_EXTENSION_VERSION]).value()
                return DotNetRuntime(
                    com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.LINUX,
                    null,
                    null,
                    FunctionRuntimeStack(runtime, version, linuxFxVersion()),
                    false
                )
            } else {
                val stack = linuxFxVersion.substringBefore('|', "DOTNETCORE")
                val version = linuxFxVersion.substringAfter('|', "8.0")
                return DotNetRuntime(
                    com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.LINUX,
                    RuntimeStack(stack, version),
                    null,
                    null,
                    false
                )
            }
        }
    } else {
        if (appSettings.containsKey(FUNCTIONS_WORKER_RUNTIME)) {
            val runtime = requireNotNull(appSettings[FUNCTIONS_WORKER_RUNTIME]).value()
            val version = requireNotNull(appSettings[FUNCTIONS_EXTENSION_VERSION]).value()
            return DotNetRuntime(
                com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.WINDOWS,
                null,
                null,
                FunctionRuntimeStack(runtime, version, linuxFxVersion()),
                false
            )
        } else {
            return DotNetRuntime(
                com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem.WINDOWS,
                null,
                netFrameworkVersion(),
                null,
                false
            )
        }
    }
}

fun AppServiceAppBase<*, *, *>.getDotNetRuntime() = remote?.getDotNetRuntime()