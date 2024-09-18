/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.appservice

import com.intellij.execution.ExecutionException
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.jetbrains.rider.model.PublishableProjectModel
import com.microsoft.azure.toolkit.intellij.legacy.ArtifactService
import com.microsoft.azure.toolkit.lib.appservice.AppServiceAppBase
import com.microsoft.azure.toolkit.lib.appservice.function.FunctionAppBase
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase
import com.microsoft.azure.toolkit.lib.common.model.AzResource
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.zeroturnaround.zip.ZipUtil
import java.io.File
import java.io.IOException
import java.nio.file.Files

@Service(Service.Level.PROJECT)
class DotNetAppServiceDeployer(private val project: Project) {
    companion object {
        fun getInstance(project: Project): DotNetAppServiceDeployer = project.service()
        private val LOG = logger<DotNetAppServiceDeployer>()
    }

    suspend fun deploy(
        target: WebAppBase<*, *, *>,
        publishableProject: PublishableProjectModel,
        configuration: String?,
        platform: String?,
        updateStatusText: (String) -> Unit
    ): Result<Unit> {
        target.status = AzResource.Status.DEPLOYING

        val publishProjectResult = ArtifactService
            .getInstance(project)
            .publishProjectToFolder(
                publishableProject,
                configuration,
                platform,
                updateStatusText
            )
            .onFailure {
                return Result.failure(it)
            }

        val artifactFolder = publishProjectResult.getOrThrow()
        updateStatusText("Creating ${publishableProject.projectName} project ZIP...")
        val zipFile = packageWebAppArtifactDirectory(artifactFolder)
            ?: return Result.failure(ExecutionException("Unable to create zip archive with project artifacts"))
        updateStatusText("Project ZIP is created: ${zipFile.absolutePath}")

        return deploy(
            target,
            zipFile,
            updateStatusText
        )
    }

    suspend fun deploy(
        target: FunctionAppBase<*, *, *>,
        publishableProject: PublishableProjectModel,
        configuration: String?,
        platform: String?,
        updateStatusText: (String) -> Unit
    ): Result<Unit> {
        target.status = AzResource.Status.DEPLOYING

        val publishProjectResult = ArtifactService
            .getInstance(project)
            .publishProjectToFolder(
                publishableProject,
                configuration,
                platform,
                updateStatusText
            )
            .onFailure {
                return Result.failure(it)
            }

        val artifactFolder = publishProjectResult.getOrThrow()
        updateStatusText("Creating ${publishableProject.projectName} project ZIP...")
        val zipFile = packageFunctionAppArtifactDirectory(artifactFolder)
            ?: return Result.failure(ExecutionException("Unable to create zip archive with project artifacts"))
        updateStatusText("Project ZIP is created: ${zipFile.absolutePath}")

        return deploy(
            target,
            zipFile,
            updateStatusText
        )
    }

    private fun packageWebAppArtifactDirectory(artifactFolder: File): File? {
        try {
            val zipFile = Files.createTempFile(artifactFolder.nameWithoutExtension, ".zip").toFile()
            ZipUtil.pack(artifactFolder, zipFile)
            return zipFile
        } catch (e: IOException) {
            LOG.error("Unable to package the artifact directory", e)
            return null
        }
    }

    private fun packageFunctionAppArtifactDirectory(artifactFolder: File): File? {
        try {
            val zipFile = Files.createTempFile(artifactFolder.nameWithoutExtension, ".zip").toFile()
            ZipUtil.pack(artifactFolder, zipFile)
            ZipUtil.removeEntry(zipFile, "local.settings.json")
            return zipFile
        } catch (e: IOException) {
            LOG.error("Unable to package the artifact directory", e)
            return null
        }
    }

    private suspend fun deploy(
        target: AppServiceAppBase<*, *, *>,
        zipFile: File,
        updateStatusText: (String) -> Unit
    ): Result<Unit> {
        val webAppBase = target.remote
        if (webAppBase == null) {
            LOG.warn("Unable to find remote of the target")
            return Result.failure(ExecutionException("Unable to get remote Azure resource"))
        }

        updateStatusText("Starting deployment...")
        updateStatusText("Trying to deploy artifact to ${webAppBase.name()}...")

        webAppBase.zipDeployAsync(zipFile)?.awaitSingleOrNull()

        updateStatusText("Successfully deployed the artifact to ${webAppBase.defaultHostname()}")

        FileUtil.delete(zipFile)

        if (!target.getFormalStatus().isRunning) {
            updateStatusText("Starting the application after deploying artifacts...")
            target.start()
            updateStatusText("Successfully started the application.")
        }

        return Result.success(Unit)
    }
}