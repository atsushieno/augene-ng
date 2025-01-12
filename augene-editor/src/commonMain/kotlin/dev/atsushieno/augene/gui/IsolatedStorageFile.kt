package dev.atsushieno.augene.gui

import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal expect fun getUserStoreForAssemblyImpl(applicationDirectoryName: String) : IsolatedStorageFile


// Partial mimick of .NET System.IO API
class IsolatedStorageFile
internal constructor(private val basePath: Path) {
    companion object {
        // on .NET app name was unnecessary as it is per-assembly thing, but there is no such thing in Java classes.
        fun getUserStoreForAssembly(applicationDirectoryName: String) = getUserStoreForAssemblyImpl(applicationDirectoryName)
    }

    fun fileExists(file: String) = FileSystem.SYSTEM.exists(basePath / file.toPath())

    fun readFileContentString(file: String) =
        FileSystem.SYSTEM.read(basePath / file.toPath()) { this.readUtf8() }

    fun writeFileContentString(file: String, content: String) =
        FileSystem.SYSTEM.write(basePath / file.toPath()) { this.writeUtf8(content) }
}
