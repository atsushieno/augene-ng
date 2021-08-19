package java.nio.file

import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import kotlin.io.path.name

class Files {
    companion object {
        fun readString(file: Path) : String {
            return FileInputStream(file.name).use { stream ->
                InputStreamReader(stream).use {
                    it.readText()
                }
            }
        }

        fun writeString(file: Path, text: String) {
            FileOutputStream(file.name).use { stream ->
                OutputStreamWriter(stream).use {
                    it.write(text)
                }
            }
        }
    }
}

