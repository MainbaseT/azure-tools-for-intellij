/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.executors.DefaultDebugExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.ui.RunContentDescriptor
import com.jetbrains.rider.debugger.DotNetDebugRunner
import com.jetbrains.rider.run.configurations.RequiresPreparationRunProfileState

class FunctionDebugRunner : DotNetDebugRunner() {
    companion object {
        private const val RUNNER_ID = "azure-function-debug-runner"
    }

    override fun getRunnerId() = RUNNER_ID

    override fun canRun(executorId: String, runConfiguration: RunProfile) =
        executorId == DefaultDebugExecutor.EXECUTOR_ID && runConfiguration is FunctionRunConfiguration

    override suspend fun executeAsync(
        environment: ExecutionEnvironment,
        state: RunProfileState
    ): RunContentDescriptor? {
        if (state is RequiresPreparationRunProfileState) {
            state.prepareExecution(environment)
        }
        return super.executeAsync(environment, state)
    }
}