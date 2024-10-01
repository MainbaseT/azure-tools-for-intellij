/*
 * Copyright 2018-2023 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

@file:OptIn(ExperimentalSerializationApi::class)

package com.microsoft.azure.toolkit.intellij.legacy.function.coreTools

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.util.net.ssl.CertificateManager
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Service(Service.Level.APP)
class FunctionCoreToolsReleaseFeedService : Disposable {
    companion object {
        fun getInstance(): FunctionCoreToolsReleaseFeedService = service()
    }

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
            })
        }
    }

    suspend fun getReleaseFeed(feedUrl: String): ReleaseFeed {
        val response = client.get(feedUrl)
        return response.body<ReleaseFeed>()
    }

    override fun dispose() = client.close()
}

@Serializable
data class ReleaseFeed(
    @SerialName("tags")
    val tags: Map<String, Tag>,
    @SerialName("releases")
    val releases: Map<String, Release>
)

@Serializable
data class Tag(
    @SerialName("release")
    val release: String?,
    @SerialName("releaseQuality")
    val releaseQuality: String?,
    @SerialName("hidden")
    val hidden: Boolean
)

@Serializable
data class Release(
    @SerialName("templates")
    val templates: String?,
    @SerialName("coreTools")
    val coreTools: List<ReleaseCoreTool>
)

@Serializable
data class ReleaseCoreTool(
    @SerialName("OS")
    val os: String?,
    @SerialName("Architecture")
    val architecture: String?,
    @SerialName("downloadLink")
    val downloadLink: String?,
    @SerialName("sha2")
    val sha2: String?,
    @SerialName("size")
    val size: String?,
    @SerialName("default")
    val default: Boolean
)