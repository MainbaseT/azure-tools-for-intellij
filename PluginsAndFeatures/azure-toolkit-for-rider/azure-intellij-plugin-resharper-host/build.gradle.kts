import org.jetbrains.intellij.platform.gradle.extensions.intellijPlatform

plugins {
    alias(libs.plugins.kotlin)
    id("org.jetbrains.intellij.platform.module")
}

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
        jetbrainsRuntime()
    }
}

val platformVersion: String by extra

dependencies {
    intellijPlatform {
        rider(platformVersion, false)
        jetbrainsRuntime()
        instrumentationTools()
    }
}
