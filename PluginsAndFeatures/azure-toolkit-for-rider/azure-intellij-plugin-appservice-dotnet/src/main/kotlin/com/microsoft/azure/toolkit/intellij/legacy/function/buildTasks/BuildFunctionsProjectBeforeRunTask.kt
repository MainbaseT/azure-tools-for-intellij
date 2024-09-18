/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.buildTasks

import com.intellij.execution.BeforeRunTask
import com.intellij.execution.BeforeRunTaskProvider
import com.intellij.execution.configurations.RunConfiguration
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.Key
import com.jetbrains.rider.build.BuildHost
import com.jetbrains.rider.build.BuildParameters
import com.jetbrains.rider.build.tasks.BuildTaskThrottler
import com.jetbrains.rider.model.BuildTarget
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.FunctionRunConfiguration
import javax.swing.Icon

class BuildFunctionsProjectBeforeRunTask :
    BeforeRunTask<BuildFunctionsProjectBeforeRunTask>(BuildFunctionsProjectBeforeRunTaskProvider.ID)

class BuildFunctionsProjectBeforeRunTaskProvider : BeforeRunTaskProvider<BuildFunctionsProjectBeforeRunTask>() {
    companion object {
        val ID = Key.create<BuildFunctionsProjectBeforeRunTask>("BuildFunctionsProject")
    }

    override fun getId() = ID

    override fun getName() = "Build Functions Project (Deprecated)"

    override fun getDescription(task: BuildFunctionsProjectBeforeRunTask) = "Build Functions project"

    override fun isConfigurable() = false

    override fun getIcon(): Icon = AllIcons.Actions.Compile

    override fun createTask(runConfiguration: RunConfiguration): BuildFunctionsProjectBeforeRunTask? {
        if (runConfiguration !is FunctionRunConfiguration) return null

        return BuildFunctionsProjectBeforeRunTask().apply { isEnabled = false }
    }

    override fun canExecuteTask(configuration: RunConfiguration, task: BuildFunctionsProjectBeforeRunTask): Boolean {
        return BuildHost.getInstance(configuration.project).ready.value
    }

    override fun executeTask(
        context: DataContext,
        configuration: RunConfiguration,
        env: ExecutionEnvironment,
        task: BuildFunctionsProjectBeforeRunTask
    ): Boolean {
        val project = configuration.project
        val buildHost = BuildHost.getInstance(project)
        val selectedProjectsForBuild = when (configuration) {
            is FunctionRunConfiguration -> listOf(configuration.parameters.projectFilePath)
            else -> emptyList()
        }

        if (!buildHost.ready.value) return false

        val throttler = BuildTaskThrottler.getInstance(project)
        return throttler.buildSequentiallySync(BuildParameters(BuildTarget(), selectedProjectsForBuild)).msBuildStatus
    }
}