/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:Suppress("UnstableApiUsage")

package com.microsoft.azure.toolkit.intellij.storage.azurite.services

import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.ColoredProcessHandler
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.services.ServiceEventListener
import com.intellij.notification.Notification
import com.intellij.notification.NotificationType
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.util.EnvironmentUtil
import com.intellij.util.application
import com.intellij.util.io.BaseOutputReader
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.util.lifetime.SequentialLifetimes
import com.microsoft.azure.toolkit.intellij.storage.azurite.actions.ShowAzuriteSettingsNotificationAction
import com.microsoft.azure.toolkit.intellij.storage.azurite.settings.AzuriteSettings
import com.microsoft.azure.toolkit.lib.storage.AzuriteStorageAccount
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

@Service(Service.Level.APP)
class AzuriteService(private val scope: CoroutineScope) : LifetimedService() {
    companion object {
        private const val AZURITE_PROCESS_TIMEOUT_MILLIS = 15000

        fun getInstance() = service<AzuriteService>()
        private val LOG = logger<AzuriteService>()
    }

    private val sessionLifetimes = SequentialLifetimes(serviceLifetime)

    private val handlerLock = Any()

    private val sessionStarted = AtomicBoolean(false)
    var session: AzuriteSession = AzuriteNotStartedSession()
        private set

    var processHandler: ColoredProcessHandler? = null
        private set

    var workspace: String? = null
        private set

    val isRunning: Boolean
        get() {
            return sessionStarted.get() && !sessionLifetimes.isTerminated
        }

    fun start(project: Project) {
        if (isRunning) {
            LOG.warn("The caller should verify if an existing session is running, before calling start()")
            return
        }

        LOG.info("Starting Azurite executable...")

        val settings = AzuriteSettings.getInstance(project)
        val azuritePath = settings.getAzuriteExecutablePath()
        if (azuritePath == null || !azuritePath.exists()) {
            Notification(
                "Azure AppServices",
                "Unable to find Azurite executable location",
                azuritePath?.let { "${it.absolutePathString()} isn't exists" } ?: "",
                NotificationType.WARNING
            )
                .addAction(ShowAzuriteSettingsNotificationAction())
                .notify(project)
            return
        }

        val workspacePath = settings.getAzuriteWorkspacePath()
        if (!workspacePath.exists()) {
            Notification(
                "Azure AppServices",
                "Unable to find Azurite workspace location",
                "${workspacePath.absolutePathString()} isn't exists",
                NotificationType.WARNING
            )
                .addAction(ShowAzuriteSettingsNotificationAction())
                .notify(project)
            return
        }

        if (settings.basicOAuth && settings.certificatePath.isEmpty()) {
            Notification(
                "Azure AppServices",
                "The certificate path cannot be empty if OAuth is enabled",
                "",
                NotificationType.WARNING
            )
                .addAction(ShowAzuriteSettingsNotificationAction())
                .notify(project)
            return
        }

        LOG.debug("Azurite executable: ${azuritePath.absolutePathString()}, Azurite workspace: ${workspacePath.absolutePathString()}")

        scope.launch {
            withBackgroundProgress(project, "Starting Azurite Emulator...") {
                val includeTableStorageParameters = supportsTableStorage(azuritePath)
                LOG.info("Azurite supports table storage: $includeTableStorageParameters")

                ensureActive()

                val commandLine = createCommandLine(azuritePath, workspacePath, includeTableStorageParameters, project)

                ensureActive()

                startAzuriteProcess(commandLine, workspacePath)
            }
        }
    }

    private fun supportsTableStorage(azuriteExecutable: Path): Boolean {
        if (!azuriteExecutable.exists())
            return false

        try {
            val commandLine = GeneralCommandLine(azuriteExecutable.absolutePathString(), "--help")
            LOG.debug("Executing ${commandLine.commandLineString}...")

            val processHandler = CapturingProcessHandler(commandLine)
            val output = processHandler.runProcess(AZURITE_PROCESS_TIMEOUT_MILLIS, true)

            LOG.debug("Result: ${output.stdout}")

            return output.stdoutLines.any { it.contains("--tableHost", true) }
        } catch (e: Exception) {
            LOG.error(
                "Error while determining whether Azurite version at '${azuriteExecutable.absolutePathString()}' supports table storage",
                e
            )
            return false
        }
    }

