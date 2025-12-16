plugins {
    alias(libs.plugins.kotlin)
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()
    mavenLocal()

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
        nightly()
    }
}

val platformVersion: String by extra

dependencies {
    intellijPlatform {
        rider(platformVersion) {
            useInstaller = false
            useCache = true
        }
        jetbrainsRuntime()
        bundledPlugins("Docker")
    }

    implementation(libs.azureToolkitAuthLib)
    implementation(libs.azureToolkitIdeCommonLib)
    implementation(project(path = ":azure-intellij-plugin-lib"))
}
