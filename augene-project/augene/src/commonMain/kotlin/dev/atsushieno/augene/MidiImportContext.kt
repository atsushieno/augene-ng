package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
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

class MidiImportContext(val midi: MidiMusic, val edit: EditElement, val audioGraphs: List<AugeneAudioGraph>, val mappedPlugins: Map<String, Iterable<JuceAudioGraph>>) {

    companion object {
        fun create(midiFileData: ByteArray, editFileContent: String) =
            MidiImportContext(loadSmf(midiFileData), loadEdit(editFileContent), listOf(), mapOf())

        private fun loadEdit(editFileContent: String): EditElement {
            XmlTextReader(editFileContent).also {
                it.namespaces = false
                return EditModelReader().read(it)
            }
        }

        private fun loadSmf(midiFileData: ByteArray): MidiMusic {
            return MidiMusic().apply { this.read(midiFileData.toList()) }
        }
    }

    // FIXME: it is so hacky.
    fun generateNewID() : String = Random.nextInt().toString()

    var cleanupExistingTracks = true

    var markerImportStrategy = MarkerImportStrategy.Default

}

