/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun.profileStates

import com.intellij.execution.CantRunException
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.util.execution.ParametersListUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.debugger.DebuggerHelperHost
import com.jetbrains.rider.model.DesktopClrRuntime
import com.jetbrains.rider.run.dotNetCore.DotNetCoreAttachProfileState
import com.jetbrains.rider.run.dotNetCore.toCPUKind
import com.jetbrains.rider.run.msNet.MsNetAttachProfileState
import com.jetbrains.rider.runtime.DotNetExecutable
import com.jetbrains.rider.runtime.DotNetRuntime

class FunctionIsolatedDebugProfileState(
    private val dotNetExecutable: DotNetExecutable,
    dotNetRuntime: DotNetRuntime,
    private val lifetime: Lifetime,
) : FunctionIsolatedBaseDebugProfileState(dotNetExecutable, dotNetRuntime) {

    override suspend fun checkBeforeExecution() {
        dotNetExecutable.validate()
    }

    override suspend fun prepareExecution(environment: ExecutionEnvironment) {
        val (executionResult, pid) = launchFunctionHostWaitingForDebugger(environment)
            ?: throw CantRunException("Unable to obtain isolated worker process ID")

        functionHostExecutionResult = executionResult

        val targetProcess = getTargetProcess(pid)
            ?: throw CantRunException("Unable to find process isolated worker process with ID $pid")

        val processArchitecture = getPlatformArchitecture(lifetime, pid, environment.project)
        val processExecutablePath = ParametersListUtil.parse(targetProcess.commandLine).firstOrNull()
        val processTargetFramework = processExecutablePath?.let {
            DebuggerHelperHost.Companion
                .getInstance(environment.project)
                .getAssemblyTargetFramework(it, lifetime)
        }

        val isNetFrameworkProcess =
            processExecutablePath?.endsWith("dotnet.exe") == false && (processTargetFramework?.isNetFramework ?: false)

        wrappedState =
            if (isNetFrameworkProcess) {
                MsNetAttachProfileState(
                    targetProcess,
                    processArchitecture.toCPUKind(),
                    DesktopClrRuntime(""),
                    environment
                )
            } else {
                DotNetCoreAttachProfileState(
                    targetProcess,
                    environment,
                    processArchitecture
                )
            }
    }
}