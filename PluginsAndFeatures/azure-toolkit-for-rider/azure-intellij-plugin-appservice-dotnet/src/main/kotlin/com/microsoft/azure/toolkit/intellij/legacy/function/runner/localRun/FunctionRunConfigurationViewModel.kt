/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.openapi.project.Project
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rd.util.reactive.adviseOnce
import com.jetbrains.rider.model.ProjectOutput
import com.jetbrains.rider.model.RunnableProject
import com.jetbrains.rider.model.RunnableProjectsModel
import com.jetbrains.rider.run.configurations.RunnableProjectKinds
import com.jetbrains.rider.run.configurations.controls.*
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettings
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettingsEditor
import com.jetbrains.rider.run.configurations.launchSettings.LaunchSettingsJson
import com.jetbrains.rider.run.configurations.project.DotNetStartBrowserParameters
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.*
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getApplicationUrl
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getArguments
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getEnvironmentVariables
import com.microsoft.azure.toolkit.intellij.legacy.function.launchProfiles.getWorkingDirectory
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionLocalSettingsService
import com.microsoft.azure.toolkit.intellij.legacy.function.localsettings.FunctionWorkerRuntime
import java.io.File

class FunctionRunConfigurationViewModel(
    private val project: Project,
    private val lifetime: Lifetime,
    private val runnableProjectsModel: RunnableProjectsModel?,
    val projectSelector: ProjectSelector,
    val tfmSelector: StringSelector,
    val launchProfileSelector: LaunchProfileSelector,
    val programParametersEditor: ProgramParametersEditor,
    val workingDirectorySelector: PathSelector,
    val functionNamesEditor: TextEditor,
    val environmentVariablesEditor: EnvironmentVariablesEditor,
    val useExternalConsoleEditor: FlagEditor,
    separator: ViewSeparator,
    val urlEditor: TextEditor,
    val dotNetBrowserSettingsEditor: BrowserSettingsEditor
) : RunConfigurationViewModelBase() {
    override val controls: List<ControlBase> =
        listOf(
            projectSelector,
            tfmSelector,
            launchProfileSelector,
            programParametersEditor,
            workingDirectorySelector,
            functionNamesEditor,
            environmentVariablesEditor,
            useExternalConsoleEditor,
            separator,
            urlEditor,
            dotNetBrowserSettingsEditor
        )

    private var isLoaded = false

    var trackArguments = true
    var trackWorkingDirectory = true
    var trackEnvs = true
    var trackUrl = true

    private var functionLocalSettings: FunctionLocalSettings? = null

    init {
        disable()

        if (runnableProjectsModel != null) {
            projectSelector.bindTo(
                runnableProjectsModel,
                lifetime,
                { p -> p.kind == AzureRunnableProjectKinds.AzureFunctions },
                ::enable,
                ::handleProjectSelection
            )
        }

        tfmSelector.string.advise(lifetime) { handleChangeTfmSelection() }
        launchProfileSelector.profile.advise(lifetime) { handleProfileSelection() }
        programParametersEditor.parametersString.advise(lifetime) { handleArgumentsChange() }
        workingDirectorySelector.path.advise(lifetime) { handleWorkingDirectoryChange() }
        environmentVariablesEditor.envs.advise(lifetime) { handleEnvValueChange() }
        urlEditor.text.advise(lifetime) { handleUrlValueChange() }
    }

    private fun handleProjectSelection(runnableProject: RunnableProject) {
        if (!isLoaded) return

        reloadTfmSelector(runnableProject)
        reloadLaunchProfileSelector(runnableProject)
        readLocalSettingsForProject(runnableProject)

        val projectOutput = getSelectedProjectOutput()
        val launchProfile = launchProfileSelector.profile.valueOrNull

        recalculateFields(projectOutput, launchProfile?.content)
    }

    private fun reloadTfmSelector(runnableProject: RunnableProject) {
        val tfms = runnableProject.projectOutputs.map { it.tfm?.presentableName ?: "" }.sorted()
        tfmSelector.stringList.apply {
            clear()
            addAll(tfms)
        }
        if (tfms.any()) {
            tfmSelector.string.set(tfms.first())
        }
    }

    private fun reloadLaunchProfileSelector(runnableProject: RunnableProject) {
        val launchProfiles = FunctionLaunchProfilesService.getInstance(project).getLaunchProfiles(runnableProject)
        launchProfileSelector.profileList.apply {
            clear()
            addAll(launchProfiles)
        }
        if (launchProfiles.any()) {
            launchProfileSelector.profile.set(launchProfiles.first())
        }
    }

    private fun readLocalSettingsForProject(runnableProject: RunnableProject) {
        functionLocalSettings = FunctionLocalSettingsService
            .getInstance(project)
            .getFunctionLocalSettings(runnableProject)
    }

    private fun handleChangeTfmSelection() {
        if (!isLoaded) return

        val projectOutput = getSelectedProjectOutput()
        val launchProfile = launchProfileSelector.profile.valueOrNull

        recalculateFields(projectOutput, launchProfile?.content)
    }

    private fun handleProfileSelection() {
        if (!isLoaded) return

        val projectOutput = getSelectedProjectOutput()
        val launchProfile = launchProfileSelector.profile.valueOrNull

        recalculateFields(projectOutput, launchProfile?.content)
    }

    private fun recalculateFields(projectOutput: ProjectOutput?, profile: LaunchSettingsJson.Profile?) {
        if (trackWorkingDirectory) {
            val workingDirectory = getWorkingDirectory(profile, projectOutput)
            workingDirectorySelector.path.set(workingDirectory)
            workingDirectorySelector.defaultValue.set(workingDirectory)
        }
        if (trackArguments) {
            val arguments = getArguments(profile, projectOutput)
            programParametersEditor.parametersString.set(arguments)
            programParametersEditor.defaultValue.set(arguments)
        }
        if (trackEnvs) {
            val envs = getEnvironmentVariables(profile).toSortedMap()
            environmentVariablesEditor.envs.set(envs)
        }
        if (trackUrl) {
            val applicationUrl = getApplicationUrl(profile, projectOutput, functionLocalSettings)
            urlEditor.text.value = applicationUrl
            urlEditor.defaultValue.value = applicationUrl
            dotNetBrowserSettingsEditor.settings.value = BrowserSettings(profile?.launchBrowser == true, false, null)
        }
    }

    private fun handleArgumentsChange() {
        if (!isLoaded) return

        val projectOutput = getSelectedProjectOutput()
        val launchProfile = launchProfileSelector.profile.valueOrNull

        val arguments = getArguments(launchProfile?.content, projectOutput)
        trackArguments = arguments == programParametersEditor.parametersString.value
    }

    private fun handleWorkingDirectoryChange() {
        if (!isLoaded) return

        val projectOutput = getSelectedProjectOutput()
        val launchProfile = launchProfileSelector.profile.valueOrNull

        val workingDirectory = getWorkingDirectory(launchProfile?.content, projectOutput)
        trackWorkingDirectory = workingDirectory == workingDirectorySelector.path.value
    }

    private fun handleEnvValueChange() {
        if (!isLoaded) return

        val launchProfile = launchProfileSelector.profile.valueOrNull

        val envs = getEnvironmentVariables(launchProfile?.content).toSortedMap()
        val editorEnvs = environmentVariablesEditor.envs.value.toSortedMap()
        trackEnvs = envs == editorEnvs
    }

    private fun handleUrlValueChange() {
        if (!isLoaded) return

        val projectOutput = getSelectedProjectOutput()
        val launchProfile = launchProfileSelector.profile.valueOrNull

        val applicationUrl = getApplicationUrl(launchProfile?.content, projectOutput, functionLocalSettings)
        trackUrl = urlEditor.text.value == applicationUrl
    }

    fun reset(
        projectFilePath: String,
        projectTfm: String,
        launchProfileName: String,
        trackArguments: Boolean,
        arguments: String,
        trackWorkingDirectory: Boolean,
        workingDirectory: String,
        functionNames: String,
        trackEnvs: Boolean,
        envs: Map<String, String>,
        useExternalConsole: Boolean,
        trackUrl: Boolean,
        dotNetStartBrowserParameters: DotNetStartBrowserParameters
    ) {
        isLoaded = false

        this.trackArguments = trackArguments
        this.trackWorkingDirectory = trackWorkingDirectory
        this.trackEnvs = trackEnvs
        this.trackUrl = trackUrl

        runnableProjectsModel?.projects?.adviseOnce(lifetime) { projectList ->
            functionNamesEditor.text.value = functionNames
            functionNamesEditor.defaultValue.value = ""

            dotNetBrowserSettingsEditor.settings.set(
                BrowserSettings(
                    dotNetStartBrowserParameters.startAfterLaunch,
                    dotNetStartBrowserParameters.withJavaScriptDebugger,
                    dotNetStartBrowserParameters.browser
                )
            )

            if (projectFilePath.isEmpty() || projectList.none {
                    it.projectFilePath == projectFilePath && it.kind == AzureRunnableProjectKinds.AzureFunctions
                }) {
                if (projectFilePath.isEmpty()) {
                    addFirstFunctionProject(projectList)
                } else {
                    addFakeProject(projectList, projectFilePath)
                }
            } else {
                addSelectedFunctionProject(
                    projectList,
                    projectFilePath,
                    projectTfm,
                    launchProfileName,
                    trackArguments,
                    arguments,
                    trackWorkingDirectory,
                    workingDirectory,
                    trackEnvs,
                    envs,
                    useExternalConsole,
                    trackUrl,
                    dotNetStartBrowserParameters
                )
            }

            isLoaded = true
        }
    }

    private fun addFirstFunctionProject(projectList: List<RunnableProject>) {
        val runnableProject = projectList.firstOrNull { it.kind == AzureRunnableProjectKinds.AzureFunctions }
            ?: return
        projectSelector.project.set(runnableProject)
        isLoaded = true
        handleProjectSelection(runnableProject)
    }

    private fun addFakeProject(projectList: List<RunnableProject>, projectFilePath: String) {
        val fakeProjectName = File(projectFilePath).name
        val fakeProject = RunnableProject(
            fakeProjectName,
            fakeProjectName,
            projectFilePath,
            RunnableProjectKinds.Unloaded,
            emptyList(),
            emptyList(),
            null,
            emptyList()
        )
        projectSelector.projectList.apply {
            clear()
            addAll(projectList + fakeProject)
        }
        projectSelector.project.set(fakeProject)
    }

    private fun addSelectedFunctionProject(
        projectList: List<RunnableProject>,
        projectFilePath: String,
        tfm: String,
        launchProfile: String,
        trackArguments: Boolean,
        arguments: String,
        trackWorkingDirectory: Boolean,
        workingDirectory: String,
        trackEnvs: Boolean,
        envs: Map<String, String>,
        useExternalConsole: Boolean,
        trackUrl: Boolean,
        dotNetStartBrowserParameters: DotNetStartBrowserParameters
    ) {
        val runnableProject = projectList.singleOrNull {
            it.projectFilePath == projectFilePath && it.kind == AzureRunnableProjectKinds.AzureFunctions
        } ?: return

        projectSelector.project.set(runnableProject)
        reloadTfmSelector(runnableProject)
        reloadLaunchProfileSelector(runnableProject)
        readLocalSettingsForProject(runnableProject)

        // Disable "Use external console" for Isolated worker
        val workerRuntime = functionLocalSettings?.values?.workerRuntime ?: FunctionWorkerRuntime.DOTNET_ISOLATED
        val workerRuntimeSupportsExternalConsole = workerRuntime != FunctionWorkerRuntime.DOTNET_ISOLATED
        useExternalConsoleEditor.isVisible.set(workerRuntimeSupportsExternalConsole)
        useExternalConsoleEditor.isSelected.set(workerRuntimeSupportsExternalConsole && useExternalConsole)

        val selectedTfm =
            if (tfm.isNotEmpty()) tfmSelector.stringList.firstOrNull { it == tfm }
            else tfmSelector.stringList.firstOrNull()
        if (selectedTfm != null) {
            tfmSelector.string.set(selectedTfm)
        } else {
            tfmSelector.string.set("")
        }

        val selectedProfile =
            if (launchProfile.isNotEmpty()) launchProfileSelector.profileList.firstOrNull { it.name == launchProfile }
            else launchProfileSelector.profileList.firstOrNull()
        if (selectedProfile != null) {
            launchProfileSelector.profile.set(selectedProfile)
        } else {
            val fakeLaunchProfile = LaunchProfile(launchProfile, LaunchSettingsJson.Profile.UNKNOWN)
            launchProfileSelector.profileList.add(fakeLaunchProfile)
            launchProfileSelector.profile.set(fakeLaunchProfile)
        }

        val selectedOutput = getSelectedProjectOutput() ?: return

        val effectiveArguments =
            if (trackArguments) getArguments(selectedProfile?.content, selectedOutput)
            else arguments
        programParametersEditor.defaultValue.set(effectiveArguments)
        programParametersEditor.parametersString.set(effectiveArguments)

        val effectiveWorkingDirectory =
            if (trackWorkingDirectory) getWorkingDirectory(selectedProfile?.content, selectedOutput)
            else workingDirectory
        workingDirectorySelector.defaultValue.set(effectiveWorkingDirectory)
        workingDirectorySelector.path.set(effectiveWorkingDirectory)

        val effectiveEnvs =
            if (trackEnvs) getEnvironmentVariables(selectedProfile?.content)
            else envs
        environmentVariablesEditor.envs.set(effectiveEnvs)

        val effectiveUrl =
            if (trackUrl) getApplicationUrl(selectedProfile?.content, selectedOutput, functionLocalSettings)
            else dotNetStartBrowserParameters.url
        urlEditor.defaultValue.value = effectiveUrl
        urlEditor.text.value = effectiveUrl
    }

    private fun getSelectedProjectOutput(): ProjectOutput? {
        val selectedProject = projectSelector.project.valueOrNull ?: return null
        return selectedProject
            .projectOutputs
            .singleOrNull { it.tfm?.presentableName == tfmSelector.string.valueOrNull }
    }
}