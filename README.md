# Maxxton IntelliJ Sync

A plugin for IntelliJ IDEA that synchronizes the latest settings and code styles for developers. It ensures consistent code formatting and inspections across projects.

## Usage

This is typically a one-time setup. If you encounter any issues or have suggestions for improvements, please reach out to Mischa on Slack.

### Installing the Plugin

1. Open IntelliJ IDEA CE.
2. Click the **gear icon** (⚙️) in the top-right corner.
3. Select **Plugins**.
4. In the Plugins window, click the new **gear icon** (⚙️) that appears.
5. Select **Manage Plugin Repositories**.
6. Add the following URL to the list of repositories:
   [https://raw.githubusercontent.com/MaxxtonGroup/maxxton-intelij-sync/refs/heads/main/updatePlugins.xml](https://raw.githubusercontent.com/MaxxtonGroup/maxxton-intelij-sync/refs/heads/main/updatePlugins.xml)
7. Switch to the **Marketplace** tab in the Plugins window.
8. Search for a plugin named **Mold**.
9. Install the plugin and restart IntelliJ IDEA.

### Using the Plugin

1. Open IntelliJ IDEA settings.
2. Navigate to **Editor** -> **Code Style**.
3. Set the **Scheme** to `Maxxton Code Style [MOLD]`.
4. Navigate to **Editor** -> **Inspections**.
5. Set the **Profile** to `Maxxton Inspections [MOLD]`.

### Automating Code Style and Inspections with IntelliJ Git Hooks

IntelliJ IDEA supports built-in Git hooks that can be used to enforce code style and inspections automatically on Git commits. Here's how to set it up:

1. Open IntelliJ IDEA and open the Settings menu.
2. Go to **Version Control** -> **Commit**.
3. Enable the **Cleanup** option and select the `Maxxton Inspections [MOLD]` profile.
4. Enable the **Analyze Code** option and select the `Maxxton Inspections [MOLD]` profile.
5. Click **OK** to save the settings.

With these settings enabled, IntelliJ IDEA will automatically apply the selected checks and formatting whenever you commit changes.

## Development

### Building the Plugin

1. Clone or download this repository.
2. Open the project in IntelliJ IDEA.
3. Run the Gradle task: `intellij platform -> buildPlugin`.
4. The plugin will be generated as a `.zip` file in the `./build/distributions/` directory.

### Testing the Plugin

1. Open the Plugins window in IntelliJ IDEA (⚙️ icon in the top-right corner).
2. Click the **gear icon** next to the **Marketplace** and **Installed** tabs.
3. Select **Install Plugin from Disk...**.
4. Choose the `.zip` file from the `./build/distributions/` directory of this project.
