# maxxton-intelij-sync

A plugin for IntelliJ IDEA that synchronizes the latest settings and code styles for our developers.
It shapes the codebase through an agreed-upon definition and helps maintain consistent code throughout projects.

## Installing the plugin

1. https://plugins.jetbrains.com/plugin/26556-mold

## Building the plugin

1. Clone or download this repository.
2. Open the project in IntelliJ IDEA
3. Run the `intellij platform -> buildPlugin` Gradle task.
4. The plugin will be available in the `./build/distributions/` directory as a `.zip` file.

## Importing the plugin

1. Open the IntelliJ IDEA plugins using the gear icon in the top right.
2. Select the gear icon next to the `Marketplace` and `Installed` buttons.
3. Select `Install Plugin from Disk...`
4. Select the `.zip` file from the `./build/distributions/` directory of this project.

## Using the plugin

1. Open the IntelliJ IDEA settings.
2. Head to Tools -> Mold 
3. Configure the correct repository URL containing the MaxxtonCodeStyle
