/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.storage.azurite.services

import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.services.SimpleServiceViewDescriptor
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.ide.projectView.PresentationData
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.application
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import com.microsoft.azure.toolkit.ide.common.icon.AzureIcons
import com.microsoft.azure.toolkit.intellij.common.IntelliJAzureIcons
import com.microsoft.azure.toolkit.intellij.storage.StorageDisposable

class AzuriteSessionDescriptor(project: Project) : SimpleServiceViewDescriptor(
    "Azurite Emulator", IntelliJAzureIcons.getIcon(AzureIcons.StorageAccount.AZURITE)
), Disposable, AzuriteSessionListener {
    companion object {
        private val defaultToolbarActions = DefaultActionGroup(
            ActionManager.getInstance().getAction("AzureToolkit.Azurite.Start"),
            ActionManager.getInstance().getAction("AzureToolkit.Azurite.Stop"),
            ActionManager.getInstance().getAction("AzureToolkit.Azurite.Clean"),
            ActionManager.getInstance().getAction("AzureToolkit.Azurite.ShowSettings")
        )
    }

    private val myLock = Any()
    private var processHandler: ColoredProcessHandler? = null

    private val consoleView: ConsoleView = TextConsoleBuilderFactory
        .getInstance()
        .createBuilder(project)
        .apply { setViewer(true) }
        .console

    init {
        val service = AzuriteService.getInstance()
        val activeProcessHandler = service.processHandler
        val activeWorkSpace = service.workspace
        if (activeProcessHandler != null && activeWorkSpace != null) {
            connectToSession(activeProcessHandler, activeWorkSpace)
        }

        application.messageBus
            .connect(this)
            .subscribe(AzuriteSessionListener.TOPIC, this)

        Disposer.register(StorageDisposable.getInstance(), this)
        Disposer.register(this, consoleView)
    }

    override fun getPresentation() = PresentationData().apply {
        if (processHandler != null) {
            setIcon(IntelliJAzureIcons.getIcon("/icons/Microsoft.Storage/azurite/running.svg"))
        } else {
            setIcon(IntelliJAzureIcons.getIcon("/icons/Microsoft.Storage/azurite/stopped.svg"))
        }
        addText("Azurite emulator", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }

    override fun getContentComponent() = BorderLayoutPanel().apply {
        border = JBUI.Borders.empty()
        add(consoleView.component)
    }

    override fun getToolbarActions() = defaultToolbarActions

    private fun connectToSession(activeProcessHandler: ColoredProcessHandler, activeWorkspace: String) {
        if (processHandler == activeProcessHandler) return

        synchronized(myLock) {
            processHandler = activeProcessHandler
        }

        consoleView.print("Workspace: $activeWorkspace\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
        consoleView.attachToProcess(activeProcessHandler)
        consoleView.print("Attached to Azurite process.\n", ConsoleViewContentType.LOG_INFO_OUTPUT)
    }

    private fun disconnectFromSession() {
        if (processHandler == null) return

        synchronized(myLock) {
            processHandler = null
        }
    }

    override fun sessionStarted(processHandler: ColoredProcessHandler, workspace: String) {
        connectToSession(processHandler, workspace)
    }

    override fun sessionStopped() {
        disconnectFromSession()
    }

    override fun dispose() {
    }
}