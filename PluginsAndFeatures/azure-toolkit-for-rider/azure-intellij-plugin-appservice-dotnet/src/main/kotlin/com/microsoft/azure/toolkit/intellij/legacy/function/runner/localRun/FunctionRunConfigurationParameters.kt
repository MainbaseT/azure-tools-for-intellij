package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.execution.configuration.EnvironmentVariablesComponent
import com.intellij.execution.configurations.RuntimeConfigurationError
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.JDOMExternalizerUtil
import com.jetbrains.rider.model.runnableProjectsModel
import com.jetbrains.rider.projectView.solution
import com.jetbrains.rider.run.RiderRunBundle
import com.jetbrains.rider.run.configurations.project.DotNetProjectConfigurationParameters
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import org.jdom.Element
import java.io.File


class FunctionRunConfigurationParameters(
    private val project: Project,
    var projectFilePath: String,
    var projectTfm: String,
    var profileName: String,
    var functionNames: String,
    var trackArguments: Boolean,
    var arguments: String,
    var trackWorkingDirectory: Boolean,
    var workingDirectory: String,
    var trackEnvs: Boolean,
    var envs: Map<String, String>,
    var useExternalConsole: Boolean,
    var trackUrl: Boolean,
    var startBrowserParameters: DotNetStartBrowserParameters
) {
    companion object {
        private const val PROJECT_FILE_PATH = "PROJECT_FILE_PATH"
        private const val LAUNCH_PROFILE_NAME = "LAUNCH_PROFILE_NAME"
        private const val PROJECT_TFM = "PROJECT_TFM"
        private const val FUNCTION_NAMES = "FUNCTION_NAMES"
        private const val PROGRAM_PARAMETERS = "PROGRAM_PARAMETERS"
        private const val ARGUMENTS_TRACKING = "PROJECT_ARGUMENTS_TRACKING"
        private const val WORKING_DIRECTORY = "WORKING_DIRECTORY"
        private const val WORKING_DIRECTORY_TRACKING = "PROJECT_WORKING_DIRECTORY_TRACKING"
        private const val TRACK_ENVS = "TRACK_ENVS"
        private const val TRACK_URL = "TRACK_URL"
        private const val USE_EXTERNAL_CONSOLE = "USE_EXTERNAL_CONSOLE"

        fun createDefault(project: Project) = FunctionRunConfigurationParameters(
            project,
            "",
            "",
            "",
            "",
            true,
            "",
            true,
            "",
            true,
            hashMapOf(),
            false,
            true,
            DotNetStartBrowserParameters()
        )
    }

    fun validate() {
        if (project.solution.isLoaded.valueOrNull != true) {
            throw RuntimeConfigurationError("Solution is loading, please wait for a few seconds.")
        }

        val runnableProjects = project.solution.runnableProjectsModel.projects.valueOrNull
        if (project.solution.isLoaded.valueOrNull != true || runnableProjects == null) {
            throw RuntimeConfigurationError(DotNetProjectConfigurationParameters.SOLUTION_IS_LOADING)
        }
        val project = runnableProjects.singleOrNull {
            it.projectFilePath == projectFilePath && it.kind == AzureRunnableProjectKinds.AzureFunctions
        } ?: throw RuntimeConfigurationError(DotNetProjectConfigurationParameters.PROJECT_NOT_SPECIFIED)
        if (!trackWorkingDirectory) {
            val workingDirectoryFile = File(workingDirectory)
            if (!workingDirectoryFile.exists() || !workingDirectoryFile.isDirectory)
                throw RuntimeConfigurationError(
                    RiderRunBundle.message(
                        "dialog.message.invalid.working.dir",
                        workingDirectory.ifEmpty { "<empty>" }
                    )
                )
        }
        if (!project.problems.isNullOrEmpty()) {
            throw RuntimeConfigurationError(project.problems)
        }
    }

    fun readExternal(element: Element) {
        projectFilePath = JDOMExternalizerUtil.readField(element, PROJECT_FILE_PATH) ?: ""
        profileName = JDOMExternalizerUtil.readField(element, LAUNCH_PROFILE_NAME) ?: ""
        projectTfm = JDOMExternalizerUtil.readField(element, PROJECT_TFM) ?: ""
        functionNames = JDOMExternalizerUtil.readField(element, FUNCTION_NAMES) ?: ""
        val trackArgumentsString = JDOMExternalizerUtil.readField(element, ARGUMENTS_TRACKING) ?: ""
        trackArguments = trackArgumentsString != "0"
        arguments = JDOMExternalizerUtil.readField(element, PROGRAM_PARAMETERS) ?: ""
        val trackWorkingDirectoryString = JDOMExternalizerUtil.readField(element, WORKING_DIRECTORY_TRACKING) ?: ""
        trackWorkingDirectory = trackWorkingDirectoryString != "0"
        workingDirectory = JDOMExternalizerUtil.readField(element, WORKING_DIRECTORY) ?: ""
        val trackEnvsString = JDOMExternalizerUtil.readField(element, TRACK_ENVS) ?: ""
        trackEnvs = trackEnvsString != "0"
        EnvironmentVariablesComponent.readExternal(element, envs)
        val useExternalConsoleString = JDOMExternalizerUtil.readField(element, USE_EXTERNAL_CONSOLE) ?: ""
        useExternalConsole = useExternalConsoleString == "1"
        val trackUrlString = JDOMExternalizerUtil.readField(element, TRACK_URL) ?: ""
        trackUrl = trackUrlString != "0"
        startBrowserParameters = DotNetStartBrowserParameters.readExternal(element)
    }

    fun writeExternal(element: Element) {
        JDOMExternalizerUtil.writeField(element, PROJECT_FILE_PATH, projectFilePath)
        JDOMExternalizerUtil.writeField(element, LAUNCH_PROFILE_NAME, profileName)
        JDOMExternalizerUtil.writeField(element, PROJECT_TFM, projectTfm)
        JDOMExternalizerUtil.writeField(element, FUNCTION_NAMES, functionNames)
        JDOMExternalizerUtil.writeField(element, ARGUMENTS_TRACKING, if (trackArguments) "1" else "0")
        JDOMExternalizerUtil.writeField(element, PROGRAM_PARAMETERS, arguments)
        JDOMExternalizerUtil.writeField(element, WORKING_DIRECTORY_TRACKING, if (trackWorkingDirectory) "1" else "0")
        JDOMExternalizerUtil.writeField(element, WORKING_DIRECTORY, workingDirectory)
        JDOMExternalizerUtil.writeField(element, TRACK_ENVS, if (trackEnvs) "1" else "0")
        EnvironmentVariablesComponent.writeExternal(element, envs)
        JDOMExternalizerUtil.writeField(element, USE_EXTERNAL_CONSOLE, if (useExternalConsole) "1" else "0")
        JDOMExternalizerUtil.writeField(element, TRACK_URL, if (trackUrl) "1" else "0")
        startBrowserParameters.writeExternal(element)
    }

    fun copy() = FunctionRunConfigurationParameters(
        project,
        projectFilePath,
        projectTfm,
        profileName,
        functionNames,
        trackArguments,
        arguments,
        trackWorkingDirectory,
        workingDirectory,
        trackEnvs,
        envs,
        useExternalConsole,
        trackUrl,
        startBrowserParameters.copy()
    )
}