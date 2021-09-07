package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
import dev.atsushieno.ktmidi.GeneralMidi
import dev.atsushieno.ktmidi.MidiChannelStatus
import dev.atsushieno.ktmidi.MidiEvent
import dev.atsushieno.ktmidi.MidiMessage
import dev.atsushieno.ktmidi.MidiMetaType
import dev.atsushieno.ktmidi.MidiTrack
import dev.atsushieno.ktmidi.mergeTracks
import dev.atsushieno.missingdot.xml.XmlReader
import kotlin.math.pow

private val SMF_SYSEX_EVENT = 0xF0
private val SMF_META_EVENT = 0xFF
internal fun Byte.toUnsigned() : Int = if (this < 0) this + 0x100 else this.toInt()


class MidiToTracktionEditConverter(private var context: MidiImportContext) {
    // state
    private var consumed = false
    private var globalMarkers = arrayOf(MidiMessage(Int.MAX_VALUE, MidiEvent(0)))

    fun process(midiFileContent: ByteArray, tracktionEditFileContent: String) : String {
        context = MidiImportContext.create(midiFileContent, tracktionEditFileContent)
        importMusic()
        val sb = StringBuilder()
        EditModelWriter().write(sb, context.edit)
        return sb.toString()
    }

    fun importMusic() {
        if (consumed)
            throw IllegalArgumentException("This instance is already used. Create another instance of ${this::class} if you want to process more.")
        consumed = true
        if (context.cleanupExistingTracks) {
            context.edit.Tracks.clear()
            context.edit.TempoSequence = null
        }
        if (context.edit.TempoSequence == null)
            context.edit.TempoSequence = TempoSequenceElement()

        when (context.markerImportStrategy) {
            MarkerImportStrategy.Global -> {
                //case MarkerImportStrategy.Default:
                val markers = mutableListOf<MidiMessage>()
                var t = 0
                for (m in context.midi.mergeTracks().tracks[0].messages) {
                    t += m.deltaTime
                    if (m.event.eventType.toInt() == SMF_META_EVENT && m.event.metaType.toInt() == MidiMetaType.MARKER)
                        markers.add(MidiMessage(t, MidiEvent(SMF_META_EVENT, 0, 0, m.event.extraData, m.event.extraDataOffset, m.event.extraDataLength)))
                }

                globalMarkers = markers.toTypedArray()
println("GLOBAL MARKERS: ${globalMarkers.size}")
            }
        }

        for (midiTrack in context.midi.tracks) {
            val ttrack = TrackElement().apply {
                Name = populateTrackName(midiTrack)
                Extension_InstrumentName = populateInstrumentName(midiTrack)

                val graph = context.mappedPlugins[Extension_InstrumentName ?: ""]
                if (graph != null)
                    for (p in AugeneModel.toTracktion(AugenePluginSpecifier.fromAudioGraph(graph)))
                        Plugins.add(p.apply { Id = context.generateNewID() })
            }
            context.edit.Tracks.add(ttrack)
            importTrack(midiTrack, ttrack)
            if (!ttrack.Clips.any() && !ttrack.Clips.any())
                context.edit.Tracks.remove(ttrack)
            else {
                ttrack.Plugins.add(PluginElement().apply {
                    Type = "volume"
                    Volume = 0.8
                    Enabled = true
                }
                )
                ttrack.Plugins.add(PluginElement().apply {
                    Type = "level"
                    Enabled = true
                }
                )
                ttrack.OutputDevices = OutputDevicesElement()
                ttrack.OutputDevices!!.OutputDevices.add(DeviceElement().apply {
                    Name = "(default audio output)"
                })
            }
        }
    }

    private fun toTracktionBarSpec(deltaTime: Int) =
        deltaTime.toDouble() / context.midi.deltaTimeSpec

    private fun importTrack(mtrack: MidiTrack, ttrack: TrackElement) {
        globalMarkers.iterator().also {
            importTrack(mtrack, ttrack, it)
        }
    }

