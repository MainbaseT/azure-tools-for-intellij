/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package com.microsoft.azure.toolkit.intellij.legacy.function.toolingFeed

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.io.ZipUtil
import com.intellij.util.net.ssl.CertificateManager
import com.jetbrains.rd.util.concurrentMapOf
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.core.isEmpty
import io.ktor.utils.io.core.readBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.exists

@Service(Service.Level.APP)
class FunctionsToolingFeedService : Disposable {
    companion object {
        fun getInstance(): FunctionsToolingFeedService = service()
        private val LOG = logger<FunctionsToolingFeedService>()
    }

    private val releaseCache = concurrentMapOf<String, FunctionsToolingRelease>()

    private val client = HttpClient(CIO) {
        engine {
            https {
                trustManager = CertificateManager.getInstance().trustManager
            }
        }
        install(ContentNegotiation) {
            json(Json {
                explicitNulls = false
                ignoreUnknownKeys = true
                allowTrailingComma = true
            })
        }
    }

    /**
     * Downloads and saves the Azure Functions tooling release feed if the release cache is empty.
     *
     * @return Result wrapping any exception encountered during the execution.
     */
    suspend fun downloadAndSaveReleaseFeed() = kotlin.runCatching {
        if (releaseCache.isNotEmpty()) return@runCatching

        val feed = getReleaseFeed()
        val releaseTags = feed.tags
            .toSortedMap()
            .filterValues { !it.releaseQuality.isNullOrEmpty() && !it.release.isNullOrEmpty() }
        val releaseFilter = getReleaseFilterForCurrentSystem()

        for ((releaseTagName, releaseTag) in releaseTags) {
            val releaseFromTag = releaseTag.release ?: continue
            val release = feed.releases[releaseFromTag] ?: continue
            val coreToolsRelease = release.findCoreToolsRelease(releaseFilter) ?: continue
            LOG.debug("Release for Azure core tools version ${releaseTagName.lowercase()}: ${releaseTag.release}; ${coreToolsRelease.downloadLink}")

            val releaseKey = releaseTagName.lowercase()
            releaseCache.putIfAbsent(
                releaseKey,
                FunctionsToolingRelease(releaseKey, releaseFromTag, coreToolsRelease.downloadLink ?: "")
            )
        }
    }

    /**
     * Retrieves the file system path for the latest available Azure Functions tooling release based on the provided Azure Functions runtime version.
     *
     * @param azureFunctionsVersion The version of Azure Functions runtime for which to get the latest tooling release path.
     * @return The path to the latest Azure Functions tooling release, or null if the release could not be determined.
     */
    fun getPathForLatestFunctionsToolingRelease(azureFunctionsVersion: String): Path? {
        val toolingRelease = getLatestFunctionsToolingRelease(azureFunctionsVersion) ?: return null
        val downloadRoot = getReleaseDownloadRoot()
        return downloadRoot.resolve(toolingRelease.functionsVersion).resolve(toolingRelease.coreToolsVersion)
    }

    /**
     * Downloads the latest Azure Functions tooling release for the specified Azure Functions runtime version.
     *
     * This method fetches the release information, determines if the release has already been downloaded,
     * and if not, it downloads the release, extracts it to the appropriate directory and cleans up any temporary files.
     *
     * @param azureFunctionsVersion The version of Azure Functions runtime for which to download the latest tooling release.
     * @return A Result wrapping the path to the latest Azure Functions tooling release.
     */
    suspend fun downloadLatestFunctionsToolingRelease(azureFunctionsVersion: String) = kotlin.runCatching {
        downloadAndSaveReleaseFeed().getOrThrow()

        val toolingRelease = getLatestFunctionsToolingRelease(azureFunctionsVersion)
            ?: error("Unable to obtain latest tooling release")
        val toolingReleasePath = getPathForLatestFunctionsToolingRelease(azureFunctionsVersion)
            ?: error(IllegalStateException("Unable to obtain a path of the latest tooling release"))
        val funcExecutablePath = toolingReleasePath.resolve("func.exe") //todo: for mac and linux
        if (funcExecutablePath.exists()) {
            LOG.trace { "The release $toolingRelease is already downloaded" }
            return@runCatching toolingReleasePath
        }

        val tempFile = FileUtil.createTempFile(
            File(FileUtil.getTempDirectory()),
            "AzureFunctions-${toolingRelease.functionsVersion}-${toolingRelease.coreToolsVersion}",
            ".zip",
            true,
            true
        )

        withContext(Dispatchers.IO) {
            client.prepareGet(toolingRelease.coreToolsArtifactUrl).execute { httpResponse ->
                val channel: ByteReadChannel = httpResponse.body()
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(DEFAULT_BUFFER_SIZE.toLong())
                    while (!packet.isEmpty) {
                        val bytes = packet.readBytes()
                        tempFile.appendBytes(bytes)
                    }
                }
            }
        }

        if (!toolingReleasePath.exists()) {
            toolingReleasePath.createDirectories()
        }

        ZipUtil.extract(tempFile.toPath(), toolingReleasePath, null, true)

        if (tempFile.exists()) tempFile.delete()

        return@runCatching toolingReleasePath
    }

    private suspend fun getReleaseFeed(): ReleaseFeed {
        val feedUrl = getReleaseFeedUrl()
        val response = withContext(Dispatchers.IO) {
            client.get(feedUrl)
        }
        return response.body<ReleaseFeed>()
    }

    private fun getReleaseFeedUrl() = Registry.get("azure.function_app.core_tools.feed.url").asString()


    private fun getLatestFunctionsToolingRelease(azureFunctionsVersion: String): FunctionsToolingRelease? {
        val toolingRelease = releaseCache[azureFunctionsVersion.lowercase()]
        if (toolingRelease == null) {
            LOG.warn("Could not determine Azure Functions core tools release for version: '$azureFunctionsVersion'")
            return null
        }

        return toolingRelease
    }

    private fun getReleaseDownloadRoot(): Path {
        val settings = AzureFunctionSettings.getInstance()
        return Path(settings.functionDownloadPath)
    }

    private fun Release.findCoreToolsRelease(releaseFilter: FunctionToolingFeedFilter) =
        coreTools
            .asSequence()
            .filter {
                it.os.equals(releaseFilter.os, ignoreCase = true) && !it.downloadLink.isNullOrEmpty()
            }
            .sortedWith(
                compareBy<CoreToolsRelease> {
                    releaseFilter.architectures.indexOfFirst { architecture ->
                        it.architecture.equals(architecture, ignoreCase = true)
                    }.let { rank -> if (rank >= 0) rank else 9999 }
                }.thenBy {
                    releaseFilter.sizes.indexOfFirst { size ->
                        it.size.equals(size, ignoreCase = true)
                    }.let { rank -> if (rank >= 0) rank else 9999 }
                })
            .firstOrNull()

    override fun dispose() = client.close()
}
