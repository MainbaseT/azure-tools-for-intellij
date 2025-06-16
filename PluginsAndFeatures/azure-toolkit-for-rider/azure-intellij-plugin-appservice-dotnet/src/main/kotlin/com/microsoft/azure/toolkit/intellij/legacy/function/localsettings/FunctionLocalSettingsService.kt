/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.localsettings

import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.RunnableProject
import com.microsoft.azure.toolkit.intellij.legacy.function.daemon.AzureRunnableProjectKinds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.exists

@Service(Service.Level.PROJECT)
class FunctionLocalSettingsService(private val project: Project) {
    companion object {
        fun getInstance(project: Project): FunctionLocalSettingsService = project.service()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        decodeEnumsCaseInsensitive = true
        explicitNulls = false
        ignoreUnknownKeys = true
        allowTrailingComma = true
        allowComments = true
    }

    private val cache = ConcurrentHashMap<Path, CachedValue<FunctionLocalSettings>>()

    suspend fun initialize(runnableProjects: List<RunnableProject>) {
        runnableProjects
            .filter { it.kind == AzureRunnableProjectKinds.AzureFunctions }
            .forEach { getFunctionLocalSettings(it) }
    }

    suspend fun getFunctionLocalSettings(publishableProject: PublishableProjectModel) =
        getFunctionLocalSettingsInternal(Path(publishableProject.projectFilePath).parent)

    suspend fun getFunctionLocalSettings(runnableProject: RunnableProject) =
        getFunctionLocalSettingsInternal(Path(runnableProject.projectFilePath).parent)

    suspend fun getFunctionLocalSettings(projectPath: Path) =
        getFunctionLocalSettingsInternal(projectPath.parent)

    private suspend fun getFunctionLocalSettingsInternal(basePath: Path): FunctionLocalSettings? {
        val localSettingsFile = getLocalSettingFilePathInternal(basePath)
        if (!localSettingsFile.exists()) return null

        val virtualFile = withContext(Dispatchers.IO) {
            VfsUtil.findFile(localSettingsFile, true)
        } ?: return null

        val localSettings = getFunctionLocalSettings(virtualFile)
        return localSettings
    }

    fun getLocalSettingFilePath(projectPath: Path) =
        getLocalSettingFilePathInternal(projectPath.parent)

    private fun getLocalSettingFilePathInternal(basePath: Path): Path =
        basePath.resolve("local.settings.json")

    private fun getFunctionLocalSettings(localSettingsFile: VirtualFile): FunctionLocalSettings? {
        val path = localSettingsFile.toNioPath()
        val localSettings = cache.getOrPut(path) {
            val cachedValuesManager = CachedValuesManager.getManager(project)
            val provider = FunctionLocalSettingsCachedValueProvider(localSettingsFile, json)
            val cachedValue = cachedValuesManager.createCachedValue(provider, false)
            cachedValue
        }

        return localSettings.value
    }
}