    private fun importTrack(mtrack: MidiTrack, ttrack: TrackElement, globalMarkersEnumerator: Iterator<MidiMessage>) {
        var currentClipStart = 0.0
        // they are explicitly assigned due to C# limitation of initialization check...
        var nextGlobalMarker = MidiMessage(0, MidiEvent(0))
        var clip: MidiClipElement? = null
        var seq = SequenceElement() // dummy, but it's easier to hush CS8602...
        var currentTotalTime = 0

        val abstractMidiEventElementType = ModelCatalog.allTypes.first { it.simpleName == "AbstractMidiEventElement" }
        val terminateClip = {
            if (clip != null) {
                clip!!.PatternGenerator = PatternGeneratorElement()
                clip!!.PatternGenerator?.Progression = ProgressionElement()
                val e = seq.Events.lastOrNull { abstractMidiEventElementType.isAssignableFrom(it.getMetaType()) }
                if (e != null) {
                    val note = e as NoteElement?
                    val extend = note?.L ?: 0
                    clip?.Length = e.B + extend.toDouble()
                } else if (!seq.Events.any())
                    ttrack.Clips.remove(clip!!)
            }
        }
        val proceedToNextGlobalMarker = {
            nextGlobalMarker = if (globalMarkersEnumerator.hasNext())
                globalMarkersEnumerator.next()
            else
                MidiMessage(Int.MAX_VALUE, MidiEvent(0))
        }

        val nextClip = {
            terminateClip()
            currentClipStart = toTracktionBarSpec(nextGlobalMarker.deltaTime)
            val name = if (nextGlobalMarker.event.extraData == null) null else nextGlobalMarker.event.extraData!!.drop(
                nextGlobalMarker.event.extraDataOffset
            ).take(nextGlobalMarker.event.extraDataLength)
                .toByteArray().decodeToString()
            clip = MidiClipElement().apply {
                Type = "midi"
                Speed = 1.0
                Start = currentClipStart
                Name = name
            }
            ttrack.Clips.add(clip!!)
            seq = SequenceElement()
            clip!!.Sequence = seq

            proceedToNextGlobalMarker()
        }
        nextClip()

        ttrack.Modifiers = ModifiersElement()
        val noteDeltaTimes = Array(16 * 128) { 0 }  // new int [16, 128];
        val notes = Array<NoteElement?>(16 * 128) { null } // new NoteElement? [16,128];
        var timeSigNumerator = 4
        var timeSigDenominator = 4
        var currentBpm = 120.0
        var currentAutomationTarget: String? = null
        var currentAutomationTargetAsNumber: Int? = null

        for (msg in mtrack.messages) {
            currentTotalTime += msg.deltaTime
            while (true) {
                if (nextGlobalMarker.deltaTime <= currentTotalTime)
                    nextClip()
                else
                    break
            }

            val tTime = toTracktionBarSpec(currentTotalTime) - currentClipStart
            var eventType = msg.event.eventType.toUnsigned()
            if (eventType == MidiChannelStatus.NOTE_ON && msg.event.lsb.toInt() == 0)
                eventType = MidiChannelStatus.NOTE_OFF

            when (eventType) {
                MidiChannelStatus.NOTE_OFF -> {
                    val noteToOff = notes[msg.event.channel * 128 + msg.event.msb]
                    if (noteToOff != null) {
                        val l = currentTotalTime - noteDeltaTimes[msg.event.channel * 128 + msg.event.msb]
                        if (l == 0)
                            println("!!! Zero-length note: at ${toTracktionBarSpec(currentTotalTime)}, value: ${msg.event.value}")
                        else {
                            noteToOff.L = toTracktionBarSpec(l)
                            noteToOff.C = msg.event.lsb.toInt()
                        }
                    }
                    notes[msg.event.channel * 128 + msg.event.msb] = null
                    noteDeltaTimes[msg.event.channel * 128 + msg.event.msb] = 0
                }
                MidiChannelStatus.NOTE_ON -> {
                    val noteOn = NoteElement().apply {
                        B = tTime
                        P = msg.event.msb.toInt()
                        V = msg.event.lsb.toInt()
                    }
                    if (notes[msg.event.channel * 128 + msg.event.msb] != null)
                        println("!!! Overlapped note: at ${toTracktionBarSpec(currentTotalTime)}, value: ${msg.event.value.toString(16)}") // FIXME: format specifier "X08"
                    notes[msg.event.channel * 128 + msg.event.msb] = noteOn
                    noteDeltaTimes[msg.event.channel * 128 + msg.event.msb] = currentTotalTime
                    seq.Events.add(noteOn)
                }
                MidiChannelStatus.CAF ->
                    seq.Events.add(ControlElement().apply {
                        B = tTime
                        Type = ControlType.CAf
                        Val = msg.event.lsb * 128
                    })
                MidiChannelStatus.CC ->
                    seq.Events.add(ControlElement().apply {
                        B = tTime
                        Type = msg.event.msb.toInt()
                        Val = msg.event.lsb * 128
                    })
                MidiChannelStatus.PROGRAM ->
                    seq.Events.add(ControlElement().apply {
                        B = tTime
                        Type = ControlType.ProgramChange
                        Val = msg.event.msb * 128
                    }) // lol
                MidiChannelStatus.PAF ->
                    seq.Events.add(ControlElement().apply {
                        B = tTime
                        Type = ControlType.PAf
                        Val = msg.event.lsb * 128
                        Metadata = msg.event.msb.toInt()
                    })
                MidiChannelStatus.PITCH_BEND ->
                    seq.Events.add(ControlElement().apply {
                        B = tTime
                        Type = ControlType.PitchBend
                        Val = msg.event.msb * 128 + msg.event.lsb
                    })
                else -> { // sysex or meta
                    if (msg.event.eventType.toUnsigned() == SMF_SYSEX_EVENT) {
                        val sysex = msg.event.extraData
                        // Check if it is augene-specific sysex
                        if (sysex != null && sysex[0] == 0x7D.toByte() && sysex.size > 10 &&
                            sysex.drop(1).take(9).toByteArray().decodeToString() == "augene-ng") {
                            if (sysex[10] == 0.toByte()) {
                                if (sysex.size > 14) {
                                    // send automation parameter
                                    val targetParameter = sysex[11] + sysex[12] * 0x80
                                    val value = sysex[13] + sysex[14] * 0x80

                                    if (!ttrack.AutomationTracks.any { it.CurrentAutoParamPluginID == currentAutomationTargetAsNumber && it.CurrentAutoParamTag == targetParameter }) {
                                        val aTrack = AutomationTrackElement().apply {
                                            Id = context.generateNewID()
                                            CurrentAutoParamPluginID = currentAutomationTargetAsNumber
                                            CurrentAutoParamTag = targetParameter
                                            MacroParameters = MacroParametersElement().apply { Id = context.generateNewID() }
                                            Modifiers = ModifiersElement()
                                        }
                                        ttrack.AutomationTracks.add(aTrack)
                                    }
                                    val plugin = ttrack.Plugins.firstOrNull { it.Uid == currentAutomationTarget }
                                    if (plugin != null) {
                                        var aCurve = plugin.AutomationCurves.firstOrNull { it.ParamID == targetParameter }
                                        if (aCurve == null) {
                                            aCurve = AutomationCurveElement().apply { ParamID = targetParameter }
                                            plugin.AutomationCurves.add(aCurve)
                                        }
                                        aCurve.Points.add(PointElement().apply {
                                            t = tTime
                                            v = value.toDouble()
                                            c = 0.0 // FIXME: what is this?
                                        })
                                    }
                                    else
                                        println("!!! AUTOMATION TARGET PLUGIN NOT FOUND in the track: $currentAutomationTarget")
                                }
                                else
                                    println("!!! INSUFFICIENT AUTOMATION SEND SYSEX BUFFER")
                            } else {
                                // set automation target parameter by name
                                val nameLen = sysex[10].toUnsigned()
                                if (sysex.size >= 1 + 9 + 1 + nameLen) { // 7D, "augene-ng", nameLen byte, name
                                    currentAutomationTarget =
                                        sysex.drop(11).take(nameLen).toByteArray().decodeToString()
                                    var target = ttrack.Plugins.firstOrNull { it.Uid == currentAutomationTarget }
                                    if (target == null) {
                                        // create a stub PluginElement.
                                        target = PluginElement().apply {
                                            Name = ttrack.Extension_InstrumentName ?: "!!! STUB !!! REPLACE THIS !!!"
                                            Uid = currentAutomationTarget
                                            Id = context.generateNewID()
                                        }
                                        ttrack.Plugins.add(target)
                                    }
                                    currentAutomationTargetAsNumber = target.Id!!.toInt()
                                    println("AUTOMATION TARGET PLUGIN: $currentAutomationTarget ($currentAutomationTargetAsNumber)")
                                }
                                else
                                    // FIXME: replace these println hacks with some viable logging stuff.
                                    println("!!! INSUFFICIENT AUTOMATION TARGET PLUGIN SYSEX BUFFER")
                            }
                        }
                    }
                    if (msg.event.eventType.toUnsigned() == SMF_META_EVENT) {
                        when (msg.event.metaType.toUnsigned()) {
                            MidiMetaType.TRACK_NAME ->
                                ttrack.Id = msg.event.extraData?.decodeToString()
                            MidiMetaType.INSTRUMENT_NAME -> // This does not exist in TracktionEdit; ntracktive extends this.
                                ttrack.Extension_InstrumentName = msg.event.extraData?.decodeToString()
                            MidiMetaType.MARKER ->
                                when (context.markerImportStrategy) {
                                    MarkerImportStrategy.PerTrack -> TODO("implement")
                                    else -> {}
                                }
                            MidiMetaType.TEMPO -> {
                                currentBpm =
                                    toBpm(msg.event.extraData!!, msg.event.extraDataOffset, msg.event.extraDataLength)
                                context.edit.TempoSequence!!.Tempos.add(TempoElement().apply {
                                    StartBeat = toTracktionBarSpec(currentTotalTime)
                                    Curve = 1.0
                                    Bpm = currentBpm
                                })
                            }
                            MidiMetaType.TIME_SIGNATURE -> {
                                val tsEv = msg.event
                                timeSigNumerator = tsEv.extraData!![tsEv.extraDataOffset].toInt()
                                timeSigDenominator =
                                    tsEv.extraData!![tsEv.extraDataOffset + 1].toDouble().pow(2).toInt()
                                context.edit.TempoSequence!!.TimeSignatures.add(
                                    TimeSigElement().apply {
                                        StartBeat = toTracktionBarSpec(currentTotalTime)
                                        Numerator = timeSigNumerator
                                        Denominator = timeSigDenominator
                                    })
                                // Tracktion engine has a problem that its tempo calculation goes fubar when timesig denomitator becomes non-4 value.
                                context.edit.TempoSequence!!.Tempos.add(TempoElement().apply {
                                    StartBeat = toTracktionBarSpec(currentTotalTime)
                                    Curve = 1.0
                                    Bpm = currentBpm / (timeSigDenominator / 4)
                                })
                            }
                        }
                    }
                }
            }
        }

        terminateClip()
    }

