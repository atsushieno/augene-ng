package dev.atsushieno.augene.gui

expect fun launchExternalProcess(command: String, vararg args: String)

expect fun runFileOrFolderLauncher(fullPath: String)
