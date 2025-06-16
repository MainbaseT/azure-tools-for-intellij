import com.jetbrains.rd.generator.gradle.RdGenTask

plugins {
    alias(libs.plugins.kotlin)
    id("com.jetbrains.rdgen") version libs.versions.rdGen
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(libs.rdGen)
    implementation(libs.kotlinStdLib)
    implementation(
        project(
            mapOf(
                "path" to ":",
                "configuration" to "riderModel"
            )
        )
    )
}

rdgen {
    val pluginSourcePath = projectDir.resolve("..")
    val ktOutput = pluginSourcePath.resolve("azure-intellij-plugin-resharper-host/src/main/kotlin/org/jetbrains/protocol")
    val csOutput = pluginSourcePath.resolve("src/dotnet/ReSharper.Azure/Azure.Daemon/Protocol")

    verbose = true
    packages = "model.daemon"

    generator {
        language = "kotlin"
        transform = "asis"
        root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
        namespace = "com.jetbrains.rider.azure.model"
        directory = ktOutput.canonicalPath
    }

    generator {
        language = "csharp"
        transform = "reversed"
        root = "com.jetbrains.rider.model.nova.ide.IdeRoot"
        namespace = "JetBrains.Rider.Azure.Model"
        directory = csOutput.canonicalPath
    }
}

tasks.withType<RdGenTask> {
    val classPath = sourceSets["main"].runtimeClasspath
    dependsOn(classPath)
    classpath(classPath)
}