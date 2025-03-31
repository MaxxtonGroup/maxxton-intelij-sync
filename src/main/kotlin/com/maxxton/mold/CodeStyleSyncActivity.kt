package com.maxxton.mold

import com.intellij.notification.Notification
import com.intellij.notification.NotificationAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.startup.ProjectActivity
import com.jetbrains.rd.util.printlnError
import git4idea.commands.Git
import git4idea.commands.GitCommand
import git4idea.commands.GitLineHandler
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Collectors
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val upToDateNotificationDuration = 1500

class CodeStyleSyncActivity : ProjectActivity {

  override suspend fun execute(project: Project) {
    println("Syncing code style...")

    try {
      syncCodeStyle()
      println("Code style sync complete")
    } catch (e: Exception) {
      printlnError("Failed to sync code style: $e")
    }
  }

  private fun syncCodeStyle() {
    val config = ApplicationManager.getApplication().getService(CodeStyleConfig::class.java)
    if (config.state.repoUrl.isBlank() && config.state.httpsRepoUrl.isBlank()) {
      showNotification(
        "Repository URL is not configured. Please set it in Settings -> Tools -> Mold.",
        NotificationType.ERROR
      )
      return
    }

    val tempDir = Files.createTempDirectory("maxxton-codestyle")
    try {
      // First try SSH URL
      if (config.state.repoUrl.isNotBlank() && tryCloneRepository(config.state.repoUrl.trim(), tempDir)) {
        updateConfigFiles(tempDir)
        return
      }

      // Fall back to HTTPS URL if SSH fails
      if (config.state.httpsRepoUrl.isNotBlank() && tryCloneRepository(
          config.state.httpsRepoUrl.trim(),
          tempDir
        )
      ) {
        updateConfigFiles(tempDir)
        return
      }

      showNotification("Failed to clone the Git repository using both SSH and HTTPS URLs", NotificationType.ERROR)
      throw IllegalStateException("Git clone failed with both URLs")
    } finally {
      tempDir.toFile().deleteRecursively()
    }
  }

  private fun tryCloneRepository(url: String, tempDir: Path): Boolean {
    if (tryCloneWithIntelliJGit(url, tempDir)) {
      println("Cloning using IntelliJ's Git API")
      return true
    }

    println("Falling back to cloning using Git command line")
    return tryCloneWithCommandLineGit(url, tempDir)
  }

  private fun tryCloneWithIntelliJGit(url: String, tempDir: Path): Boolean {
    try {
      val project = ProjectManager.getInstance().defaultProject
      val git = Git.getInstance()

      val handler = GitLineHandler(project, File(tempDir.toString()), GitCommand.CLONE)
      handler.addParameters("--depth", "1", url)
      handler.endOptions()
      handler.addParameters(tempDir.toString())

      val result = git.runCommand(handler)
      return result.success()
    } catch (e: Exception) {
      println("Exception while cloning with IntelliJ Git API using URL $url: ${e.message}")
      return false
    }
  }

  private fun tryCloneWithCommandLineGit(url: String, tempDir: Path): Boolean {
    try {
      val process = ProcessBuilder()
        .command("git", "clone", "--depth", "1", url, tempDir.toString())
        .start()

      val errorOutput = BufferedReader(InputStreamReader(process.errorStream))
        .lines()
        .collect(Collectors.joining("\n"))

      return if (process.waitFor() == 0) {
        true
      } else {
        println("Failed to clone using URL $url: $errorOutput")
        false
      }
    } catch (e: Exception) {
      println("Exception while cloning using command-line Git with URL $url: ${e.message}")
      return false
    }
  }

  private fun updateConfigFiles(tempDir: Path) {
    // Get IntelliJ config directories
    val configDirs = getConfigDirs()
    if (configDirs.isEmpty()) {
      throw IllegalStateException("Could not find any IntelliJ config directories")
    }

    val allChanges = mutableListOf<String>()

    // Apply changes to each IntelliJ installation
    configDirs.forEach { configDir ->
      // Setup directories
      val codestyleDir = configDir.resolve("codestyles").apply { createDirectories() }
      val inspectionsDir = configDir.resolve("inspection").apply { createDirectories() }

      // Copy files
      val changes = mutableListOf<String>()

      val codeStyleSource = tempDir.resolve("intellij/MaxxtonCodeStyle.xml")
      val codeStyleTarget = codestyleDir.resolve("MaxxtonCodeStyle.xml")

      if (copyIfDifferent(codeStyleSource, codeStyleTarget)) {
        changes.add("MaxxtonCodeStyle.xml in ${configDir.fileName}")
      }

      val inspectionsSource = tempDir.resolve("intellij/MaxxtonInspections.xml")
      val inspectionsTarget = inspectionsDir.resolve("MaxxtonInspections.xml")

      if (copyIfDifferent(inspectionsSource, inspectionsTarget)) {
        changes.add("MaxxtonInspections.xml in ${configDir.fileName}")
      }

      if (changes.isNotEmpty()) {
        allChanges.addAll(changes)
      }
    }

    if (allChanges.isNotEmpty()) {
      showRestartNotification(allChanges)
    } else {
      showNotification(
        "IDE Config is up-to-date!",
        NotificationType.INFORMATION,
        upToDateNotificationDuration.milliseconds
      )
    }
  }

  private fun showNotification(
    content: String,
    type: NotificationType = NotificationType.INFORMATION,
    duration: Duration = Duration.ZERO
  ) {
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

      notification.notify(null)
    }
  }

  private fun getConfigDirs(): List<Path> {
    val userHome = System.getProperty("user.home")
    val osName = System.getProperty("os.name").lowercase()

    val basePath = when {
      osName.contains("mac") -> {
        Path(userHome).resolve("Library/Application Support/JetBrains")
      }

      osName.contains("linux") -> {
        Path(userHome).resolve(".config/JetBrains")
      }

      osName.contains("windows") -> {
        Path(System.getenv("APPDATA")).resolve("JetBrains")
      }

      else -> return emptyList()
    }

    // Return all IntelliJ IDEA directories
    return basePath.listDirectoryEntries()
      .filter { it.name.startsWith("IdeaIC") || it.name.startsWith("IntelliJIdea") }
      .toList()
  }

  private fun copyIfDifferent(source: Path, target: Path): Boolean {
    val sourceContent = Files.readString(source)
    println("Source file: ${source.fileName} (${sourceContent.lines().size} lines)")

    val existingContent = getTargetContent(target)
    println("Existing file: ${target.fileName} (${existingContent.lines().size} lines)")

    val normalizedSource = sourceContent.lines().joinToString("\n") { it.trim() }
    val normalizedExisting = existingContent.lines().joinToString("\n") { it.trim() }

    if (normalizedSource != normalizedExisting) {
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

    return Files.readString(target)
  }
}
