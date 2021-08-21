package dev.atsushieno.midi2tracktionedit

class CommandArgumentContext(val midiFileContent: ByteArray, val tracktionEditTemplateFileContent: String) {

    fun createImportContext(): MidiImportContext = MidiImportContext(this)
}
