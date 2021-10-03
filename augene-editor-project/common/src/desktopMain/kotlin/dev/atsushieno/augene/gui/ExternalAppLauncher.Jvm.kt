package dev.atsushieno.augene.gui

actual fun launchExternalProcess(command: String, vararg args: String) {
    ProcessBuilder(listOf(command).plus(args)).start()
}

actual fun runFileOrFolderLauncher (fullPath: String) {
    val os = System.getProperty("os.name")
    if (os.contains("windows"))
        launchExternalProcess("explorer", fullPath)
    if (os.contains("mac"))
        launchExternalProcess("open", fullPath)
    else
        launchExternalProcess("xdg-open", fullPath)
}
