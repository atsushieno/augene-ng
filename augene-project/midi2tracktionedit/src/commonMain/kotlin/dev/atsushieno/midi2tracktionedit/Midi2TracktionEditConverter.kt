package dev.atsushieno.midi2tracktionedit

import dev.atsushieno.kotractive.*
import dev.atsushieno.ktmidi.GeneralMidi
import dev.atsushieno.ktmidi.MidiChannelStatus
import dev.atsushieno.ktmidi.MidiEvent
import dev.atsushieno.ktmidi.MidiMessage
import dev.atsushieno.ktmidi.MidiMetaType
import dev.atsushieno.ktmidi.MidiTrack
import dev.atsushieno.ktmidi.mergeTracks
import kotlin.math.pow

private val SMF_META_EVENT = 0xFF

class MidiToTracktionEditConverter(private var context: MidiImportContext) {
    // state
    private var consumed = false
    private var globalMarkers = arrayOf(MidiMessage(Int.MAX_VALUE, MidiEvent(0)))

    fun process(midiFileContent: ByteArray, tracktionEditFileContent: String) : String {
        context = CommandArgumentContext(midiFileContent, tracktionEditFileContent).createImportContext()
        importMusic()
        val sb = StringBuilder()
        EditModelWriter().write(sb, context.edit)
        return sb.toString()
    }

    /* It is not doable ATM, as there is no File access API in Kotlin MPP.

    fun Process (args: Array<String>) {
        var argumentContext = GetContextFromCommandArguments (args)
        context = argumentContext.createImportContext()
        ImportMusic ()
        EditModelWriter ().Write (Console.Out, context.edit)
    }

    fun GetContextFromCommandArguments (args: Array<String>): CommandArgumentContext {
        var context =  CommandArgumentContext ()
        context.MidiFile = args.firstOrNull ()
        context.TracktionEditTemplateFile = args.drop (1).firstOrNull ()
        return context
    }
    */

    private fun importMusic() {
        if (consumed)
            throw IllegalArgumentException("This instance is already used. Create another instance of ${this.GetType()} if you want to process more.")
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
                var markers = mutableListOf<MidiMessage>()
                var t = 0
                for (m in context.midi.mergeTracks().tracks[0].messages) {
                    t += m.deltaTime
                    if (m.event.eventType.toInt() == SMF_META_EVENT && m.event.metaType.toInt() == MidiMetaType.MARKER)
                        markers.add(MidiMessage(t, MidiEvent(SMF_META_EVENT, 0, 0, m.event.extraData, m.event.extraDataOffset, m.event.extraDataLength)))
                }

                globalMarkers = markers.toTypedArray()
                Console.Error.WriteLine("GLOBAL MARKERS:" + globalMarkers.size)
            }
        }

        for (mtrack in context.midi.tracks) {
            var trackName = populateTrackName(mtrack)
            var ttrack = TrackElement().apply { Name = trackName }
            context.edit.Tracks.add(ttrack)
            importTrack(mtrack, ttrack)
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

        val terminateClip = {
            if (clip != null) {
                clip!!.PatternGenerator = PatternGeneratorElement()
                clip!!.PatternGenerator?.Progression = ProgressionElement()
                var e = seq.Events.OfType<AbstractMidiEventElement>().lastOrNull()
                if (e != null) {
                    var note = e as NoteElement
                    var extend = if (note != null) note.L else 0
                    clip?.Length = e.B + extend
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
                .toByteArray().contentToString()
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
        var noteDeltaTimes = Array<Int>(16 * 128) { 0 }  // new int [16, 128];
        var notes = Array<NoteElement?>(16 * 128) { null } // new NoteElement? [16,128];
        var timeSigNumerator = 4
        var timeSigDenominator = 4
        var currentBpm = 120.0

        for (msg in mtrack.messages) {
            currentTotalTime += msg.deltaTime
            while (true) {
                if (nextGlobalMarker.deltaTime <= currentTotalTime)
                    nextClip()
                else
                    break
            }

            var tTime = toTracktionBarSpec(currentTotalTime) - currentClipStart
            var eventType = msg.event.eventType.toInt()
            if (eventType == MidiChannelStatus.NOTE_ON && msg.event.lsb.toInt() == 0)
                eventType = MidiChannelStatus.NOTE_OFF

            when (eventType) {
                MidiChannelStatus.NOTE_OFF -> {
                    var noteToOff = notes[msg.event.channel * 128 + msg.event.msb]
                    if (noteToOff != null) {
                        var l = currentTotalTime - noteDeltaTimes[msg.event.channel * 128 + msg.event.msb]
                        if (l == 0)
                            Console.Error.WriteLine(("!!! Zero-length note: at ${toTracktionBarSpec(currentTotalTime)}, value: ${msg.event.value}"))
                        else {
                            noteToOff.L = toTracktionBarSpec(l)
                            noteToOff.C = msg.event.lsb.toInt()
                        }
                    }
                    notes[msg.event.channel * 128 + msg.event.msb] = null
                    noteDeltaTimes[msg.event.channel * 128 + msg.event.msb] = 0
                }
                MidiChannelStatus.NOTE_ON -> {
                    var noteOn = NoteElement().apply {
                        B = tTime
                        P = msg.event.msb.toInt()
                        V = msg.event.lsb.toInt()
                    }
                    if (notes[msg.event.channel * 128 + msg.event.msb] != null)
                        Console.Error.WriteLine(
                            "!!! Overlapped note: at ${toTracktionBarSpec(currentTotalTime)}, value: ${
                                msg.event.value.toString(
                                    16
                                )
                            }"
                        ) // FIXME: format specifier "X08"
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
                    if (msg.event.eventType.toInt() == SMF_META_EVENT) {
                        when (msg.event.metaType.toInt()) {
                            MidiMetaType.TRACK_NAME ->
                                ttrack.Id = msg.event.extraData.contentToString()
                            MidiMetaType.INSTRUMENT_NAME -> // This does not exist in TracktionEdit; ntracktive extends this.
                                ttrack.Extension_InstrumentName = msg.event.extraData.contentToString()
                            MidiMetaType.MARKER ->
                                when (context.markerImportStrategy) {
                                    MarkerImportStrategy.PerTrack -> {
                                        // TODO: implement
                                    }
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
                                var tsEv = msg.event
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
        var t = (data[offset] shl 16) + (data[offset + 1] shl 8) + data[offset + 2]
        return 60000000.0 / t
    }

    private fun populateTrackName(track: MidiTrack): String? {
        val tnEv = track.messages.map { m -> m.event }
            .firstOrNull { e -> e.eventType.toInt() == SMF_META_EVENT && e.metaType.toInt() == MidiMetaType.TRACK_NAME }
        var trackName =
            if (tnEv?.extraData != null) tnEv.extraData!!.drop(tnEv.extraDataOffset).take(tnEv.extraDataLength)
                .toByteArray().contentToString() else null
        val progChgs =
            track.messages.map { m -> m.event }.filter { e -> e.eventType.toInt() == MidiChannelStatus.PROGRAM }
                .toTypedArray()
        val firstProgramChangeValue = if (progChgs.isNotEmpty()) progChgs[0].msb else -1
        if (0 <= firstProgramChangeValue && firstProgramChangeValue < GeneralMidi.INSTRUMENT_NAMES.size)
            trackName = GeneralMidi.INSTRUMENT_NAMES[firstProgramChangeValue.toInt()]
        return trackName
    }
}

private infix fun Byte.shl(i: Int): Int = this.toInt() shl i

