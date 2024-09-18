/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*

@Service(Service.Level.APP)
class FunctionCoreToolsReleaseFeedService : Disposable {
    companion object {
        fun getInstance(): FunctionCoreToolsReleaseFeedService = service()
    }

    private val client = HttpClient(CIO)

    suspend fun getReleaseFeed(feedUrl: String): ReleaseFeed {
        val body: String = client.get(feedUrl).body()
        return GsonBuilder().create().fromJson(body, ReleaseFeed::class.java)
    }

    override fun dispose() = client.close()
}

data class ReleaseFeed(
    @SerializedName("tags")
    val tags: Map<String, Tag>,
    @SerializedName("releases")
    val releases: Map<String, Release>
)

data class Tag(
    @SerializedName("release")
    val release: String?,
    @SerializedName("releaseQuality")
    val releaseQuality: String?,
    @SerializedName("hidden")
    val hidden: Boolean
)

data class Release(
    @SerializedName("templates")
    val templates: String?,
    @SerializedName("coreTools")
    val coreTools: List<ReleaseCoreTool>
)

data class ReleaseCoreTool(
    @SerializedName("OS")
    val os: String?,
    @SerializedName("Architecture")
    val architecture: String?,
    @SerializedName("downloadLink")
    val downloadLink: String?,
    @SerializedName("sha2")
    val sha2: String?,
    @SerializedName("size")
    val size: String?,
    @SerializedName("default")
    val default: Boolean
)