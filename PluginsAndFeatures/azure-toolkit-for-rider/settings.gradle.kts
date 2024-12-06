rootProject.name = "azure-toolkit-for-rider"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven("https://oss.sonatype.org/content/repositories/snapshots/")
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.jetbrains.rdgen") {
                useModule("com.jetbrains.rd:rd-gen:${requested.version}")
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

fun intellijModule(name: String) {
    include(":$name")
    project(":$name").projectDir = file("../azure-toolkit-for-intellij/$name")
}

include(":protocol")
include(":azure-intellij-plugin-resharper-host")
intellijModule("azure-intellij-plugin-lib")
include(":azure-intellij-plugin-lib-dotnet")
intellijModule("azure-intellij-plugin-guidance")
intellijModule("azure-intellij-resource-connector-lib")
intellijModule("azure-intellij-plugin-service-explorer")
intellijModule("azure-intellij-plugin-arm")
intellijModule("azure-intellij-plugin-monitor")
intellijModule("azure-intellij-plugin-appservice")
include(":azure-intellij-plugin-appservice-dotnet")
intellijModule("azure-intellij-plugin-database")
include(":azure-intellij-plugin-database-dotnet")
intellijModule("azure-intellij-plugin-redis")
include(":azure-intellij-plugin-redis-dotnet")
intellijModule("azure-intellij-plugin-storage")
include(":azure-intellij-plugin-storage-dotnet")
intellijModule("azure-intellij-plugin-keyvault")
include(":azure-intellij-plugin-keyvault-dotnet")
include(":azure-intellij-plugin-cloud-shell")
intellijModule("azure-intellij-plugin-servicebus")
intellijModule("azure-intellij-plugin-eventhubs")
intellijModule("azure-intellij-plugin-vm")
