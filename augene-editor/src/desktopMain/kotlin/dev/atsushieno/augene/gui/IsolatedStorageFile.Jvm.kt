package dev.atsushieno.augene.gui

import okio.FileSystem
import okio.Path.Companion.toPath

internal actual fun getUserStoreForAssemblyImpl(applicationDirectoryName: String) : IsolatedStorageFile {
    // FIXME: give different subdir name for Windows
    val home = System.getProperty("user.home")
    val dir = home.toPath() / ".config".toPath() / applicationDirectoryName.toPath()
    if (!FileSystem.SYSTEM.exists(dir))
        FileSystem.SYSTEM.createDirectory(dir)
    return IsolatedStorageFile(dir)
}
