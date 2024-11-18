/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.openapi.util.SystemInfo
import com.intellij.util.text.VersionComparatorUtil
import com.microsoft.azure.toolkit.intellij.legacy.function.isFunctionCoreToolsExecutable
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import com.microsoft.azure.toolkit.intellij.legacy.function.toolingFeed.FunctionsToolingFeedService
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver
import java.nio.file.Path
import kotlin.io.path.*
import kotlin.sequences.lastOrNull
import kotlin.sequences.sortedWith

@Service(Service.Level.APP)
class FunctionCoreToolsManager2 {
    companion object {
        fun getInstance(): FunctionCoreToolsManager2 = service()
        private val LOG = logger<FunctionCoreToolsManager2>()
    }

    /**
     * Retrieves the path to the Azure Function core tools for a specified Azure Function runtime version
     * or downloads the latest core tools if not available.
     *
     * @param azureFunctionsVersion The version of Azure Functions runtime for which to get or download the core tools.
     * @return The path to the Azure Function core tools for the specified Azure Function runtime version,
     * or null if the path cannot be determined or the download fails.
     */
    suspend fun getFunctionCoreToolsPathOrDownloadForVersion(azureFunctionsVersion: String): Path? {
        val existingCoreToolsPath = getFunctionCoreToolsPathForVersion(azureFunctionsVersion)
        if (existingCoreToolsPath != null) {
            LOG.trace { "Found existing core tools path: $existingCoreToolsPath" }
            return existingCoreToolsPath
        }

        LOG.trace { "Existing core tools aren't found, downloading the latest one" }
        return downloadLatestFunctionCoreToolsForVersion(azureFunctionsVersion)
    }

    /**
     * Retrieves the path to the Azure Function core tools folder for a specified Azure Function runtime version.
     *
     * @param azureFunctionsVersion The version of Azure Functions runtime for which to get the folder.
     * @return The path to the Azure Function core tools folder for the specified Azure Function runtime version, or null if not found.
     */
    fun getFunctionCoreToolsPathForVersion(azureFunctionsVersion: String): Path? {
        val settings = AzureFunctionSettings.getInstance()
        val coreToolsPathEntries = settings.azureCoreToolsPathEntries
        val coreToolsPathFromSettings = coreToolsPathEntries
            .firstOrNull { it.functionsVersion.equals(azureFunctionsVersion, ignoreCase = true) }
            ?.coreToolsPath
            ?.let(::resolveCoreToolsPathFromSettings)
        if (coreToolsPathFromSettings?.exists() == true) {
            LOG.trace { "Get Azure Function core tools path from the settings: $coreToolsPathFromSettings" }
            return coreToolsPathFromSettings
        }

        val coreToolsRootFolder = settings.functionDownloadPath
        val coreToolsPathForVersion = Path(settings.functionDownloadPath).resolve(azureFunctionsVersion)
        if (coreToolsPathForVersion.notExists()) {
            LOG.info("Unable to find any downloaded core tools in the folder $coreToolsRootFolder for version $azureFunctionsVersion")
            return null
        }

        LOG.trace { "Get Azure Function core tools path from the download folder: $coreToolsPathForVersion" }
        return findCoreToolsPathWithLatestTag(coreToolsPathForVersion)
    }

    /**
     * Downloads the latest Azure Function core tools release for the specified Azure Functions runtime version.
     *
     * @param azureFunctionsVersion The version of Azure Functions runtime for which to download the latest core tools release.
     * @return The path to the downloaded Azure Function core tools, or null if the download was unsuccessful.
     */
    suspend fun downloadLatestFunctionCoreToolsForVersion(azureFunctionsVersion: String): Path? {
        val downloadLatestReleaseResult = FunctionsToolingFeedService
            .getInstance()
            .downloadLatestFunctionsToolingRelease(azureFunctionsVersion)

        val latestReleasePath = downloadLatestReleaseResult.getOrNull()
        if (latestReleasePath == null) {
            LOG.warn(
                "Unable to download the latest Azure Function core tooling release for version $azureFunctionsVersion",
                downloadLatestReleaseResult.exceptionOrNull()
            )
            return null
        }

        return latestReleasePath
    }

    private fun resolveCoreToolsPathFromSettings(coreToolsPathValue: String): Path? {
        if (coreToolsPathValue.isEmpty()) return null

        if (isFunctionCoreToolsExecutable(coreToolsPathValue)) {
            val coreToolsPathFromEnvironment = FunctionCliResolver.resolveFunc()?.let(::Path) ?: return null
            LOG.trace { "Resolved core tools path from environment: $coreToolsPathFromEnvironment" }
            return patchCoreToolsPath(coreToolsPathFromEnvironment)
        } else {
            val coreToolsPathFromSettings = Path(coreToolsPathValue)
            LOG.trace { "Resolved core tools path from settings: $coreToolsPathFromSettings" }
            return patchCoreToolsPath(coreToolsPathFromSettings)
        }
    }

    private fun patchCoreToolsPath(funcCoreToolsPath: Path): Path {
        val normalizedPath = if (funcCoreToolsPath.isRegularFile() && funcCoreToolsPath.isFunctionCoreTools()) {
            funcCoreToolsPath.parent
        } else {
            funcCoreToolsPath
        }
        if (!SystemInfo.isWindows) return normalizedPath

        // Chocolatey and NPM have shim executables that are not .NET (and not debuggable).
        // If it's a Chocolatey install or NPM install, rewrite the path to the tools path
        // where the func executable is located.
        //
        // Logic is similar to com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionCliResolver.resolveFunc()
        val chocolateyPath = normalizedPath.resolve("../lib/azure-functions-core-tools/tools").normalize()
        if (chocolateyPath.exists()) {
            LOG.info("Functions core tools path $normalizedPath is Chocolatey-installed. Rewriting path to $chocolateyPath")
            return chocolateyPath
        }

        val npmPath = normalizedPath.resolve("../node_modules/azure-functions-core-tools/bin").normalize()
        if (npmPath.exists()) {
            LOG.info("Functions core tools path $normalizedPath is NPM-installed. Rewriting path to $npmPath")
            return npmPath
        }

        return normalizedPath
    }

    private fun findCoreToolsPathWithLatestTag(coreToolsPathForVersion: Path): Path? {
        val latestTagFolderForVersion = coreToolsPathForVersion
            .listDirectoryEntries()
            .asSequence()
            .filter { it.isDirectory() }
            .sortedWith { first, second -> VersionComparatorUtil.compare(first.name, second.name) }
            .lastOrNull { it.exists() }

        LOG.trace { "The latest tag folder from $coreToolsPathForVersion is $latestTagFolderForVersion" }

        return latestTagFolderForVersion
    }
}