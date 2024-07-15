package dev.atsushieno.augene

import dev.atsushieno.missingdot.xml.XDocument
import dev.atsushieno.missingdot.xml.XElement
import dev.atsushieno.missingdot.xml.XmlReader

class JuceAudioGraph {
    companion object {
        const val emptyAudioGraph = """<FILTERGRAPH>
<FILTER uid='5' x='0.5' y='0.1'>
<PLUGIN name='Audio Input' descriptiveName='' format='Internal' category='I/O devices' manufacturer='JUCE' version='1.0' file='' uid='246006c0' isInstrument='0' fileTime='0' infoUpdateTime='0' numInputs='0' numOutputs='4' isShell='0'/>
<STATE>0.</STATE>
<LAYOUT>
  <INPUTS><BUS index='0' layout='disabled'/></INPUTS>
  <OUTPUTS><BUS index='0' layout='disabled'/></OUTPUTS>
</LAYOUT>
</FILTER>
<FILTER uid='6' x='0.25' y='0.1'>
<PLUGIN name='Midi Input' descriptiveName='' format='Internal' category='I/O devices' manufacturer='JUCE' version='1.0' file='' uid='cb5fde0b' isInstrument='0' fileTime='0' infoUpdateTime='0' numInputs='0' numOutputs='0' isShell='0'/>
<STATE>0.</STATE>
<LAYOUT>
  <INPUTS><BUS index='0' layout='disabled'/></INPUTS>
  <OUTPUTS><BUS index='0' layout='disabled'/></OUTPUTS>
</LAYOUT>
</FILTER>
<FILTER uid='7' x='0.5' y='0.9'>
<PLUGIN name='Audio Output' descriptiveName='' format='Internal' category='I/O devices' manufacturer='JUCE' version='1.0' file='' uid='724248cb' isInstrument='0' fileTime='0' infoUpdateTime='0' numInputs='0' numOutputs='0' isShell='0'/>
<STATE>0.</STATE>
<LAYOUT>
  <INPUTS><BUS index='0' layout='L R Ls Rs'/></INPUTS>
  <OUTPUTS><BUS index='0' layout='disabled'/></OUTPUTS>
</LAYOUT>
</FILTER>
</FILTERGRAPH>
"""

        fun load (reader: XmlReader) :  Sequence<JuceAudioGraph> =
            sequence {
                val doc = XDocument.load (reader)
                val midiInFilter: (String?) -> Boolean = { s -> s != null && s.equals("Midi Input", true) }
                val audioInFilter: (String?) -> Boolean = { s -> s != null && s == "Audio Input" }
                val elementFilter: ((String?) -> Boolean) -> XElement? = { f ->
                    doc.root!!.elements("FILTER").firstOrNull { e ->
                        e.elements("PLUGIN").any { p ->
                            f(p.attribute("name")?.value) && // it is MIDI Input since Waveform11 (maybe)
                                    p.attribute("format")?.value == "Internal"
                        }
                    }
                }
                val midiInput = elementFilter(midiInFilter)
                val audioInput = elementFilter(audioInFilter)
                val output = doc.root!!.elements ("FILTER").firstOrNull { e ->
                    e.elements ("PLUGIN").any { p -> p.attribute ("name")?.value == "Audio Output" &&
                            p.attribute ("format")?.value == "Internal" }
                }
                if ((midiInput == null && audioInput == null) || output == null)
                    return@sequence
                listOf(midiInput, audioInput).forEach { input ->
                    if (input == null)
                        return@forEach
                    var uid =
                        input.attribute("uid")?.value // this on JVM results in "Debug information is inconsistent" on IDEA...?
                    while (true) {
                        val conn = doc.root!!.elements("CONNECTION").firstOrNull { e ->
                            e.attribute("srcFilter")?.value == uid
                        }
                        if (conn == null || conn == output)
                            break
                        if (uid != input.attribute("uid")?.value) {
                            val filter = doc.root!!.elements("FILTER")
                                .firstOrNull { e -> e.attribute("uid")?.value == uid } ?: return@sequence
                            val plugin = filter.element("PLUGIN") ?: continue
                            val state = filter.element("STATE")
                            val prog = plugin.attribute("programNum")
                            yield(JuceAudioGraph().apply {
                                file = plugin.attribute("file")?.value
                                category = plugin.attribute("category")?.value
                                manufacturer = plugin.attribute("manufacturer")?.value
                                name = plugin.attribute("name")?.value
                                this.uid = plugin.attribute("uid")?.value
                                programNum = prog?.value?.toInt() ?: 0
                                this.state = state?.value
                            })
                        }
                        uid = conn.attribute("dstFilter")?.value
                    }
                }
            }

        fun toAudioGraph (audioGraph: Iterable<JuceAudioGraph> ) : Iterable<AugenePluginSpecifier> =
            audioGraph.map { src -> AugenePluginSpecifier().apply {
                type = src.format
                uid = src.uid
                filename = src.file
                name = src.name
                manufacturer = src.manufacturer
                programNum = src.programNum
                state = src.state
            } }
    }

    //<PLUGIN name="Midi Input" descriptiveName="" format="Internal" category="I/O devices"
    //manufacturer="JUCE" version="1.0" file="" uid="cb5fde0b" isInstrument="0"
    //fileTime="0" infoUpdateTime="0" numInputs="0" numOutputs="0"
    //isShell="0"/>
    //<STATE>0.</STATE>
    //
    // For audio plugins there is something like `type="vst" ` too.
    var name : String? = null
    var descriptiveName : String? = null
    var format : String? = null
    var category : String? = null
    var manufacturer : String? = null
    var version : String? = null
    var file : String? = null
    var uid : String? = null
    var isInstrument = 0
    var fileTime = 0L
    var infoUpdateTime = 0L
    var numInputs = 0
    var numOutputs = 0
    var isShell = 0 // not Boolean?
    var state : String? = null
    var programNum = 0 // ?
}

