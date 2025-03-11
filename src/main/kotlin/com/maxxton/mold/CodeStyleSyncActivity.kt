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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val upToDateNotificationDuration = 1500

class CodeStyleSyncActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    println("Syncing code style...")

    try {
      syncCodeStyle(project)
      println("Code style sync complete")
    } catch (e: Exception) {
      printlnError("Failed to sync code style: $e")
    }
  }

  private fun syncCodeStyle(project: Project) {
    val config = ApplicationManager.getApplication().getService(CodeStyleConfig::class.java)

    if (config.state.repoUrl.isBlank()) {
      showNotification(
        "Repository URL is not configured. Please set it in Settings -> Tools -> Mold.",
        NotificationType.ERROR
      )
      return
    }

    val tempDir = Files.createTempDirectory("maxxton-codestyle")

    try {
      val process = ProcessBuilder()
        .command("git", "clone", "--depth", "1", config.state.repoUrl.trim(), tempDir.toString())
        .start()

      val errorOutput = BufferedReader(InputStreamReader(process.errorStream))
        .lines()
        .collect(Collectors.joining("\n"))

      if (process.waitFor() != 0) {
        showNotification("Failed to clone the Git repository: $errorOutput", NotificationType.ERROR)
        throw IllegalStateException("Git clone failed")
      }

      updateConfigFiles(tempDir)
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }

  private fun updateConfigFiles(tempDir: Path) {
    // Get IntelliJ config directory
    val configDir = getConfigDir() ?: throw IllegalStateException("Could not find IntelliJ config directory")

    // Setup directories
    val codestyleDir = configDir.resolve("codestyles").apply { createDirectories() }
    val inspectionsDir = configDir.resolve("inspection").apply { createDirectories() }
    val optionsDir = configDir.resolve("options").apply { createDirectories() }

    // Copy files
    val changes = mutableListOf<String>()

    if (copyIfDifferent(tempDir.resolve("intellij/MaxxtonCodeStyle.xml"), codestyleDir.resolve("MaxxtonCodeStyle.xml"))) {
      changes.add("MaxxtonCodeStyle.xml")
    }

    if (copyIfDifferent(tempDir.resolve("intellij/MaxxtonInspections.xml"), inspectionsDir.resolve("MaxxtonInspections.xml"))) {
      changes.add("MaxxtonInspections.xml")
    }

    if (changes.isNotEmpty()) {
      updateCodeStyleSchemes(optionsDir)
      updateInspections(optionsDir)
      showRestartNotification(changes)
    } else {
      showNotification("IDE Config is up-to-date!", NotificationType.INFORMATION, upToDateNotificationDuration.milliseconds)
    }
  }

  private fun showNotification(content: String, type: NotificationType = NotificationType.INFORMATION, duration: Duration = Duration.ZERO) {
    ApplicationManager.getApplication().invokeLater {
      val notification = NotificationGroupManager.getInstance()
        .getNotificationGroup("Mold")
        .createNotification(content, type)

      notification.notify(null)

      // auto-expire the notification when a duration is specified
      if (duration.inWholeMilliseconds > 0) {
        ApplicationManager.getApplication().executeOnPooledThread {
          Thread.sleep(duration.inWholeMilliseconds)
          notification.expire()
        }
      }
    }
  }

  private fun showRestartNotification(changes: List<String>) {
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

      if (changes.isNotEmpty()) {
        notification.addAction(object : NotificationAction("View changes") {
          override fun actionPerformed(e: AnActionEvent, notification: Notification) {
            notification.expire()
            showNotification("Updated files: ${changes.joinToString(", ")}")
          }
        })
      }

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
    val sourceContent = Files.readString(source).replace("\r\n", "\n")
    val targetContent = getTargetContent(target)

    if (sourceContent != targetContent) {
      Files.writeString(target, sourceContent)
      println("Updated ${target.fileName}")
      return true
    }

    println("${target.fileName} is already up to date")
    return false
  }

  private fun getTargetContent(target: Path): String {
    if (Files.notExists(target)) {
      return ""
    }

    return Files.readString(target).replace("\r\n", "\n")
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

  private fun updateInspections(optionsDir: Path) {
    val editorFile = optionsDir.resolve("editor.xml")
    val content = """
          <application>
            <component name="DaemonCodeAnalyzerSettings" profile="Maxxton" />
          </application>
      """.trimIndent()
    editorFile.writeText(content)
  }
}
