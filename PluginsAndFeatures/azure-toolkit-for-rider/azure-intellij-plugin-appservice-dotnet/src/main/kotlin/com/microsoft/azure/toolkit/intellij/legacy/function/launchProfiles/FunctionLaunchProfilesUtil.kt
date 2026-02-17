/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles

import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rider.model.ProjectOutput
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJson
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettings
import org.apache.http.client.utils.URIBuilder

internal fun getArguments(profile: LaunchSettingsJson.Profile?, projectOutput: ProjectOutput?) = buildString {
    val defaultArguments = projectOutput?.defaultArguments
    val profileArgs = profile?.commandLineArgs

    if (!defaultArguments.isNullOrEmpty()) {
        append(ParametersListUtil.join(defaultArguments))
        append(" ")
    }

    if (!profileArgs.isNullOrEmpty()) {
        append(profileArgs)
    }
}

fun getWorkingDirectory(profile: LaunchSettingsJson.Profile?, projectOutput: ProjectOutput?): String {
    return profile?.workingDirectory ?: projectOutput?.workingDirectory ?: ""
}

internal fun getEnvironmentVariables(profile: LaunchSettingsJson.Profile?) = buildMap {
    profile?.environmentVariables?.forEach { pair ->
        pair.value?.let { put(pair.key, it) }
    }
}

private val portRegex = Regex("(--port|-p) (\\d+)", RegexOption.IGNORE_CASE)

internal fun getApplicationUrl(
    profile: LaunchSettingsJson.Profile?,
    projectOutput: ProjectOutput?,
    functionLocalSettings: FunctionLocalSettings?
): String {
    val arguments = getArguments(profile, projectOutput)
    return getApplicationUrl(profile, arguments, functionLocalSettings)
}

fun getApplicationUrl(
    profile: LaunchSettingsJson.Profile?,
    arguments: String,
    functionLocalSettings: FunctionLocalSettings?
): String {
    val applicationUrl = profile?.applicationUrl
    if (!applicationUrl.isNullOrEmpty()) {
        return applicationUrl.substringBefore(';')
    }

    if (arguments.isNotEmpty()) {
        val argumentPort = portRegex.find(arguments)?.groupValues?.getOrNull(2)?.toIntOrNull()
        if (argumentPort != null) {
            return URIBuilder("http://localhost").setPort(argumentPort).build().toString()
        }
    }

    val localHttpPort = functionLocalSettings?.host?.localHttpPort
    if (localHttpPort != null) {
        return URIBuilder("http://localhost").setPort(localHttpPort).build().toString()
    }

    return ""
}

internal fun getLaunchBrowserFlag(profile: LaunchSettingsJson.Profile?): Boolean {
    val launchBrowser = profile?.launchBrowser
    return launchBrowser == true
}