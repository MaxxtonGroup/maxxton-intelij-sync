package com.maxxton.mold.settings

import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.FormBuilder
import javax.swing.JPanel

class CodeStyleSettingsComponent {
    private val repoUrlField = JBTextField()
    private val mainPanel: JPanel = FormBuilder.createFormBuilder()
        .addLabeledComponent(JBLabel("Repository URL: "), repoUrlField, 1, false)
        .addComponentFillVertically(JPanel(), 0)
        .panel

    fun getPanel(): JPanel = mainPanel

    fun getRepoUrl(): String = repoUrlField.text

    fun setRepoUrl(url: String) {
        repoUrlField.text = url
    }
}
