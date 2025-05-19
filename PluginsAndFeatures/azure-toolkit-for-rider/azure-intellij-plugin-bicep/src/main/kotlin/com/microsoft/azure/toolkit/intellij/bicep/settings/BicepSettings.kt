package com.microsoft.azure.toolkit.intellij.bicep.settings

import com.intellij.openapi.components.*
import com.intellij.openapi.project.Project

@Service(Service.Level.PROJECT)
@State(name = "BicepSettings", storages = [Storage(StoragePathMacros.WORKSPACE_FILE)])
internal class BicepSettings : SimplePersistentStateComponent<BicepSettingsState>(BicepSettingsState()) {
  companion object {
    fun getInstance(project: Project) = project.service<BicepSettings>()
  }

  fun rememberToIgnoreLsDownloadSuggestion() {
    state.ignoreLsDownloadSuggestion = true
  }

  fun shouldIgnoreLsDownloadSuggestion(): Boolean {
    return state.ignoreLsDownloadSuggestion
  }
}

internal class BicepSettingsState : BaseState() {
  var ignoreLsDownloadSuggestion: Boolean by property(false)
}
