/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.runner.localRun

import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rd.util.lifetime.Lifetime
import com.jetbrains.rider.run.configurations.ProtocolLifetimedSettingsEditor
import com.jetbrains.rider.run.configurations.controls.*
import com.jetbrains.rider.run.configurations.controls.startBrowser.BrowserSettingsEditor
import com.jetbrains.rider.run.configurations.runnableProjectsModelIfAvailable
import javax.swing.JComponent

class FunctionRunSettingsEditor(private val project: Project) :
    ProtocolLifetimedSettingsEditor<FunctionRunConfiguration>() {

    private lateinit var viewModel: FunctionRunConfigurationViewModel

    override fun createEditor(lifetime: Lifetime): JComponent {
        viewModel = FunctionRunConfigurationViewModel(
            project,
            lifetime,
            project.runnableProjectsModelIfAvailable,
            ProjectSelector("Project:", "Project"),
            StringSelector("Target framework:", "Target_framework"),
            LaunchProfileSelector("Launch profile:", "Launch_profile"),
            ProgramParametersEditor("Function host arguments:", "Program_arguments", lifetime),
            PathSelector(
                "Working directory:",
                "Working_directory",
                FileChooserDescriptorFactory.createSingleFolderDescriptor(),
                lifetime
            ),
            TextEditor("Function names to run", "Function_name", lifetime),
            EnvironmentVariablesEditor("Environment variables:", "Environment_variables"),
            FlagEditor("Use external console", "Use_external_console"),
            ViewSeparator("Open browser"),
            TextEditor("URL", "URL", lifetime),
            BrowserSettingsEditor("")
        )
        return ControlViewBuilder(lifetime, project).build(viewModel)
    }

    override fun applyEditorTo(configuration: FunctionRunConfiguration) {
        configuration.parameters.apply {
            projectFilePath = viewModel.projectSelector.project.valueOrNull?.projectFilePath ?: ""
            projectTfm = viewModel.tfmSelector.string.valueOrNull ?: ""
            profileName = viewModel.launchProfileSelector.profile.valueOrNull?.name ?: ""
            functionNames = viewModel.functionNamesEditor.text.value
            trackArguments = viewModel.trackArguments
            arguments = viewModel.programParametersEditor.parametersString.value
            trackWorkingDirectory = viewModel.trackWorkingDirectory
            workingDirectory = FileUtil.toSystemIndependentName(viewModel.workingDirectorySelector.path.value)
            trackEnvs = viewModel.trackEnvs
            envs = viewModel.environmentVariablesEditor.envs.value
            useExternalConsole = viewModel.useExternalConsoleEditor.isSelected.value
            trackUrl = viewModel.trackUrl
            startBrowserParameters.url = viewModel.urlEditor.text.value
            startBrowserParameters.browser = viewModel.dotNetBrowserSettingsEditor.settings.value.myBrowser
            startBrowserParameters.startAfterLaunch =
                viewModel.dotNetBrowserSettingsEditor.settings.value.startAfterLaunch
            startBrowserParameters.withJavaScriptDebugger =
                viewModel.dotNetBrowserSettingsEditor.settings.value.withJavaScriptDebugger
        }
    }

    override fun resetEditorFrom(configuration: FunctionRunConfiguration) {
        configuration.parameters.apply {
            viewModel.reset(
                projectFilePath,
                projectTfm,
                profileName,
                trackArguments,
                arguments,
                trackWorkingDirectory,
                workingDirectory,
                functionNames,
                trackEnvs,
                envs,
                useExternalConsole,
                trackUrl,
                startBrowserParameters
            )
        }
    }
}