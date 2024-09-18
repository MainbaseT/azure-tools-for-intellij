/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.RunManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.rider.build.tasks.BuildProjectBeforeRunTask
import com.microsoft.azure.toolkit.intellij.legacy.function.buildTasks.BuildFunctionsProjectBeforeRunTaskProvider

class MigrateBuildProjectTaskActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        RunManager.getInstance(project).allSettings.forEach { setting ->
            if (setting.type.id == "AzureFunctionAppRun") {
                val buildProjectTasks = setting.configuration.beforeRunTasks.filter {
                    it.providerId == BuildFunctionsProjectBeforeRunTaskProvider.ID
                }
                if (buildProjectTasks.isNotEmpty()) {
                    buildProjectTasks.forEach {
                        setting.configuration.beforeRunTasks.remove(it)
                    }

                    val buildSolutionTask = BuildProjectBeforeRunTask()
                    buildSolutionTask.isEnabled = true
                    setting.configuration.beforeRunTasks.add(0, buildSolutionTask)
                }
            }
        }
    }
}