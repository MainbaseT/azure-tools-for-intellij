/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function

import com.intellij.openapi.application.EDT
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.ui.EditorNotifications
import com.jetbrains.rd.platform.util.idea.LifetimedService
import com.jetbrains.rd.util.firstOrNull
import com.jetbrains.rider.model.RdNuGetProject
import com.jetbrains.rider.nuget.RiderNuGetFacade
import com.jetbrains.rider.projectView.workspace.*
import kotlinx.coroutines.*
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration.Companion.milliseconds

@Service(Service.Level.PROJECT)
class FunctionMissingNugetPackageService(
    private val project: Project,
    private val scope: CoroutineScope
) : LifetimedService() {
    companion object {
        fun getInstance(project: Project) = project.service<FunctionMissingNugetPackageService>()

        private val packageNames = setOf(
            "Microsoft.Azure.Functions.Worker.Extensions.Http.AspNetCore",
            "Microsoft.Azure.WebJobs.Extensions.Storage.Blobs",
            "Microsoft.Azure.WebJobs.Extensions.Storage.Queues",
            "Microsoft.Azure.WebJobs.Extensions.CosmosDB",
            "Microsoft.Azure.WebJobs.Extensions.DurableTask",
            "Microsoft.Azure.WebJobs.Extensions.EventGrid",
            "Microsoft.Azure.WebJobs.Extensions.EventHubs",
            "Microsoft.Azure.WebJobs.Extensions.ServiceBus",
            "Microsoft.Azure.WebJobs.Extensions.Sql",
            "Microsoft.Azure.WebJobs.Extensions.Dapr",
            "Microsoft.Azure.Functions.Worker.Extensions.Storage.Blobs",
            "Microsoft.Azure.Functions.Worker.Extensions.Storage.Queues",
            "Microsoft.Azure.Functions.Worker.Extensions.CosmosDB",
            "Microsoft.Azure.Functions.Worker.Extensions.EventGrid",
            "Microsoft.Azure.Functions.Worker.Extensions.EventHubs",
            "Microsoft.Azure.Functions.Worker.Extensions.Http",
            "Microsoft.Azure.Functions.Worker.Extensions.ServiceBus",
            "Microsoft.Azure.Functions.Worker.Extensions.Timer",
            "Microsoft.Azure.Functions.Worker.Extensions.Sql",
            "Microsoft.Azure.Functions.Worker.Extensions.Dapr",
            "CloudNative.CloudEvents"
        )

        private val markerToTriggerMap = mapOf(
            // Default worker
            "Microsoft.Azure.WebJobs" to mapOf(
                "BlobTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage.Blobs", "5.3.3")
                ),
                "QueueTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.Storage.Queues", "5.3.3")
                ),
                "CosmosDBTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.CosmosDB", "4.8.1")
                ),
                "OrchestrationTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.DurableTask", "2.13.7")
                ),
                "EventGridTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventGrid", "3.4.2")
                ),
                "EventHubTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "6.3.5")
                ),
                "IoTHubTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.EventHubs", "6.3.5")
                ),
                "ServiceBusTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.ServiceBus", "5.16.4")
                ),
                "SqlTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.Sql", "3.0.534")
                ),
                "DaprPublish" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.1")
                ),
                "DaprInvoke" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.1")
                ),
                "DaprState" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.1")
                ),
                "DaprServiceInvocationTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.1")
                ),
                "DaprTopicTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.WebJobs.Extensions.Dapr", "1.0.1")
                ),
            ),

            // Isolated worker
            "Microsoft.Azure.Functions.Worker" to mapOf(
                //In Program.cs see: https://learn.microsoft.com/en-us/azure/azure-functions/dotnet-isolated-process-guide?tabs=windows#aspnet-core-integration
                "ConfigureFunctionsWebApplication" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Http.AspNetCore", "1.3.2")
                ),
                //In triggers
                "BlobTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Storage.Blobs", "6.6.0")
                ),
                "QueueTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Storage.Queues", "5.5.0")
                ),
                "CosmosDBTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.CosmosDB", "4.11.0")
                ),
                "EventGridTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.EventGrid", "3.4.2")
                ),
                "EventHubTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.EventHubs", "6.3.6")
                ),
                "HttpTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Http", "3.2.0")
                ),
                "ServiceBusTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.ServiceBus", "5.22.0")
                ),
                "TimerTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Timer", "4.3.1")
                ),
                "SqlTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Sql", "3.0.534")
                ),
                "DaprPublish" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.1"),
                    PackageDependency("CloudNative.CloudEvents", "2.8.0")
                ),
                "DaprInvoke" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.1"),
                    PackageDependency("CloudNative.CloudEvents", "2.8.0")
                ),
                "DaprState" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.1"),
                    PackageDependency("CloudNative.CloudEvents", "2.8.0")
                ),
                "DaprServiceInvocationTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.1"),
                    PackageDependency("CloudNative.CloudEvents", "2.8.0")
                ),
                "DaprTopicTrigger" to listOf(
                    PackageDependency("Microsoft.Azure.Functions.Worker.Extensions.Dapr", "1.0.1"),
                    PackageDependency("CloudNative.CloudEvents", "2.8.0")
                ),
            )
        )

        private val LOG = logger<FunctionMissingNugetPackageService>()
    }

    init {
        val workspaceModelEvents = WorkspaceModelEvents.getInstance(project)
        workspaceModelEvents.addSignal.advise(serviceLifetime) {
            if (it.entity.isDependencyPackage()) {
                val packageName = it.entity.name.substringBefore("/")
                if (packageNames.contains(packageName)) {
                    LOG.trace("Package $packageName was installed")
                    cache.clear()
                }
            }
        }
        workspaceModelEvents.removeSignal.advise(serviceLifetime) {
            if (it.entity.isDependencyPackage()) {
                val packageName = it.entity.name.substringBefore("/")
                if (packageNames.contains(packageName)) {
                    LOG.trace("Package $packageName was removed")
                    cache.clear()
                }
            }
        }
    }

    private val cache = ConcurrentHashMap<String, Pair<Long, MutableList<InstallableDependency>>>()

    data class PackageDependency(val id: String, val version: String)
    data class InstallableDependency(val dependency: PackageDependency, val installableProjectPath: Path)

    fun getMissingPackages(file: VirtualFile): List<InstallableDependency>? {
        val filePath = file.path
        LOG.trace("Getting packages for $filePath")

        val (modificationStamp, dependencies) = cache[filePath] ?: return null
        if (modificationStamp == file.modificationStamp) {
            LOG.trace("Found dependencies in cache for $filePath: ${dependencies.joinToString()}")
            return dependencies
        }

        LOG.trace("Removing $filePath from cache")
        cache.remove(filePath)
        return null
    }

    fun checkForMissingPackages(file: VirtualFile) {
        scope.launch {
            val modificationStamp = file.modificationStamp
            val dependencies = getInstallableDependencies(file).toMutableList()
            cache[file.path] = modificationStamp to dependencies
            LOG.trace("Saving dependencies to cache for ${file.path}: ${dependencies.joinToString()}")

            withContext(Dispatchers.EDT) {
                EditorNotifications.getInstance(project).updateNotifications(file)
            }
        }
    }

    fun clearCache() {
        LOG.trace("Clearing the cache")
        cache.clear()
    }

    private suspend fun getInstallableDependencies(file: VirtualFile): List<InstallableDependency> {
        val fileContent = withContext(Dispatchers.IO) {
            file.readText()
        }

        if (fileContent.isEmpty()) return emptyList()

        // Check for known marker words
        val knownMarker = markerToTriggerMap
            .filter { fileContent.contains(it.key, true) }
            .firstOrNull()
            ?: return emptyList()

        // Determine project(s) to install into
        val installableProjects = WorkspaceModel
            .getInstance(project)
            .getProjectModelEntities(file, project)
            .mapNotNull { it.containingProjectEntity() }

        if (installableProjects.isEmpty()) return emptyList()

        // For every known trigger name, verify required dependencies are installed
        val riderNuGetFacade = RiderNuGetFacade.getInstance(project)

        val loadingResult = riderNuGetFacade.waitForLoading()
        if (!loadingResult) return emptyList()

        val installableDependencies = mutableListOf<InstallableDependency>()
        for (installableProject in installableProjects) {
            val path = installableProject.getFile()?.toPath() ?: continue
            val installableProjectId = installableProject.getId(project) ?: continue
            val nugetProject = riderNuGetFacade.host.nuGetProjectModel.projects[installableProjectId] ?: continue
            for ((triggerName, dependencies) in knownMarker.value) {
                if (fileContent.contains(triggerName, true)) {
                    for (dependency in dependencies) {
                        if (!nugetProject.hasPackage(dependency.id)) {
                            installableDependencies.add(InstallableDependency(dependency, path))
                        }
                    }
                }
            }
        }

        return installableDependencies
    }

    fun installPackage(file: VirtualFile, dependency: InstallableDependency) {
        scope.launch {
            LOG.trace("Installing dependency $dependency for ${file.path}")
            val installableProject = WorkspaceModel.getInstance(project)
                .getProjectModelEntities(dependency.installableProjectPath, project)
                .firstOrNull()
            val installableProjectId = installableProject?.getId(project)
            val riderNuGetFacade = RiderNuGetFacade.getInstance(project)
            val nugetProject = riderNuGetFacade.host.nuGetProjectModel.projects[installableProjectId]

            if (installableProject != null && nugetProject != null) {
                LOG.trace("Installing dependency $dependency in project ${installableProject.name}")
                withContext(Dispatchers.EDT) {
                    riderNuGetFacade.installForProject(
                        installableProject.name,
                        dependency.dependency.id,
                        dependency.dependency.version
                    )
                }

                for (i in 0..<30) {
                    if (nugetProject.hasPackage(dependency.dependency.id)) break
                    delay(1000)
                }
            }

            withContext(Dispatchers.EDT) {
                EditorNotifications.getInstance(project).updateNotifications(file)
            }
        }
    }

    private suspend fun RiderNuGetFacade.waitForLoading(): Boolean {
        var isLoaded = host.isLoaded.valueOrNull
        if (isLoaded == true) return true

        for (i in 0..<10) {
            delay(300.milliseconds)

            isLoaded = host.isLoaded.valueOrNull
            if (isLoaded == true) return true
        }

        return false
    }

    private fun RdNuGetProject.hasPackage(dependencyId: String): Boolean {
        if (explicitPackages.any { it.id.equals(dependencyId, ignoreCase = true) }) return true
        if (integratedPackages.any { it.identity.id.equals(dependencyId, ignoreCase = true) }) return true
        return false
    }
}