/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.diagnostic.trace
import com.intellij.util.concurrency.ThreadingAssertions
import com.jetbrains.rider.projectView.projectTemplates.providers.RiderProjectTemplateProvider
import com.microsoft.azure.toolkit.intellij.legacy.function.FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION
import com.microsoft.azure.toolkit.intellij.legacy.function.coreTools.FunctionCoreToolsManager
import java.nio.file.Path
import kotlin.io.path.*

@Service
class FunctionTemplateManager {
    companion object {
        fun getInstance(): FunctionTemplateManager = service()

        private val LOG = logger<FunctionTemplateManager>()
    }

    private val netIsolatedPath = Path("net-isolated")

    /**
     * Checks if the Azure Function templates are installed.
     *
     * This function verifies if the required Azure Function templates are available
     * in the user's template sources.
     *
     * @return `true` if Azure Function templates are installed, otherwise `false`.
     */
    fun areAzureFunctionTemplatesInstalled(): Boolean {
        val functionCoreToolsFolder = FunctionCoreToolsManager
            .getInstance()
            .getFunctionCoreToolsPathForVersion(FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION)
            ?: return false

        return RiderProjectTemplateProvider
            .getUserTemplateSources()
            .any { (isFunctionProjectTemplate(it.toPath(), functionCoreToolsFolder)) && it.exists() }
    }

    /**
     * Reloads the Azure Function templates.
     *
     * This function attempts to reload the Azure Function templates by performing the following steps:
     * 1. Retrieves the path to the installed Azure Function core tools.
     * 2. Removes any previously loaded templates from the user's template sources.
     * 3. Checks and registers new templates from the Azure Function core tool folder.
     */
    fun reloadAzureFunctionTemplates() {
        LOG.trace { "Reloading Azure Functions templates" }

        ThreadingAssertions.assertBackgroundThread()

        val functionCoreToolsFolder = FunctionCoreToolsManager
            .getInstance()
            .getFunctionCoreToolsPathForVersion(FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION)
            ?: return

        removePreviousTemplates(functionCoreToolsFolder.parent)

        val templateFolders = listOf(
            functionCoreToolsFolder.resolve("templates"),
            functionCoreToolsFolder.resolve("templates/net6-isolated"),
            functionCoreToolsFolder.resolve("templates/net-isolated")
        ).filter { it.exists() }

        for (templateFolder in templateFolders) {
            try {
                LOG.trace { "Checking $templateFolder for Azure Functions templates" }

                val templateFiles = templateFolder
                    .listDirectoryEntries()
                    .filter { isFunctionProjectTemplate(it, functionCoreToolsFolder) }

                LOG.trace { "Found ${templateFiles.size} function template(s) in $templateFolder" }

                templateFiles.forEach { file ->
                    RiderProjectTemplateProvider.addUserTemplateSource(file.toFile())
                }
            } catch (e: Exception) {
                LOG.error("Could not register project templates from $templateFolder", e)
            }
        }
    }

    private fun isFunctionProjectTemplate(path: Path?, coreToolPath: Path): Boolean {
        if (path == null) return false
        if (
            !path.nameWithoutExtension.startsWith("projectTemplates.", true) ||
            !path.extension.equals("nupkg", true)
        ) return false

        return path.startsWith(coreToolPath)
    }

    private fun removePreviousTemplates(functionCoreToolsFolder: Path) {
        val templateSources = RiderProjectTemplateProvider
            .getUserTemplateSources()
            .map { it.toPath() }

        templateSources.forEach {
            if (it.startsWith(functionCoreToolsFolder)) {
                LOG.trace { "Removing Azure Functions template $it" }
                RiderProjectTemplateProvider.removeUserTemplateSource(it.toFile())
            } else if (it.contains(netIsolatedPath)) {
                val index = it.lastIndexOf(netIsolatedPath)
                val prefix = it.root.resolve(it.subpath(0, index))
                val sourcesToRemove = templateSources.filter { ts -> ts.startsWith(prefix) }

                sourcesToRemove.forEach { str ->
                    LOG.trace { "Removing Azure Functions template $it" }
                    RiderProjectTemplateProvider.removeUserTemplateSource(str.toFile())
                }
            }
        }
    }
}