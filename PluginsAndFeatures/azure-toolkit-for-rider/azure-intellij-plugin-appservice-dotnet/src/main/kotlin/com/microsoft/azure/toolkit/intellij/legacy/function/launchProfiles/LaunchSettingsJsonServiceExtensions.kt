/*
 * Copyright 2018-2025 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles

import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.run.configurations.controls.LaunchProfile
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJsonService
import kotlin.collections.asSequence
import kotlin.collections.component1
import kotlin.collections.component2

fun LaunchSettingsJsonService.getFirstOrNullLaunchProfileProfile(runnableProject: RunnableProject): LaunchProfile? {
    val profiles = loadLaunchSettings(runnableProject)?.profiles ?: return null

    return profiles
        .asSequence()
        .filter { it.value.commandName.equals("Project", true) }
        .firstOrNull()
        ?.let { LaunchProfile(it.key, it.value) }
}

suspend fun LaunchSettingsJsonService.getFirstOrNullLaunchProfileProfileSuspend(runnableProject: RunnableProject): LaunchProfile? {
    val profiles = loadLaunchSettingsSuspend(runnableProject)?.profiles ?: return null

    return profiles
        .asSequence()
        .filter { it.value.commandName.equals("Project", true) }
        .firstOrNull()
        ?.let { LaunchProfile(it.key, it.value) }
}

suspend fun LaunchSettingsJsonService.getProjectLaunchProfiles(runnableProject: RunnableProject): List<LaunchProfile> {
    val profiles = loadLaunchSettingsSuspend(runnableProject)?.profiles ?: return emptyList()

    return profiles
        .asSequence()
        .filter { it.value.commandName.equals("Project", true) }
        .map { (name, content) -> LaunchProfile(name, content) }
        .sortedBy { it.name }
        .toList()
}

suspend fun LaunchSettingsJsonService.getProjectLaunchProfileByName(
    runnableProject: RunnableProject,
    launchProfileName: String?
): LaunchProfile? {
    val profiles = loadLaunchSettingsSuspend(runnableProject)?.profiles ?: return null

    return profiles
        .asSequence()
        .filter { it.value.commandName.equals("Project", true) }
        .firstOrNull { it.key == launchProfileName }
        ?.let { (name, content) -> LaunchProfile(name, content) }
}