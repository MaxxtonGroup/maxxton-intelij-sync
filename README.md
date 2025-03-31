# maxxton-intelij-sync

A plugin for IntelliJ IDEA that synchronizes the latest settings and code styles for our developers.
It shapes the codebase through an agreed-upon definition and helps maintain consistent code throughout projects.


## Usage

This should be a one-time process.
If you need help or something does not quite work, or if you see any improvements, please reach out to me (Mischa) on Slack.

### Installing the plugin

1. Open IntelliJ IDEA CE.
2. Click the gear (settings) icon in the top right.
3. Select `Plugins`.
4. Click the `gear` icon in the plugins popup.
5. Select `Manage Plugin Repositories`.
6. Copy [https://raw.githubusercontent.com/MaxxtonGroup/maxxton-intelij-sync/refs/heads/main/updatePlugins.xml](https://raw.githubusercontent.com/MaxxtonGroup/maxxton-intelij-sync/refs/heads/main/updatePlugins.xml) 
7. Click the `+` icon and paste the copied URL.
8. Switch to the `Marketplace` tab of the `Plugins` tab.
9. Search for a plugin called `Mold` - if you can't find it try searching for this instead: `/repository:"https://raw.githubusercontent.com/MaxxtonGroup/maxxton-intelij-sync/refs/heads/main/updatePlugins.xml"`
10. Install it and restart IntelliJ.

### Using the plugin

1. Open the IntelliJ IDEA settings.
2. Navigate to `Editor` -> `Code Style`
3. Set the `Scheme` to `Maxxton Code Style [Mold]`
4. Navigate to `Editor` -> `Inspections`
5. Set the `Profile` to `Maxxton Inspections [Mold]`


## Development

### Building the plugin

1. Clone or download this repository.
2. Open the project in IntelliJ IDEA
3. Run the `intellij platform -> buildPlugin` Gradle task.
4. The plugin will be available in the `./build/distributions/` directory as a `.zip` file.

### Testing the plugin

1. Open the IntelliJ IDEA plugins using the gear icon in the top right.
2. Select the gear icon next to the `Marketplace` and `Installed` buttons.
3. Select `Install Plugin from Disk...`
4. Select the `.zip` file from the `./build/distributions/` directory of this project.
