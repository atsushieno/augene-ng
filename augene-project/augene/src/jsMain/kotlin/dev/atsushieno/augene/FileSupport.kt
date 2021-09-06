package dev.atsushieno.augene

import okio.FileSystem
import okio.Path.Companion.toPath

internal actual fun pwd() : String =
    TODO("Not implemented") // FileSystem.SYSTEM does not exist yet!

internal actual fun readStringFromFileSystem(fullPath: String) : String =
    TODO("Not implemented") // FileSystem.SYSTEM does not exist yet!

internal actual fun writeStringToFileSystem(fullPath: String, text: String) {
    TODO("Not implemented") // FileSystem.SYSTEM does not exist yet!
}
internal actual fun writeBinaryToFileSystem(fullPath: String, binary: ByteArray) {
    TODO("Not implemented") // FileSystem.SYSTEM does not exist yet!
}
internal actual fun canonicalizeFilePath(path: String) : String =
    TODO("Not implemented") // FileSystem.SYSTEM does not exist yet!

internal actual fun resolveFilePath(basePath: String, targetPath: String) : String =
    TODO("Not implemented") // FileSystem.SYSTEM does not exist yet!
