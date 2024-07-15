package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
import dev.atsushieno.ktmidi.Midi2Music
import dev.atsushieno.ktmidi.MidiMusic
import dev.atsushieno.ktmidi.read
import dev.atsushieno.missingdot.xml.XmlTextReader
import kotlin.random.Random

enum class MarkerImportStrategy {
    Default,
    None,
    Global,
    PerTrack,
}

class Midi2ToTracktionImportContext(val midi: Midi2Music, val edit: EditElement, val audioGraphs: List<AugeneAudioGraph>, val mappedPlugins: Map<String, Iterable<JuceAudioGraph>>) {

    companion object {
        fun create(midiFileData: ByteArray, editFileContent: String) =
            Midi2ToTracktionImportContext(loadSmf(midiFileData), loadEdit(editFileContent), listOf(), mapOf())

        private fun loadEdit(editFileContent: String): EditElement {
            XmlTextReader(editFileContent).also {
                it.namespaces = false
                return EditModelReader().read(it)
            }
        }

        private fun loadSmf(midiFileData: ByteArray): Midi2Music {
            return Midi2Music().apply { this.read(midiFileData.toList()) }
        }
    }

    // FIXME: it is so hacky.
    fun generateNewID() : String = Random.nextInt().toString()

    var cleanupExistingTracks = true

    var markerImportStrategy = MarkerImportStrategy.Default

    var reporter: (message: String, source: String?, line: Int, column: Int) -> Unit = { message, source, line, column ->
            println(message)
            if (source != null)
                println("  at $source ($line, $column)")
        }

    fun report(message: String, source: String? = null, line: Int = 0, column: Int = 0) =
        reporter(message, source, line, column)
}
