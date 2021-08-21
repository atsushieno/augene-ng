package dev.atsushieno.augene

import okio.FileSystem
import okio.Path.Companion.toPath

internal actual fun readStringFromFileSystem(fullPath: String) =
    FileSystem.SYSTEM.read(fullPath.toPath()) { this.readUtf8() }

internal actual fun writeStringToFileSystem(fullPath: String, text: String) {
    FileSystem.SYSTEM.write(fullPath.toPath()) { this.writeUtf8(text) }
}
internal actual fun canonicalizeFilePath(path: String) : String = FileSystem.SYSTEM.canonicalize(path.toPath()).toString()
