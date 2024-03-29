package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
import dev.atsushieno.ktmidi.*
import kotlin.math.pow

internal fun Byte.toUnsigned() : Int = if (this < 0) this + 0x100 else this.toInt()

internal val Ump.metaEventType : Int
    get() = if (this.messageType != MidiMessageType.SYSEX8_MDS) 0 else (this.int3 shr 8) and 0x7F

data class TimedMetaEvent(val position: Int, val event: List<Byte>)

internal val TrackElement.instrumentNameSplit : List<String>
    get() = this.Extension_InstrumentName?.split(';') ?: listOf()

class MidiToTracktionEditConverter(private var context: Midi2ToTracktionImportContext) {
    // state
    private var consumed = false
    // The initial entry is a dummy marker that indicates the end of events.
    private var globalMarkers = arrayOf(TimedMetaEvent(Int.MAX_VALUE, listOf()))

    fun process(midiFileContent: ByteArray, tracktionEditFileContent: String) : String {
        context = Midi2ToTracktionImportContext.create(midiFileContent, tracktionEditFileContent)
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
            MarkerImportStrategy.Global, MarkerImportStrategy.Default -> {
                val markers = mutableListOf<TimedMetaEvent>()
                var t = 0
                val messages = context.midi.mergeTracks().tracks.firstOrNull()?.messages ?: listOf()
                var inMetaEvents = false
                for (m in messages) {
                    if (inMetaEvents && m.messageType != MidiMessageType.SYSEX8_MDS)
                        inMetaEvents = false
                    if (m.isJRTimestamp)
                        t += m.jrTimestamp
                    else if (Midi2Music.isMetaEventMessageStarter(m) && m.metaEventType == MidiMetaType.MARKER) {
                        markers.add(TimedMetaEvent(t, UmpRetriever.getSysex8Data(messages.drop(messages.indexOf(m)).iterator())))
                    }
                    // else -> skip
                }

                globalMarkers = markers.toTypedArray()
            }
            else -> {}
        }

        val markerTrack = MarkerTrackElement()
        context.edit.Tracks.add(markerTrack)
        var markerID = 1
        var lastMarker : MarkerClipElement? = null
        globalMarkers.forEach {
            // FIXME: length seems wrong.
            lastMarker?.Length = toTracktionBarSpec(it.position) - (lastMarker?.Start ?: 0.0)
            val marker = MarkerClipElement().apply {
                // FIXME: verify if this drop(8) is correct
                Name = it.event.drop(8).toByteArray().decodeToString()
                Start = if (lastMarker != null) lastMarker!!.Start + lastMarker!!.Length else 0.0
                Offset = 0.0
                Speed = 1.0
                MarkerID = markerID.toString()
            }
            markerTrack.Clips.add(marker)
            lastMarker = marker
            markerID++
        }
        lastMarker?.Length = toTracktionBarSpec(context.midi.getTotalTicks()) - (lastMarker?.Start ?: 0.0)

