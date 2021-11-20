package dev.atsushieno.augene

import okio.FileSystem
import okio.Path.Companion.toPath

internal actual fun pwd() : String =
    TODO("Not implemented")

internal actual fun readStringFromFileSystem(fullPath: String) =
    FileSystem.SYSTEM.read(fullPath.toPath()) { this.readUtf8() }

internal actual fun writeStringToFileSystem(fullPath: String, text: String) {
    FileSystem.SYSTEM.write(fullPath.toPath()) { this.writeUtf8(text) }
}

internal actual fun writeBinaryToFileSystem(fullPath: String, binary: ByteArray) {
    FileSystem.SYSTEM.write(fullPath.toPath()) { this.write(binary) }
}

internal actual fun canonicalizeFilePath(path: String) : String {
    return FileSystem.SYSTEM.canonicalize(path.toPath()).toString()
}

internal actual fun resolveFilePath(basePath: String, targetPath: String) : String {
    TODO("Not implemented")
}

internal actual fun fileExists(fullPath: String): Boolean =
    FileSystem.SYSTEM.exists(fullPath.toPath())
