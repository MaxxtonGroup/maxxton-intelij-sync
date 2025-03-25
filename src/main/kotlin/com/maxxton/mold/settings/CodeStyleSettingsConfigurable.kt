package com.maxxton.mold.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.Configurable
import com.maxxton.mold.CodeStyleConfig
import javax.swing.JComponent

class CodeStyleSettingsConfigurable : Configurable {
  private var settingsComponent: CodeStyleSettingsComponent? = null

  override fun getDisplayName() = "Mold"

  override fun createComponent(): JComponent {
    settingsComponent = CodeStyleSettingsComponent()
    return settingsComponent!!.getPanel()
  }

  override fun isModified(): Boolean {
    val settings = ApplicationManager.getApplication().getService(CodeStyleConfig::class.java)
    return settingsComponent?.getRepoUrl() != settings.state.repoUrl ||
        settingsComponent?.getHttpsRepoUrl() != settings.state.httpsRepoUrl
  }

  override fun apply() {
    val settings = ApplicationManager.getApplication().getService(CodeStyleConfig::class.java)
    settings.state.repoUrl = settingsComponent?.getRepoUrl() ?: return
    settings.state.httpsRepoUrl = settingsComponent?.getHttpsRepoUrl() ?: return
  }

  override fun reset() {
    val settings = ApplicationManager.getApplication().getService(CodeStyleConfig::class.java)
    settingsComponent?.setRepoUrl(settings.state.repoUrl)
    settingsComponent?.setHttpsRepoUrl(settings.state.httpsRepoUrl)
  }

  override fun disposeUIResources() {
    settingsComponent = null
  }
}
