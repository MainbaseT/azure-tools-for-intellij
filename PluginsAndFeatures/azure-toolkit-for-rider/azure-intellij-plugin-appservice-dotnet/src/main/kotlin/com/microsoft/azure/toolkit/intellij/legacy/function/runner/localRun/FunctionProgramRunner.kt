/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.configurations.RunProfileState
import com.intellij.execution.configurations.RunnerSettings
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.showRunContent
import com.intellij.execution.ui.RunContentDescriptor
import com.jetbrains.rider.debugger.DotNetRunnerBase
import com.jetbrains.rider.run.RiderAsyncProgramRunner

class FunctionProgramRunner : RiderAsyncProgramRunner<RunnerSettings>(), DotNetRunnerBase {
    companion object {
        private const val RUNNER_ID = "azure-function-runner"
    }

    override fun getRunnerId() = RUNNER_ID

    override fun canRun(executorId: String, runConfiguration: RunProfile) =
        executorId == DefaultRunExecutor.EXECUTOR_ID && runConfiguration is FunctionRunConfiguration

    override suspend fun executeAsync(
        environment: ExecutionEnvironment,
        state: RunProfileState
    ): RunContentDescriptor? {
        val executionResult = state.execute(environment.executor, this)
        return showRunContent(executionResult, environment)
    }
}