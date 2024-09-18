/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.debug
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.util.registry.Registry
import com.intellij.util.concurrency.ThreadingAssertions
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsMsBuildService.Companion.PROPERTY_AZURE_FUNCTIONS_VERSION
import com.microsoft.azure.toolkit.intellij.legacy.function.isFunctionCoreToolsExecutable
import com.microsoft.azure.toolkit.intellij.legacy.function.settings.AzureFunctionSettings
import com.microsoft.azure.toolkit.lib.appservice.utils.FunctionCliResolver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Service
class FunctionCoreToolsInfoProvider {
    companion object {
        fun getInstance(): FunctionCoreToolsInfoProvider = service()

        private val LOG = logger<FunctionCoreToolsInfoProvider>()
    }

    suspend fun retrieveForProject(
        project: Project,
        projectFilePath: String,
        allowDownload: Boolean
    ): Pair<String, FunctionCoreToolsInfo>? {
        val azureFunctionsVersion = FunctionCoreToolsMsBuildService
            .getInstance()
            .requestAzureFunctionsVersion(project, projectFilePath)

        if (azureFunctionsVersion == null) {
            LOG.warn("Could not determine project MSBuild property '${PROPERTY_AZURE_FUNCTIONS_VERSION}'")
            return null
        }

        LOG.debug { "Azure Functions version: $azureFunctionsVersion" }

        val coreToolsInfo = withContext(Dispatchers.Default) {
            retrieveForVersion(azureFunctionsVersion, allowDownload)
        }
        if (coreToolsInfo == null) {
            LOG.warn("Could not determine Azure Functions Core Tools information")
            return null
        }

        LOG.debug { "Core tools executable: ${coreToolsInfo.coreToolsExecutable}" }

        return azureFunctionsVersion to coreToolsInfo
    }

    suspend fun retrieveForVersion(
        azureFunctionsVersion: String,
        allowDownload: Boolean
    ): FunctionCoreToolsInfo? {
        ThreadingAssertions.assertBackgroundThread()

        val coreToolsFromConfiguration = retrieveFromConfiguration(azureFunctionsVersion)
        if (coreToolsFromConfiguration != null) return coreToolsFromConfiguration

        LOG.info("Could not determine Azure Core Tools path from configuration")

        val coreToolsFromFeed = retrieveFromFeed(azureFunctionsVersion, allowDownload)
        if (coreToolsFromFeed != null) return coreToolsFromFeed

        LOG.info("Could not determine Azure Core Tools path from feed")

        return null
    }

    private fun retrieveFromConfiguration(azureFunctionsVersion: String): FunctionCoreToolsInfo? {
        val settings = AzureFunctionSettings.getInstance()
        val toolsPathEntries = settings.azureCoreToolsPathEntries
        LOG.debug("Azure Core Tools path entries: ${toolsPathEntries.joinToString { "${it.functionsVersion}: ${it.coreToolsPath}" }}")

        val toolsPathFromConfiguration = toolsPathEntries
            .firstOrNull { it.functionsVersion.equals(azureFunctionsVersion, ignoreCase = true) }
            ?.coreToolsPath
            ?: return null

        if (toolsPathFromConfiguration.isEmpty()) return null

        // If the configuration is func/func.cmd/func.exe, try and determine the full path from the environment
        if (isFunctionCoreToolsExecutable(toolsPathFromConfiguration)) {
            val toolsPathFromEnvironment = FunctionCliResolver.resolveFunc()?.let { resolveFromPath(File(it)) }
            if (toolsPathFromEnvironment == null) {
                LOG.warn("Azure Functions Core Tools path is set to '$toolsPathFromConfiguration' in configuration, but could not be resolved.")
            }

            return toolsPathFromEnvironment
        }

        return resolveFromPath(File(toolsPathFromConfiguration))
    }

    private suspend fun retrieveFromFeed(
        azureFunctionsVersion: String,
        allowDownload: Boolean
    ): FunctionCoreToolsInfo? {
        val coreToolsPathFromFeed = FunctionCoreToolsManager
            .getInstance()
            .demandCoreToolsPathForVersion(
                azureFunctionsVersion,
                Registry.get("azure.function_app.core_tools.feed.url").asString(),
                allowDownload
            ) ?: return null

        return resolveFromPath(coreToolsPathFromFeed)
    }

    private fun resolveFromPath(funcCoreToolsPath: File): FunctionCoreToolsInfo? {
        val patchedPath = patchCoreToolsPath(funcCoreToolsPath)

        val executablePath = if (SystemInfo.isWindows) {
            patchedPath.resolve("func.exe")
        } else {
            patchedPath.resolve("func")
        }

        if (!executablePath.exists()) {
            return null
        }

        if (!executablePath.canExecute()) {
            LOG.warn("Updating executable flag for $executablePath...")
            try {
                executablePath.setExecutable(true)
            } catch (s: SecurityException) {
                LOG.error("Failed setting executable flag for $executablePath", s)
            }
        }

        return FunctionCoreToolsInfo(patchedPath.toPath(), executablePath.toPath())
    }

    private fun patchCoreToolsPath(funcCoreToolsPath: File): File {
        val normalizedPath = normalizeCoreToolsPath(funcCoreToolsPath)
        if (!SystemInfo.isWindows) return normalizedPath

        // Chocolatey and NPM have shim executables that are not .NET (and not debuggable).
        // If it's a Chocolatey install or NPM install, rewrite the path to the tools path
        // where the func executable is located.
        //
        // Logic is similar to com.microsoft.azure.toolkit.intellij.function.runner.core.FunctionCliResolver.resolveFunc()
        val chocolateyPath = normalizedPath.resolve("../lib/azure-functions-core-tools/tools").normalize()
        if (chocolateyPath.exists()) {
            LOG.info("Functions core tools path ${normalizedPath.path} is Chocolatey-installed. Rewriting path to ${chocolateyPath.path}")
            return chocolateyPath
        }

        val npmPath = normalizedPath.resolve("../node_modules/azure-functions-core-tools/bin").normalize()
        if (npmPath.exists()) {
            LOG.info("Functions core tools path ${normalizedPath.path} is NPM-installed. Rewriting path to ${npmPath.path}")
            return npmPath
        }

        return normalizedPath
    }

    private fun normalizeCoreToolsPath(path: File) =
        if (path.isFile && path.isFunctionCoreTools()) {
            path.parentFile
        } else {
            path
        }
}