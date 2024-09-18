/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.openapi.rd.util.lifetime
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.util.application
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rider.run.configurations.runnableProjectsModelIfAvailable
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.FunctionLaunchProfilesService
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettingsService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class FunctionWarmupStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        withContext(Dispatchers.EDT) {
            project.runnableProjectsModelIfAvailable?.projects?.adviseOnce(project.lifetime) {
                application.runReadAction {
                    FunctionLaunchProfilesService.getInstance(project).initialize(it)
                }
                FunctionLocalSettingsService.getInstance(project).initialize(it)
            }
        }
    }
}