    private fun toBpm(data: ByteArray, offset: Int, length: Int): Double {
        val t = (data[offset] shl 16) + (data[offset + 1] shl 8) + data[offset + 2]
        return 60000000.0 / t
    }

    private fun populateTrackName(track: MidiTrack): String? {
        val tnEv = track.messages.map { m -> m.event }
            .firstOrNull { e -> e.eventType.toUnsigned() == SMF_META_EVENT && e.metaType.toUnsigned() == MidiMetaType.TRACK_NAME }
        var trackName =
            if (tnEv?.extraData != null) tnEv.extraData!!.drop(tnEv.extraDataOffset).take(tnEv.extraDataLength)
                .toByteArray().decodeToString() else null
        val progChgs =
            track.messages.map { m -> m.event }.filter { e -> e.eventType.toInt() == MidiChannelStatus.PROGRAM }
                .toTypedArray()
        val firstProgramChangeValue = if (progChgs.isNotEmpty()) progChgs[0].msb else -1
        if (0 <= firstProgramChangeValue && firstProgramChangeValue < GeneralMidi.INSTRUMENT_NAMES.size)
            trackName = GeneralMidi.INSTRUMENT_NAMES[firstProgramChangeValue.toUnsigned()]
        return trackName
    }

    private fun populateInstrumentName(track: MidiTrack): String? {
        val inEv = track.messages.map { m -> m.event }
            .firstOrNull { e -> e.eventType.toUnsigned() == SMF_META_EVENT && e.metaType.toUnsigned() == MidiMetaType.INSTRUMENT_NAME }
        return if (inEv?.extraData != null) inEv.extraData!!.drop(inEv.extraDataOffset).take(inEv.extraDataLength)
                .toByteArray().decodeToString() else null
    }
}

private infix fun Byte.shl(i: Int): Int = this.toInt() shl i

