package com.maxxton.mold

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.rd.util.printlnError
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import kotlin.io.path.*

class CodeStyleSyncActivity : ProjectActivity {
  override suspend fun execute(project: Project) {
    try {
      syncCodeStyle()
    } catch (e: Exception) {
      printlnError("Failed to sync code style: $e")
    }
  }

  private fun syncCodeStyle() {
    val config = ApplicationManager.getApplication().getService(CodeStyleConfig::class.java)
    val tempDir = Files.createTempDirectory("maxxton-codestyle")

    try {
      // Use config.state.repoUrl instead of hardcoded value
      val process = ProcessBuilder()
        .command("git", "clone", "--depth", "1", config.state.repoUrl, tempDir.toString())
        .redirectErrorStream(true)
        .start()

      // Log output
      process.inputStream.bufferedReader().useLines { lines ->
        lines.forEach { println("Git: $it") }
      }

      if (process.waitFor() != 0) {
        throw IllegalStateException("Git clone failed")
      }

      // Get IntelliJ config directory
      val configDir = getConfigDir() ?: throw IllegalStateException("Could not find IntelliJ config directory")

      // Setup directories
      val codestyleDir = configDir.resolve("codestyles").apply { createDirectories() }
      val inspectionsDir = configDir.resolve("inspection").apply { createDirectories() }
      val optionsDir = configDir.resolve("options").apply { createDirectories() }

      // Copy files
      var hasChanged = copyIfDifferent(
        tempDir.resolve("intellij/MaxxtonCodeStyle.xml"),
        codestyleDir.resolve("MaxxtonCodeStyle.xml")
      )

      hasChanged = hasChanged || copyIfDifferent(
        tempDir.resolve("intellij/MaxxtonInspections.xml"),
        inspectionsDir.resolve("MaxxtonInspections.xml")
      )

      // Update scheme settings
      updateCodeStyleSchemes(optionsDir)
      updateEditorSettings(optionsDir)

      if (hasChanged) {
        showRestartNotification()
      }
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }

  private fun showRestartNotification() {
    ApplicationManager.getApplication().invokeLater {
      val notification = NotificationGroupManager.getInstance()
        .getNotificationGroup("Mold")
        .createNotification(
          "IDE configuration updated",
          "Please restart IntelliJ IDEA for the changes to take effect",
          NotificationType.INFORMATION
        )

      notification.addAction(object : NotificationAction("Restart now") {
        override fun actionPerformed(e: AnActionEvent, notification: Notification) {
          notification.expire()
          ApplicationManager.getApplication().restart()
        }
      })

      notification.notify(null)
    }
  }

  private fun getConfigDir(): Path? {
    val userHome = System.getProperty("user.home")
    val osName = System.getProperty("os.name").lowercase()

    return when {
      osName.contains("mac") -> {
        Path(userHome).resolve("Library/Application Support/JetBrains")
      }

      osName.contains("linux") -> {
        Path(userHome).resolve(".config/JetBrains")
      }

      osName.contains("windows") -> {
        Path(System.getenv("APPDATA")).resolve("JetBrains")
      }

      else -> null
    }?.let { basePath ->
      basePath.listDirectoryEntries()
        .filter { it.name.startsWith("IdeaIC") || it.name.startsWith("IntelliJIdea") }
        .maxByOrNull { it.name }
    }
  }

  private fun copyIfDifferent(source: Path, target: Path): Boolean {
    if (!Files.exists(target) || Files.mismatch(source, target) != -1L) {
      Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
      println("Updated ${target.fileName}")
      return true;
    }

    println("${target.fileName} is already up to date")
    return false;
  }

  private fun updateCodeStyleSchemes(optionsDir: Path) {
    val schemesFile = optionsDir.resolve("code.style.schemes.xml")
    val content = """
          <component name="CodeStyleSchemeSettings">
            <option name="CURRENT_SCHEME_NAME" value="Maxxton Code Style" />
          </component>
      """.trimIndent()
    schemesFile.writeText(content)
  }

  private fun updateEditorSettings(optionsDir: Path) {
    val editorFile = optionsDir.resolve("editor.xml")
    val content = """
          <application>
            <component name="DaemonCodeAnalyzerSettings" profile="Maxxton" />
          </application>
      """.trimIndent()
    editorFile.writeText(content)
  }
}