/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.runners

import com.intellij.execution.configurations.RunProfile
import com.intellij.execution.executors.DefaultRunExecutor
import com.jetbrains.rider.debugger.DotNetProgramRunner
import com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.FunctionRunConfiguration

class FunctionProgramRunner : DotNetProgramRunner() {
    companion object {
        private const val RUNNER_ID = "azure-function-runner"
    }

    override fun getRunnerId() = RUNNER_ID

    override fun canRun(executorId: String, runConfiguration: RunProfile) =
        executorId == DefaultRunExecutor.EXECUTOR_ID && runConfiguration is FunctionRunConfiguration
}