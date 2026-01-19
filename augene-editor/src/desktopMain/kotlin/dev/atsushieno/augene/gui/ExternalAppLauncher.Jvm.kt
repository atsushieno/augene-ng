package dev.atsushieno.augene.gui

import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toLowerCase
import java.awt.Desktop
import java.io.File

actual fun launchExternalProcess(command: String, vararg args: String) {
    if (File(command).isDirectory) // Apple
        Desktop.getDesktop().open(File(command))
    else
        ProcessBuilder(listOf(command).plus(args)).start()
}

actual fun runFileOrFolderLauncher (fullPath: String) {
    val os = System.getProperty("os.name").toLowerCase(Locale.current)
    if (os.contains("windows"))
        launchExternalProcess("explorer", fullPath)
    if (os.contains("mac"))
        launchExternalProcess("open", fullPath)
    else
        launchExternalProcess("xdg-open", fullPath)
}
