/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.configurations.ConfigurationType
import com.intellij.openapi.project.Project
import com.jetbrains.rider.run.configurations.DotNetConfigurationFactoryBase

class FunctionRunConfigurationFactory(type: ConfigurationType) :
    DotNetConfigurationFactoryBase<FunctionRunConfiguration>(type) {
    companion object {
        private const val FACTORY_ID = "Azure - Run Function"
    }

    override fun getId() = FACTORY_ID

    override fun createTemplateConfiguration(project: Project) =
        FunctionRunConfiguration(
            project,
            this,
            project.name,
            FunctionRunConfigurationParameters.createDefault(project)
        )
}