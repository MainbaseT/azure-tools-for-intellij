dependencies {
    implementation(project(":azure-intellij-plugin-lib"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-lib-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-lib-java", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-guidance"))
    // runtimeOnly project(path: ":azure-intellij-plugin-guidance", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-guidance-java"))
    // runtimeOnly project(path: ":azure-intellij-plugin-guidance-java", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-applicationinsights"))
    // runtimeOnly project(path: ":azure-intellij-plugin-applicationinsights", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-monitor"))
    // runtimeOnly project(path: ":azure-intellij-plugin-monitor", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-plugin-containerregistry"))
    // runtimeOnly project(path: ":azure-intellij-plugin-containerregistry", configuration: "instrumentedJar")
    implementation(project(":azure-intellij-resource-connector-lib"))
    // runtimeOnly project(path: ":azure-intellij-resource-connector-lib", configuration: "instrumentedJar")
    implementation("com.github.docker-java:docker-java:3.3.0")
    implementation("com.microsoft.azure:azure-toolkit-containerapps-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-containerregistry-lib")
    implementation("com.microsoft.azure:azure-toolkit-containerregistry-lib")
    implementation("com.microsoft.azure:azure-toolkit-ide-containerapps-lib")

    intellijPlatform {
        // Plugin Dependencies. Uses `platformBundledPlugins` property from the gradle.properties file for bundled IntelliJ Platform plugins.
        bundledPlugin("com.intellij.java")
        bundledPlugin("org.jetbrains.idea.maven")
        bundledPlugin("com.intellij.gradle")
    }
}
