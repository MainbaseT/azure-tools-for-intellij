/*
 * Copyright 2018-2024 JetBrains s.r.o. and contributors. Use of this source code is governed by the MIT license.
 */

package com.microsoft.azure.toolkit.intellij.legacy.function.localsettings

import com.google.gson.annotations.SerializedName

//Return back when `allowComments` is available.
//@Serializable
//data class FunctionLocalSettings(
//    @SerialName("IsEncrypted") val isEncrypted: Boolean?,
//    @SerialName("Values") val values: FunctionValuesModel?,
//    @SerialName("Host") val host: FunctionHostModel?,
//    @SerialName("ConnectionStrings") val connectionStrings: Map<String, String>?
//)
//
//@Serializable
//data class FunctionValuesModel(
//    @SerialName("FUNCTIONS_WORKER_RUNTIME") val workerRuntime: FunctionWorkerRuntime?,
//    @SerialName("AzureWebJobsStorage") val webJobsStorage: String?,
//    @SerialName("AzureWebJobsDashboard") val webJobsDashboard: String?,
//    @SerialName("AzureWebJobs.HttpExample.Disabled") val webJobsHttpExampleDisabled: Boolean?,
//    @SerialName("MyBindingConnection") val bindingConnection: String?
//)
//
//@Serializable
//data class FunctionHostModel(
//    @SerialName("LocalHttpPort") val localHttpPort: Int?,
//    @SerialName("CORS") val cors: String?,
//    @SerialName("CORSCredentials") val corsCredentials: Boolean?
//)
//
//@Serializable
//enum class FunctionWorkerRuntime {
//    @SerialName("DOTNET") DOTNET {
//        override fun value() = "DOTNET"
//    },
//    @SerialName("DOTNET-ISOLATED") DOTNET_ISOLATED {
//        override fun value() = "DOTNET-ISOLATED"
//    };
//
//    abstract fun value(): String
//}

data class FunctionLocalSettings(
    @SerializedName("IsEncrypted") val isEncrypted: Boolean?,
    @SerializedName("Values") val values: FunctionValuesModel?,
    @SerializedName("Host") val host: FunctionHostModel?,
    @SerializedName("ConnectionStrings") val connectionStrings: Map<String, String>?
)

data class FunctionValuesModel(
    @SerializedName("FUNCTIONS_WORKER_RUNTIME") val workerRuntime: FunctionWorkerRuntime?,
    @SerializedName("AzureWebJobsStorage") val webJobsStorage: String?,
    @SerializedName("AzureWebJobsDashboard") val webJobsDashboard: String?,
    @SerializedName("AzureWebJobs.HttpExample.Disabled") val webJobsHttpExampleDisabled: Boolean?,
    @SerializedName("MyBindingConnection") val bindingConnection: String?
)

data class FunctionHostModel(
    @SerializedName("LocalHttpPort") val localHttpPort: Int?,
    @SerializedName("CORS") val cors: String?,
    @SerializedName("CORSCredentials") val corsCredentials: Boolean?
)

enum class FunctionWorkerRuntime {
    @SerializedName(value = "DOTNET", alternate = ["dotnet"])
    DOTNET {
        override fun value() = "DOTNET"
    },
    @SerializedName(value = "DOTNET-ISOLATED", alternate = ["dotnet-isolated"])
    DOTNET_ISOLATED {
        override fun value() = "DOTNET-ISOLATED"
    };

    abstract fun value(): String
}
