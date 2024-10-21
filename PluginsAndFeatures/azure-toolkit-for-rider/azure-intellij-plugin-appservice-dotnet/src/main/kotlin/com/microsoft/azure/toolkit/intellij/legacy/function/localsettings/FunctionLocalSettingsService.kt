/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.localsettings

import com.google.gson.Gson
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.readText
import com.jetbrains.rider.model.PublishableProjectModel
import com.jetbrains.rider.model.RunnableProject
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import kotlin.io.path.Path
import kotlin.io.path.absolutePathString
import kotlin.io.path.exists

@Service(Service.Level.PROJECT)
class FunctionLocalSettingsService {
    companion object {
        fun getInstance(project: Project): FunctionLocalSettingsService = project.service()
    }

    @OptIn(ExperimentalSerializationApi::class)
    private val json = Json {
        decodeEnumsCaseInsensitive = true
        explicitNulls = false
        ignoreUnknownKeys = true
        allowTrailingComma = true
    }

    private val cache = ConcurrentHashMap<String, Pair<Long, FunctionLocalSettings>>()

    fun initialize(runnableProjects: List<RunnableProject>) {
        runnableProjects.forEach {
            val localSettingsFile = getLocalSettingFilePathInternal(Path(it.projectFilePath).parent)
            if (!localSettingsFile.exists()) return@forEach
            val pathString = localSettingsFile.absolutePathString()
            if (cache.containsKey(pathString)) return@forEach
            val virtualFile = VfsUtil.findFile(localSettingsFile, true) ?: return@forEach
            val localSettingsFileStamp = localSettingsFile.toFile().lastModified()
            val localSettings = getFunctionLocalSettings(virtualFile)
            cache[pathString] = Pair(localSettingsFileStamp, localSettings)
        }
    }

    fun getFunctionLocalSettings(publishableProject: PublishableProjectModel) =
        getFunctionLocalSettingsInternal(Path(publishableProject.projectFilePath).parent)

    fun getFunctionLocalSettings(runnableProject: RunnableProject) =
        getFunctionLocalSettingsInternal(Path(runnableProject.projectFilePath).parent)

    fun getFunctionLocalSettings(projectPath: Path) =
        getFunctionLocalSettingsInternal(projectPath.parent)

    private fun getFunctionLocalSettingsInternal(basePath: Path): FunctionLocalSettings? {
        val localSettingsFile = getLocalSettingFilePathInternal(basePath)
        if (!localSettingsFile.exists()) return null

        val localSettingsFileStamp = localSettingsFile.toFile().lastModified()
        val pathString = localSettingsFile.absolutePathString()
        val existingLocalSettings = cache[pathString]
        if (existingLocalSettings == null || localSettingsFileStamp != existingLocalSettings.first) {
            val virtualFile = VfsUtil.findFile(localSettingsFile, true) ?: return null
            val localSettings = getFunctionLocalSettings(virtualFile)
            cache[pathString] = Pair(localSettingsFileStamp, localSettings)
            return localSettings
        }

        return existingLocalSettings.second
    }

    fun getLocalSettingFilePath(projectPath: Path) =
        getLocalSettingFilePathInternal(projectPath.parent)

    private fun getLocalSettingFilePathInternal(basePath: Path): Path = basePath.resolve("local.settings.json")

    private fun getFunctionLocalSettings(localSettingsFile: VirtualFile): FunctionLocalSettings {
        val content = localSettingsFile.readText()
        //Return back when `allowComments` is available.
        //return json.decodeFromString<FunctionLocalSettings>(content)
        return Gson().fromJson(content, FunctionLocalSettings::class.java)
    }
}