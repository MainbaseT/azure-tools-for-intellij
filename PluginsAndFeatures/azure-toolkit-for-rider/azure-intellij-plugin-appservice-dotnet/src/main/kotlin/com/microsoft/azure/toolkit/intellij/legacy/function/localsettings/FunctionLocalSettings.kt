/*
 * Copyright 2018-2026 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.localsettings

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
data class FunctionLocalSettings(
    @SerialName("IsEncrypted") val isEncrypted: Boolean?,
    @SerialName("Values") val values: Map<String, JsonElement>?,
    @SerialName("Host") val host: FunctionHostModel?,
)

@Serializable
data class FunctionHostModel(
    @SerialName("LocalHttpPort") val localHttpPort: Int?,
    @SerialName("CORS") val cors: String?,
    @SerialName("CORSCredentials") val corsCredentials: Boolean?
)

fun FunctionLocalSettings.getWorkerRuntime(): FunctionWorkerRuntime? {
    val runtime = values?.get("FUNCTIONS_WORKER_RUNTIME")?.jsonPrimitive?.contentOrNull ?: return null
    return when {
        runtime.equals(FunctionWorkerRuntime.DOTNET_ISOLATED.value(), true) -> FunctionWorkerRuntime.DOTNET_ISOLATED
        runtime.equals(FunctionWorkerRuntime.DOTNET.value(), true) -> FunctionWorkerRuntime.DOTNET
        else -> null
    }
}

enum class FunctionWorkerRuntime {
    DOTNET {
        override fun value() = "DOTNET"
    },
    DOTNET_ISOLATED {
        override fun value() = "DOTNET-ISOLATED"
    };

    abstract fun value(): String
}
