package com.microsoft.azure.toolkit.intellij.common

import com.intellij.openapi.ui.ComboBox
import com.jetbrains.rider.run.configurations.publishing.PublishRuntimeSettingsCoreHelper

fun ComboBox<PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform?>.setPublishConfiguration(
    configuration: String,
    platform: String
) {
    for (i in 0 until model.size) {
        val item = model.getElementAt(i)
        if (item?.configuration == configuration && item.platform == platform) {
            selectedItem = item
            break
        }
    }
}

fun ComboBox<PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform?>.getPublishConfiguration(): Pair<String, String> {
    val item = selectedItem as? PublishRuntimeSettingsCoreHelper.ConfigurationAndPlatform
    return (item?.configuration ?: "") to (item?.platform ?: "")
}