        for (midiTrack in context.midi.tracks) {
            val ttrack = TrackElement().apply {
                Id = context.generateNewID()
                Name = populateTrackName(midiTrack)
                Extension_InstrumentName = populateInstrumentName(midiTrack)

                instrumentNameSplit.forEach {
                    val graph = context.mappedPlugins[it]
                    if (graph != null)
                        for (p in AugeneCompiler.toTracktion(JuceAudioGraph.toAudioGraph(graph)))
                            Plugins.add(p.apply { Id = context.generateNewID() })
                }
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

    private fun importTrack(mtrack: Midi2Track, ttrack: TrackElement) {
        globalMarkers.iterator().also {
            importTrack(mtrack, ttrack, it)
        }
    }

    private fun importTrack(mtrack: Midi2Track, ttrack: TrackElement, globalMarkersEnumerator: Iterator<TimedMetaEvent>) {
        var currentClipStart = 0.0
        var nextGlobalMarker = TimedMetaEvent(0, listOf())
        var clip: MidiClipElement? = null
        var seq = SequenceElement() // dummy
        var currentTotalTime = 0

        val abstractMidiEventElementType = ModelCatalog.allTypes.first { it.simpleName == "AbstractMidiEventElement" }
        val terminateClip = {
            if (clip != null) {
                clip!!.PatternGenerator = PatternGeneratorElement()
                clip!!.PatternGenerator?.Progression = ProgressionElement()
                val e = seq.Events.lastOrNull { abstractMidiEventElementType.isAssignableFrom(it.getMetaType()) }
                if (e != null) {
                    val note = if (e is NoteElement) e else null
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
                TimedMetaEvent(Int.MAX_VALUE, listOf()) // dummy marker that indicates end
        }

        val nextClip = {
            terminateClip()
            currentClipStart = toTracktionBarSpec(nextGlobalMarker.position)
            val name = nextGlobalMarker.event.drop(8).toByteArray().decodeToString()
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
        val machine = Midi2Machine().apply {
            diagnosticsHandler = { msg, ump -> this@MidiToTracktionEditConverter.context.report(
                msg + (if (ump != null) " : $ump" else null)) }
        }
        val noteDeltaTimes = Array(16 * 16 * 128) { 0 }
        val notes = Array<NoteElement?>(16 * 16 * 128) { null }
        var timeSigNumerator = 4
        var timeSigDenominator = 4
        var currentBpm = 120.0
        var currentAutomationTarget: String? = null
        var currentAutomationTargetAsNumber: Int? = null

        mtrack.messages.forEachIndexed { index, msg ->
            machine.processEvent(msg)

            if (msg.isDeltaClockstamp) {
                currentTotalTime += msg.deltaClockstamp
                return@forEachIndexed
            }
            // FIXME: maybe enable this with some compilation options?
            //  When clips are split, program changes and maybe other controls are not preserved to the next clip...
            /*
            while (true) {
                if (nextGlobalMarker.position <= currentTotalTime)
                    nextClip()
                else
                    break
            }
            */

            val tTime = toTracktionBarSpec(currentTotalTime) - currentClipStart
            var eventType = msg.statusCode
            if (msg.messageType == MidiMessageType.MIDI1 && eventType == MidiChannelStatus.NOTE_ON && msg.midi1Lsb == 0)
                eventType = MidiChannelStatus.NOTE_OFF

            when (msg.messageType) {
                MidiMessageType.MIDI1 ->
                    when (eventType) {
                        MidiChannelStatus.NOTE_OFF -> {
                            if (msg.midi1Msb < 0 || msg.midi1Msb > 127)
                                throw IllegalArgumentException("Note must be non-negative byte range: " + msg.midi1Msb)
                            val noteToOff = notes[msg.groupAndChannel * 128 + msg.midi1Msb]
                            if (noteToOff != null) {
                                val l = currentTotalTime - noteDeltaTimes[msg.groupAndChannel * 128 + msg.midi1Msb]
                                if (l == 0)
                                    context.report("Zero-length note: at ${toTracktionBarSpec(currentTotalTime)}, value: $msg")
                                else {
                                    noteToOff.L = toTracktionBarSpec(l)
                                    noteToOff.C = msg.midi1Lsb
                                }
                            }
                            notes[msg.groupAndChannel * 128 + msg.midi1Msb] = null
                            noteDeltaTimes[msg.groupAndChannel * 128 + msg.midi1Msb] = 0
                        }
                        MidiChannelStatus.NOTE_ON -> {
                            if (msg.midi1Msb < 0 || msg.midi1Msb > 127)
                                throw IllegalArgumentException("Note must be non-negative byte range: " + msg.midi1Msb)
                            val noteOn = NoteElement().apply {
                                B = tTime
                                P = msg.midi1Msb
                                V = msg.midi1Lsb
                            }
                            if (notes[msg.groupAndChannel * 128 + msg.midi1Msb] != null)
                                context.report("Overlapped note: at ${toTracktionBarSpec(currentTotalTime)}, value: $msg")
                            notes[msg.groupAndChannel * 128 + msg.midi1Msb] = noteOn
                            noteDeltaTimes[msg.groupAndChannel * 128 + msg.midi1Msb] = currentTotalTime
                            seq.Events.add(noteOn)

                            // Per-Note Expressions. Only PitchBend so far.
                            val pnp = machine.channel(msg.groupAndChannel).perNotePitchbend[msg.midi1Msb]
                            if (pnp != 0x80000000u)
                                noteOn.Expressions.add(PitchBendElement().apply {
                                    B = 0.0
                                    V = pnp.toDouble() / 0x40000 / 8192 // FIXME: take pitch bend sensitivity into account too.
                                })
                        }
                        MidiChannelStatus.CAF ->
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = ControlType.CAf
                                Val = msg.midi1Lsb * 256
                            })
                        MidiChannelStatus.CC ->
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = msg.midi1Msb
                                Val = msg.midi1Lsb * 256
                            })
                        MidiChannelStatus.PROGRAM ->
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = ControlType.ProgramChange
                                Val = msg.midi1Msb * 128 // lol
                                Metadata = 0
                            })
                        MidiChannelStatus.PAF ->
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = ControlType.PAf
                                Val = msg.midi1Lsb * 256
                                Metadata = msg.midi1Msb
                            })
                        MidiChannelStatus.PITCH_BEND ->
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = ControlType.PitchBend
                                Val = msg.midi1Msb * 256 + msg.midi1Lsb
                            })
                    }
                MidiMessageType.MIDI2 ->
                    // FIXME: consider "group" with "channelInGroup"
                    // FIXME: support new MIDI2-specific events (per-note CC, per-note management etc.)
                    when (eventType) {
                        MidiChannelStatus.NOTE_OFF -> {
                            if (msg.midi2Note < 0 || msg.midi2Note > 127)
                                throw IllegalArgumentException("Note must be non-negative byte range: " + msg.midi2Note)
                            val noteToOff = notes[msg.groupAndChannel * 128 + msg.midi2Note]
                            if (noteToOff != null) {
                                val l = currentTotalTime - noteDeltaTimes[msg.groupAndChannel * 128 + msg.midi2Note]
                                if (l == 0)
                                    context.report("Zero-length note: at ${toTracktionBarSpec(currentTotalTime)}, value: $msg")
                                else {
                                    noteToOff.L = toTracktionBarSpec(l)
                                    noteToOff.C = msg.midi2Velocity16 / 0x100 // it seems the value range is between 0..127
                                }
                            }
                            notes[msg.groupAndChannel * 128 + msg.midi2Note] = null
                            noteDeltaTimes[msg.groupAndChannel * 128 + msg.midi2Note] = 0
                        }
                        MidiChannelStatus.NOTE_ON -> {
                            if (msg.midi2Note < 0 || msg.midi2Note > 127)
                                throw IllegalArgumentException("Note must be in non-negative byte range: " + msg.midi2Note)
                            val noteOn = NoteElement().apply {
                                B = tTime
                                P = msg.midi2Note
                                V = msg.midi2Velocity16 / 0x100 // it seems the value range is between 0..127
                            }
                            if (notes[msg.groupAndChannel * 128 + msg.midi2Note] != null)
                                context.report("Overlapped note: at ${toTracktionBarSpec(currentTotalTime)}, value: $msg")
                            notes[msg.groupAndChannel * 128 + msg.midi2Note] = noteOn
                            noteDeltaTimes[msg.groupAndChannel * 128 + msg.midi2Note] = currentTotalTime
                            seq.Events.add(noteOn)

                            // Per-Note Expressions. Only PitchBend so far.
                            val pnp = machine.channel(msg.groupAndChannel).perNotePitchbend[msg.midi2Note]
                            if (pnp != 0x80000000u)
                                noteOn.Expressions.add(PitchBendElement().apply {
                                    B = 0.0
                                    V = pnp.toDouble() / 0x40000 / 8192 // FIXME: take pitch bend sensitivity into account too.
                                })
                        }
                        MidiChannelStatus.CAF ->
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = ControlType.CAf
                                Val = (msg.midi2CAfData / 0x20000u).toInt() // downconverting value range from 32bit to 15bit
                            })
                        MidiChannelStatus.CC ->
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = msg.midi2CCIndex
                                Val = (msg.midi2CCData / 0x20000u).toInt() // downconverting value range from 32bit to 15bit
                            })
                        MidiChannelStatus.PROGRAM -> {
                            if (msg.midi2ProgramBankMsb != 0)
                                seq.Events.add(ControlElement().apply {
                                    B = tTime
                                    Type = MidiCC.BANK_SELECT
                                    Val = msg.midi2ProgramBankMsb
                                })
                            if (msg.midi2ProgramBankMsb != 0)
                                seq.Events.add(ControlElement().apply {
                                    B = tTime
                                    Type = MidiCC.BANK_SELECT
                                    Val = msg.midi2ProgramBankMsb
                                })
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = ControlType.ProgramChange
                                Val = msg.midi2ProgramProgram * 128 // lol
                            })
                        }
                        MidiChannelStatus.PAF ->
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = ControlType.PAf
                                Val = (msg.midi2PAfData / 0x20000u).toInt() // downconverting value range from 32bit to 15bit
                                Metadata = msg.midi2Note
                            })
                        MidiChannelStatus.PITCH_BEND ->
                            seq.Events.add(ControlElement().apply {
                                B = tTime
                                Type = ControlType.PitchBend
                                Val = (msg.midi2PitchBendData / 0x200u).toInt() // downconverting value range from 32bit to 15bit
                            })
                        MidiChannelStatus.PER_NOTE_PITCH_BEND -> {
                            // generate PITCHBEND element only if there is current note-on element.
                            val noteOn = notes[msg.groupAndChannel * 128 + msg.midi2Note]
                            noteOn?.Expressions?.add(PitchBendElement().apply {
                                B = tTime - noteOn.B
                                V = msg.midi2PitchBendData.toDouble() / 0x40000 / 8192 // FIXME: take pitch bend sensitivity into account too.
                            })
                        }
                    }
                MidiMessageType.SYSEX7, MidiMessageType.SYSEX8_MDS -> { // sysex or meta
                    val sysex =
                        if (msg.messageType == MidiMessageType.SYSEX7 && (msg.statusCode == Midi2BinaryChunkStatus.START || msg.statusCode == Midi2BinaryChunkStatus.COMPLETE_PACKET))
                            UmpRetriever.getSysex7Data(mtrack.messages.drop(index).iterator())
                        else if (msg.messageType == MidiMessageType.SYSEX8_MDS && (msg.statusCode == Midi2BinaryChunkStatus.START || msg.statusCode == Midi2BinaryChunkStatus.COMPLETE_PACKET))
                            UmpRetriever.getSysex8Data(mtrack.messages.drop(index).iterator())
                        else null
                    // Check if it is augene-specific sysex
                    if (sysex != null) {
                        if (sysex[0] == 0x7D.toByte() && sysex.size > 10 &&
                            sysex.drop(1).take(9).toByteArray().decodeToString() == "augene-ng"
                        ) {
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
                                            MacroParameters =
                                                MacroParametersElement().apply { Id = context.generateNewID() }
                                            Modifiers = ModifiersElement()
                                        }
                                        ttrack.AutomationTracks.add(aTrack)
                                    }
                                    val plugin = ttrack.Plugins.firstOrNull { it.Uid == currentAutomationTarget }
                                    if (plugin != null) {
                                        var aCurve =
                                            plugin.AutomationCurves.firstOrNull { it.ParamID == targetParameter }
                                        if (aCurve == null) {
                                            aCurve = AutomationCurveElement().apply { ParamID = targetParameter }
                                            plugin.AutomationCurves.add(aCurve)
                                        }
                                        aCurve.Points.add(PointElement().apply {
                                            t = tTime
                                            v = value.toDouble()
                                            c = 0.0 // FIXME: what is this?
                                        })
                                    } else
                                        context.report("AUTOMATION TARGET PLUGIN NOT FOUND in the track: $currentAutomationTarget")
                                } else
                                    context.report("INSUFFICIENT AUTOMATION SEND SYSEX BUFFER")
                            } else {
                                // set automation target parameter by name
                                val nameLen = sysex[10].toUnsigned()
                                if (sysex.size >= 1 + 9 + 1 + nameLen) { // 7D, "augene-ng", nameLen byte, name
                                    currentAutomationTarget =
                                        sysex.drop(11).take(nameLen).toByteArray().decodeToString()
                                    val target = ttrack.Plugins.firstOrNull { it.Uid == currentAutomationTarget }
                                    if (target == null) {
                                        ttrack.instrumentNameSplit.forEach {
                                            // create a stub PluginElement.
                                            val inst = PluginElement().apply {
                                                Name = it
                                                Uid = currentAutomationTarget
                                                Id = context.generateNewID()
                                            }
                                            ttrack.Plugins.add(inst)
                                            currentAutomationTargetAsNumber = inst.Id!!.toInt()
                                        }
                                    } else
                                        currentAutomationTargetAsNumber = target.Id!!.toInt()
                                } else
                                    context.report(
                                        "INSUFFICIENT AUTOMATION TARGET PLUGIN SYSEX BUFFER: required ${1 + 9 + 1 + nameLen} byets, got ${sysex.size} bytes (\"${
                                            sysex.drop(
                                                1 + 9 + 1
                                            ).toByteArray().decodeToString()
                                        }\" ?)"
                                    )
                            }
                        } else when (msg.metaEventType) {
                            MidiMetaType.TRACK_NAME ->
                                ttrack.Name = sysex.drop(8).toByteArray().decodeToString()

                            MidiMetaType.INSTRUMENT_NAME -> // This does not exist in TracktionEdit; ntracktive extends this.
                                ttrack.Extension_InstrumentName = sysex.drop(8).toByteArray().decodeToString()

                            MidiMetaType.MARKER ->
                                when (context.markerImportStrategy) {
                                    MarkerImportStrategy.PerTrack -> TODO("implement")
                                    else -> {}
                                }

                            MidiMetaType.TEMPO -> {
                                currentBpm = toBpm(sysex.drop(8).toByteArray(), 0, sysex.size - 8)
                                context.edit.TempoSequence!!.Tempos.add(TempoElement().apply {
                                    StartBeat = toTracktionBarSpec(currentTotalTime)
                                    Curve = 1.0
                                    Bpm = currentBpm / (timeSigDenominator.toDouble() / 4)
                                })
                            }

                            MidiMetaType.TIME_SIGNATURE -> {
                                timeSigNumerator = sysex[8].toInt()
                                timeSigDenominator = 2.0.pow(sysex[9].toDouble()).toInt()
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
                                    Bpm = currentBpm / (timeSigDenominator.toDouble() / 4)
                                })
                            }
                        }
                    }
                }
                else -> {
                    if (msg.isTempo) {
                        currentBpm = toBpm(msg.tempo)
                        context.edit.TempoSequence!!.Tempos.add(TempoElement().apply {
                            StartBeat = toTracktionBarSpec(currentTotalTime)
                            Curve = 1.0
                            Bpm = currentBpm / (timeSigDenominator.toDouble() / 4)
                        })
                    } else if (msg.isTimeSignature) {
                        timeSigNumerator = msg.timeSignatureNumerator
                        timeSigDenominator = 2.0.pow(msg.timeSignatureDenominator).toInt()
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
                            Bpm = currentBpm / (timeSigDenominator.toDouble() / 4)
                        })
                    }
                }
            }
        }

        terminateClip()
    }

    private fun toBpm(data: ByteArray, offset: Int, length: Int): Double {
        val t = if (length < 2) 500000 else (data[offset].toUnsigned() shl 16) + (data[offset + 1].toUnsigned() shl 8) + data[offset + 2].toUnsigned()
        return 60000000.0 / t
    }
    // FlexData-based tempo, note that the precision is different.
    private fun toBpm(t: Int): Double = 6000000000.0 / t

    private fun populateTrackName(track: Midi2Track): String? {
        val tnEv = track.messages.firstOrNull { m -> Midi2Music.isMetaEventMessageStarter(m) && m.metaEventType == MidiMetaType.TRACK_NAME }
        var trackName = if (tnEv == null) null else UmpRetriever.getSysex8Data(track.messages.drop(track.messages.indexOf(tnEv)).iterator())
                .drop(8) // skip: manufacturer ID, device ID, sub ID1, sub ID2, FFh, FFh, FFh, and message type
                .toByteArray().decodeToString()
        if (trackName == null) {
            var firstProgramChangeValue = -1
            val programChanges = track.messages.filter { e -> e.statusCode == MidiChannelStatus.PROGRAM }.toTypedArray()
            if (programChanges.isNotEmpty()) {
                val m = programChanges[0]
                firstProgramChangeValue = if (m.messageType == MidiMessageType.MIDI1) m.midi1Program else m.midi2ProgramProgram
            }
            if (0 <= firstProgramChangeValue && firstProgramChangeValue < GeneralMidi.INSTRUMENT_NAMES.size)
                trackName = GeneralMidi.INSTRUMENT_NAMES[firstProgramChangeValue]
        }
        return trackName
    }

    private fun populateInstrumentName(track: Midi2Track): String? {
        val inEv = track.messages.firstOrNull {m -> Midi2Music.isMetaEventMessageStarter(m) && m.metaEventType == MidiMetaType.INSTRUMENT_NAME }
        return if (inEv == null) null else UmpRetriever.getSysex8Data(track.messages.drop(track.messages.indexOf(inEv)).iterator())
            .drop(8) // skip: manufacturer ID, device ID, sub ID1, sub ID2, FFh, FFh, FFh, and message type
            .toByteArray().decodeToString()
    }
}

private infix fun Byte.shl(i: Int): Int = this.toInt() shl i

