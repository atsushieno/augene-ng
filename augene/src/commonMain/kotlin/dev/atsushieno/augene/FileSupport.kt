package dev.atsushieno.augene

import okio.Path.Companion.toPath

internal expect fun pwd() : String
internal expect fun readStringFromFileSystem(fullPath: String) : String
internal expect fun writeStringToFileSystem(fullPath: String, text: String)
internal expect fun writeBinaryToFileSystem(fullPath: String, binary: ByteArray)
internal expect fun canonicalizeFilePath(path: String) : String
internal expect fun resolveFilePath(basePath: String, targetPath: String) : String
internal expect fun fileExists(fullPath: String): Boolean

class FileSupport(baseFileName: String = "dummy") {

    private val baseDir = canonicalizeFilePath(resolveFilePath(pwd(), baseFileName.toPath().parent.toString()))

    companion object {
        fun canonicalizePath(path: String) = canonicalizeFilePath(path)
    }

    fun resolvePathRelativeToProject (pathSpec: String) : String =
        resolveFilePath(baseDir, pathSpec)

    private val absPath = { s:String? -> resolvePathRelativeToProject(s!!) }

    fun readString(file: String) =  readStringFromFileSystem(absPath (file))

    fun writeString(file: String, text: String) = writeStringToFileSystem(absPath(file), text)

    fun writeBytes(file: String, binary: ByteArray) = writeBinaryToFileSystem(absPath(file), binary)

    fun exists(file: String) = fileExists(absPath(file))
}
