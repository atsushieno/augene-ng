package dev.atsushieno.augene

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter

// Partial mimick of .NET System.IO API
class IsolatedStorageFile private constructor(private val basePath: File) {
    companion object {
        // on .NET app name was unnecessary as it is per-assembly thing, but there is no such thing in Java classes.
        fun getUserStoreForAssembly(applicationDirectoryName: String) : IsolatedStorageFile {
            // FIXME: give different subdir name for Windows
            val home = System.getProperty("user.home")
            val dir = File(home).resolve(".local").resolve(applicationDirectoryName)
            if (!dir.exists())
                if (!dir.mkdirs())
                    throw IllegalStateException("Could not create application settings directory: $dir")
            return IsolatedStorageFile(dir)
        }
    }

    fun fileExists(file: String) =
        basePath.resolve(file).exists()

    fun readFileContentString(file: String) =
        FileInputStream(basePath.resolve(file)).use { InputStreamReader(it).readText() }

    fun writeFileContentString(file: String, content: String) =
        FileOutputStream(basePath.resolve(file)).use { OutputStreamWriter(it).write(content) }
}
