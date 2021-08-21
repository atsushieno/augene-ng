package dev.atsushieno.augene

class CommandArgumentContext(val midiFileContent: ByteArray, val tracktionEditTemplateFileContent: String) {

    fun createImportContext(): MidiImportContext = MidiImportContext(this)
}
