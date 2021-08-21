package dev.atsushieno.augene

import okio.FileSystem
import okio.Path.Companion.toPath

internal actual fun readStringFromFileSystem(fullPath: String) : String =
    TODO("Not implemented") // FileSystem.SYSTEM does not exist yet!

internal actual fun writeStringToFileSystem(fullPath: String, text: String) {
    TODO("Not implemented") // FileSystem.SYSTEM does not exist yet!
}
internal actual fun canonicalizeFilePath(path: String) : String =
    TODO("Not implemented") // FileSystem.SYSTEM does not exist yet!
