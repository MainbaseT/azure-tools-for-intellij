package com.microsoft.azure.toolkit.intellij.legacy.function.runner.deploy

import com.intellij.execution.Executor
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.project.Project
import com.microsoft.azure.toolkit.intellij.legacy.common.RiderAzureRunConfigurationBase

class FunctionDeploymentConfiguration(private val project: Project, factory: ConfigurationFactory, name: String?) :
        RiderAzureRunConfigurationBase<FunctionDeployModel>(project, factory, name) {

    private val functionDeploymentModel = FunctionDeployModel()

    override fun getModel() = functionDeploymentModel

    override fun validate() {
    }

    override fun getState(executor: Executor, environment: ExecutionEnvironment) =
            FunctionDeploymentState(project, this)

    override fun getConfigurationEditor() = FunctionDeploymentEditor(project)
}