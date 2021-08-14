package dev.atsushieno.kotractive

import kotlin.test.Test
import kotlin.test.assertEquals

class EditModelReaderTest {
    @Test
    fun readTemplate() {
        val xml = """<EDIT projectID="539786/-ec0a0ae" appVersion="Waveform 10.0.26" creationTime="1629290601"><TRANSPORT /><MACROPARAMETERS id="1001" /><TEMPOSEQUENCE><TEMPO startBeat="0.0" bpm="120.0" curve="0.0" /><TIMESIG numerator="4" denominator="4" startBeat="0.0" /></TEMPOSEQUENCE><PITCHSEQUENCE><PITCH startBeat="0.0" start="0.0" pitch="60.0" /></PITCHSEQUENCE><VIDEO/><AUTOMAPXML /><CLICKTRACK level="0.6" /><ID3VORBISMETADATA trackNumber="1.0" date="2021" /><MASTERVOLUME><PLUGIN /></MASTERVOLUME><RACKS /><AUXBUSNAMES /><INPUTDEVICES /><TRACKCOMPS /><TEMPOTRACK /><MARKERTRACK trackType="0" /><CHORDTRACK /><TRACK colour="#00008000"><OUTPUTDEVICES><DEVICE name="(default audio output)" /></OUTPUTDEVICES><MIDICLIP><SEQUENCE /></MIDICLIP></TRACK></EDIT>"""

        val xr = XmlTextReader(xml)
        while (xr.read())
            println("XmlReader: ${xr.depth} ${xr.nodeType} ${xr.name} ${xr.value}")

        EditModelReader().read(XmlTextReader(xml))
    }
}
