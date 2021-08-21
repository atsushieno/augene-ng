package dev.atsushieno.augene

import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath

internal expect fun readStringFromFileSystem(fullPath: String) : String
internal expect fun writeStringToFileSystem(fullPath: String, text: String)
internal expect fun canonicalizeFilePath(path: String) : String

class FileSupport(private val baseFileName: String) {
    companion object {
        fun canonicalizePath(path: String) = canonicalizeFilePath(path)
    }

    @OptIn(ExperimentalFileSystem::class)
    fun resolvePathRelativeToProject (pathSpec: String) : String =
        (baseFileName.toPath() / pathSpec).toString()

    val absPath = { s:String? -> resolvePathRelativeToProject(s!!) }

    @OptIn(ExperimentalFileSystem::class)
    fun readString(file: String) =  readStringFromFileSystem(absPath (file))

    @OptIn(ExperimentalFileSystem::class)
    fun writeString(file: String, text: String) = writeStringToFileSystem(absPath(file), text)
}
