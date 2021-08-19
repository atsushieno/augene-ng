package dev.atsushieno.augene

import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.Path.Companion.toPath

class Files(private val baseFileName: String) {

    @OptIn(ExperimentalFileSystem::class)
    fun resolvePathRelativetoProject (pathSpec: String) : String =
        (baseFileName.toPath() / pathSpec).toString()

    val absPath = { s:String? -> resolvePathRelativetoProject(s!!) }

    @OptIn(ExperimentalFileSystem::class)
    fun readString(file: String) =  FileSystem.SYSTEM.read(absPath (file).toPath()) { this.readUtf8() }

    @OptIn(ExperimentalFileSystem::class)
    fun writeString(file: String, text: String) = FileSystem.SYSTEM.write(file.toPath()) { this.writeUtf8(text) }
}