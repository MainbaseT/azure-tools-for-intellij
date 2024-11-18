/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.templates

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
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

    fun areAzureFunctionTemplatesInstalled(): Boolean {
        val functionCoreToolsFolder = FunctionCoreToolsManager
            .getInstance()
            .getFunctionCoreToolsPathForVersion(FUNCTIONS_CORE_TOOLS_LATEST_SUPPORTED_VERSION)
            ?: return false

        return RiderProjectTemplateProvider
            .getUserTemplateSources()
            .any { (isFunctionProjectTemplate(it.toPath(), functionCoreToolsFolder)) && it.exists() }
    }

    fun reloadAzureFunctionTemplates() {
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
                val templateFiles = templateFolder
                    .listDirectoryEntries()
                    .filter { isFunctionProjectTemplate(it, functionCoreToolsFolder) }

                LOG.debug("Found ${templateFiles.size} function template(s) in $templateFolder")

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
                RiderProjectTemplateProvider.removeUserTemplateSource(it.toFile())
            } else if (it.contains(netIsolatedPath)) {
                val index = it.lastIndexOf(netIsolatedPath)
                val prefix = it.root.resolve(it.subpath(0, index))
                val sourcesToRemove = templateSources.filter { ts -> ts.startsWith(prefix) }

                sourcesToRemove.forEach { str ->
                    RiderProjectTemplateProvider.removeUserTemplateSource(str.toFile())
                }
            }
        }
    }
}