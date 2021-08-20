package dev.atsushieno.midi2tracktionedit

import dev.atsushieno.kotractive.*
import dev.atsushieno.ktmidi.MidiMusic
import dev.atsushieno.ktmidi.read

enum class MarkerImportStrategy {
    Default,
    None,
    Global,
    PerTrack,
}

class MidiImportContext {
    constructor (commandArgumentContext: CommandArgumentContext) {
        midi = LoadSmf(commandArgumentContext.midiFileContent)
        edit = LoadEdit(commandArgumentContext.tracktionEditTemplateFileContent)
    }

    constructor(midi: MidiMusic, edit: EditElement) {
        this.midi = midi
        this.edit = edit
    }

    var cleanupExistingTracks = true

    var markerImportStrategy = MarkerImportStrategy.Default

    val midi: MidiMusic
    val edit: EditElement

    fun LoadEdit(editFileContent: String): EditElement {
        XmlTextReader(editFileContent).also {
            it.namespaces = false
            return EditModelReader().read(it)
        }
    }

    fun LoadSmf(midiFileData: ByteArray): MidiMusic {
        return MidiMusic().apply { this.read(midiFileData.toList()) }
    }
}

