/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.project.Project
import com.jetbrains.rider.run.configurations.IAutoSelectableRunConfiguration
import com.jetbrains.rider.run.configurations.IProjectBasedRunConfiguration
import com.jetbrains.rider.run.configurations.RiderAsyncRunConfiguration
import org.jdom.Element

class FunctionRunConfiguration(
    project: Project,
    factory: ConfigurationFactory,
    name: String?,
    val parameters: FunctionRunConfigurationParameters
) : RiderAsyncRunConfiguration(
    name ?: "Azure Functions",
    project,
    factory,
    { FunctionRunSettingsEditor(it) },
    FunctionRunExecutorFactory(project, parameters)
), IProjectBasedRunConfiguration, IAutoSelectableRunConfiguration {

    override fun checkConfiguration() {
        super.checkConfiguration()
        parameters.validate()
    }

    override fun readExternal(element: Element) {
        super.readExternal(element)
        parameters.readExternal(element)
    }

    override fun writeExternal(element: Element) {
        super.writeExternal(element)
        parameters.writeExternal(element)
    }

    override fun clone(): FunctionRunConfiguration {
        val newConfiguration = FunctionRunConfiguration(
            project,
            requireNotNull(factory),
            name,
            parameters.copy()
        )
        newConfiguration.doCopyOptionsFrom(this)
        return newConfiguration
    }

    override fun getAutoSelectPriority() = 10

    override fun getProjectFilePath() = parameters.projectFilePath

    override fun setProjectFilePath(path: String) {
        parameters.projectFilePath = path
    }
}