    private fun createCommandLine(
        azuritePath: Path,
        workspacePath: Path,
        includeTableStorageParameters: Boolean,
        project: Project
    ): GeneralCommandLine {
        val settings = AzuriteSettings.getInstance(project)

        val commandLine = GeneralCommandLine(
            azuritePath.absolutePathString(),
            "--blobHost",
            settings.blobHost,
            "--blobPort",
            settings.blobPort.toString(),
            "--queueHost",
            settings.queueHost,
            "--queuePort",
            settings.queuePort.toString(),
            "--location",
            workspacePath.absolutePathString()
        )

        if (includeTableStorageParameters) {
            commandLine.addParameters(
                "--tableHost",
                settings.tableHost,
                "--tablePort",
                settings.tablePort.toString()
            )
        }

        if (settings.looseMode) {
            commandLine.addParameter("--loose")
        }

        if (settings.basicOAuth) {
            commandLine.addParameters("--oauth", "basic")
        }

        val certificatePath = settings.certificatePath
        if (certificatePath.isNotBlank()) {
            commandLine.addParameter("--cert")
            commandLine.addParameter(certificatePath)
        }

        val certificateKeyPath = settings.certificateKeyPath
        if (certificateKeyPath.isNotBlank()) {
            commandLine.addParameter("--key")
            commandLine.addParameter(certificateKeyPath)
        }

        val certificatePassword = settings.certificatePassword
        if (certificatePassword.isNotBlank()) {
            commandLine.addParameter("--pwd")
            commandLine.addParameter(certificatePassword)
        }

        commandLine.environment.putAll(EnvironmentUtil.getEnvironmentMap())

        return commandLine
    }

    private fun startAzuriteProcess(commandLine: GeneralCommandLine, workspaceLocation: Path) {
        val sessionLifetime = sessionLifetimes.next()

        val newProcessHandler = object : ColoredProcessHandler(commandLine) {
            override fun readerOptions(): BaseOutputReader.Options = BaseOutputReader.Options.forMostlySilentProcess()
        }

        sessionLifetime.onTermination {
            if (!newProcessHandler.isProcessTerminating && !newProcessHandler.isProcessTerminated) {
                LOG.trace("Killing Azurite process")
                newProcessHandler.killProcess()
            }
        }

        newProcessHandler.addProcessListener(object : ProcessAdapter() {
            override fun processTerminated(e: ProcessEvent) {
                sessionLifetime.executeIfAlive {
                    LOG.trace("Terminating Azurite session lifetime")
                    sessionLifetime.terminate(true)
                }
            }
        })

        sessionLifetime.bracketIfAlive({
            synchronized(handlerLock) {
                processHandler = newProcessHandler
                workspace = workspaceLocation.absolutePathString()
                AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT.refresh()
            }
        }, {
            synchronized(handlerLock) {
                processHandler = null
                workspace = null
                AzuriteStorageAccount.AZURITE_STORAGE_ACCOUNT.refresh()
            }
        })

        newProcessHandler.startNotify()

        setStartedSession()
        publishSessionStartedEvent(newProcessHandler, workspaceLocation.absolutePathString())
    }

    private fun setStartedSession() {
        if (sessionStarted.compareAndSet(false, true)) {
            session = AzuriteStartedSession()
            resetServices()
        } else {
            updateServices()
        }
    }

    fun clean(project: Project) {
        if (isRunning) stop()

        val settings = AzuriteSettings.getInstance(project)
        val workspacePath = settings.getAzuriteWorkspacePath()
        if (!workspacePath.exists()) {
            Notification(
                "Azure AppServices",
                "Unable to find Azurite workspace location",
                "${workspacePath.absolutePathString()} isn't exists",
                NotificationType.WARNING
            )
                .addAction(ShowAzuriteSettingsNotificationAction())
                .notify(project)
            return
        }

        try {
            application.runWriteAction {
                FileUtil.deleteRecursively(workspacePath)
                FileUtil.createDirectory(workspacePath.toFile())
            }
        } catch (e: Exception) {
            LOG.error("Error during clean", e)
        }
    }

    fun stop() {
        sessionLifetimes.terminateCurrent()
        updateServices()
        publishSessionStoppedEvent()
    }

    private fun updateServices() =
        application.messageBus
            .syncPublisher(ServiceEventListener.TOPIC)
            .handle(ServiceEventListener.ServiceEvent.createEvent(
                ServiceEventListener.EventType.SERVICE_CHANGED,
                AzuriteServiceViewContributor::class.java,
                AzuriteServiceViewContributor::class.java
            ))

    private fun resetServices() =
        application.messageBus
            .syncPublisher(ServiceEventListener.TOPIC)
            .handle(ServiceEventListener.ServiceEvent.createResetEvent(AzuriteServiceViewContributor::class.java))

    private fun publishSessionStartedEvent(processHandler: ColoredProcessHandler, workspace: String) =
        application.messageBus
            .syncPublisher(AzuriteSessionListener.TOPIC)
            .sessionStarted(processHandler, workspace)

    private fun publishSessionStoppedEvent() =
        application.messageBus
            .syncPublisher(AzuriteSessionListener.TOPIC)
            .sessionStopped()
}