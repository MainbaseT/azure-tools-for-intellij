/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.toolingFeed

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

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
    val coreTools: List<CoreToolsRelease>
)

@Serializable
data class CoreToolsRelease(
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