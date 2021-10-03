package dev.atsushieno.augene.gui

import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toPath

internal expect fun getUserStoreForAssemblyImpl(applicationDirectoryName: String) : IsolatedStorageFile


// Partial mimick of .NET System.IO API
class IsolatedStorageFile @OptIn(ExperimentalFileSystem::class)
internal constructor(private val basePath: Path) {
    companion object {
        // on .NET app name was unnecessary as it is per-assembly thing, but there is no such thing in Java classes.
        @OptIn(ExperimentalFileSystem::class)
        fun getUserStoreForAssembly(applicationDirectoryName: String) = getUserStoreForAssemblyImpl(applicationDirectoryName)
    }

    @OptIn(ExperimentalFileSystem::class)
    fun fileExists(file: String) = FileSystem.SYSTEM.exists(basePath / file.toPath())

    @OptIn(ExperimentalFileSystem::class)
    fun readFileContentString(file: String) =
        FileSystem.SYSTEM.read(basePath / file.toPath()) { this.readUtf8() }

    @OptIn(ExperimentalFileSystem::class)
    fun writeFileContentString(file: String, content: String) =
        FileSystem.SYSTEM.write(basePath / file.toPath()) { this.writeUtf8(content) }
}
