package dev.atsushieno.augene.gui

actual fun launchExternalProcess(command: String, vararg args: String) {
    throw UnsupportedOperationException("Not supported on Android")
}

actual fun runFileOrFolderLauncher (fullPath: String) {
    throw UnsupportedOperationException("Not supported on Android")
}
