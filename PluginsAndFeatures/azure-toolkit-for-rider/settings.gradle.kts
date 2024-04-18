rootProject.name = "azure-toolkit-for-rider"

pluginManagement {
    repositories {
        maven { setUrl("https://cache-redirector.jetbrains.com/plugins.gradle.org") }
        gradlePluginPortal()
        // This is for snapshot version of 'org.jetbrains.intellij' plugin
        maven { setUrl("https://oss.sonatype.org/content/repositories/snapshots/") }
    }
    resolutionStrategy {
        eachPlugin {
            when(requested.id.name) {
                // This required to correctly rd-gen plugin resolution. May be we should switch our naming to match Gradle plugin naming convention.
                "rdgen" -> {
                    useModule("com.jetbrains.rd:rd-gen:${requested.version}")
                }
            }
        }
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}

include("protocol")
include("azure-intellij-plugin-resharper-host")
include(":azure-intellij-plugin-lib")
project(":azure-intellij-plugin-lib").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-lib")
include(":azure-intellij-plugin-lib-dotnet")
include(":azure-intellij-plugin-guidance")
project(":azure-intellij-plugin-guidance").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-guidance")
include(":azure-intellij-resource-connector-lib")
project(":azure-intellij-resource-connector-lib").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-resource-connector-lib")
include(":azure-intellij-plugin-service-explorer")
project(":azure-intellij-plugin-service-explorer").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-service-explorer")
include(":azure-intellij-plugin-arm")
project(":azure-intellij-plugin-arm").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-arm")
include(":azure-intellij-plugin-appservice")
project(":azure-intellij-plugin-appservice").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-appservice")
include(":azure-intellij-plugin-appservice-dotnet")
include(":azure-intellij-plugin-monitor")
project(":azure-intellij-plugin-monitor").projectDir = file("../azure-toolkit-for-intellij/azure-intellij-plugin-monitor")
