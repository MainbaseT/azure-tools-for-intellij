/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalCoroutinesApi::class)

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.CantRunException
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.runners.ExecutionEnvironment
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.run.dotNetCore.DotNetCoreDebugProfile
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime
import com.jetbrains.rider.runtime.RiderDotNetActiveRuntimeHost
import com.jetbrains.rider.runtime.dotNetCore.DotNetCoreRuntimeType
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsInfo
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionWorkerRuntime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlin.io.path.absolutePathString

class FunctionNetCoreRuntime(
    private val coreToolsInfo: FunctionCoreToolsInfo,
    private val workerRuntime: FunctionWorkerRuntime,
    private val lifetime: Lifetime
) : DotNetRuntime(DotNetCoreRuntimeType) {

    override fun patchRunCommandLine(commandLine: GeneralCommandLine, runtimeArguments: List<String>) {
        if (commandLine.exePath.endsWith(".dll", true)) {
            val exePath = commandLine.exePath
            if (commandLine.parametersList.parametersCount > 0 && commandLine.parametersList[0] != exePath) {
                commandLine.parametersList.prepend(exePath)
            }
            commandLine.exePath = coreToolsInfo.coreToolsExecutable.absolutePathString()
        }
    }

    override fun createRunState(
        dotNetExecutable: DotNetExecutable,
        executionEnvironment: ExecutionEnvironment
    ) = FunctionRunProfileState(dotNetExecutable, this, executionEnvironment)

    override fun createDebugState(
        dotNetExecutable: DotNetExecutable,
        executionEnvironment: ExecutionEnvironment
    ) = when (workerRuntime) {
        FunctionWorkerRuntime.DOTNET -> createDebugStateForDefaultFunctionRuntime(
            executionEnvironment,
            dotNetExecutable
        )

        FunctionWorkerRuntime.DOTNET_ISOLATED -> createDebugStateForIsolatedFunctionRuntime(
            dotNetExecutable,
        )
    }

    private fun createDebugStateForDefaultFunctionRuntime(
        executionEnvironment: ExecutionEnvironment,
        dotNetExecutable: DotNetExecutable
    ): DotNetCoreDebugProfile {
        val activeDotnetRuntime = RiderDotNetActiveRuntimeHost
            .getInstance(executionEnvironment.project)
            .dotNetCoreRuntime
            .value
        if (activeDotnetRuntime == null) throw CantRunException("Unable to get active .NET runtime")

        return DotNetCoreDebugProfile(
            activeDotnetRuntime,
            dotNetExecutable,
            executionEnvironment,
            coreToolsInfo.coreToolsExecutable.absolutePathString()
        )
    }

    private fun createDebugStateForIsolatedFunctionRuntime(dotNetExecutable: DotNetExecutable) =
        FunctionIsolatedDebugProfileState(
            dotNetExecutable,
            this,
            lifetime
        )
}