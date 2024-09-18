/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.localsettings

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FunctionLocalSettings(
    @SerialName("IsEncrypted") val isEncrypted: Boolean?,
    @SerialName("Values") val values: FunctionValuesModel?,
    @SerialName("Host") val host: FunctionHostModel?,
    @SerialName("ConnectionStrings") val connectionStrings: Map<String, String>?
)

@Serializable
data class FunctionValuesModel(
    @SerialName("FUNCTIONS_WORKER_RUNTIME") val workerRuntime: FunctionWorkerRuntime?,
    @SerialName("AzureWebJobsStorage") val webJobsStorage: String?,
    @SerialName("AzureWebJobsDashboard") val webJobsDashboard: String?,
    @SerialName("AzureWebJobs.HttpExample.Disabled") val webJobsHttpExampleDisabled: Boolean?,
    @SerialName("MyBindingConnection") val bindingConnection: String?
)

@Serializable
data class FunctionHostModel(
    @SerialName("LocalHttpPort") val localHttpPort: Int?,
    @SerialName("CORS") val cors: String?,
    @SerialName("CORSCredentials") val corsCredentials: Boolean?
)

@Serializable
enum class FunctionWorkerRuntime {
    @SerialName("DOTNET") DOTNET {
        override fun value() = "DOTNET"
    },
    @SerialName("DOTNET-ISOLATED") DOTNET_ISOLATED {
        override fun value() = "DOTNET-ISOLATED"
    };

    abstract fun value(): String
}
