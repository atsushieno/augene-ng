package dev.atsushieno.augene

import dev.atsushieno.kotractive.MidiClipElement
import dev.atsushieno.kotractive.MidiTrackerElement
import dev.atsushieno.kotractive.TrackElement
import kotlin.math.roundToInt
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class AugeneCompilerJvmTest {
    @Test
    fun loadXml() {
        val model = AugeneCompiler()
        model.loadProjectFile("../../samples/automation/opnplug.augene")
    }

    @Test
    fun compile() {
        val model = AugeneCompiler()
        model.loadProjectFile("../../samples/automation/opnplug.augene")
        model.compile()
        assertTrue(model.edit.Tracks.filterIsInstance<TrackElement>().all { it.Plugins.any { it.Type == "vst" } }, "no vst in the track")
    }

    @Test
    fun compile2() {
        val model = AugeneCompiler()
        model.loadProjectFile("../../samples/mars/mars.augene")
        model.compile()
        model.edit.Tracks.filterIsInstance<TrackElement>().forEach {
            assertTrue(it.Plugins.any { it.Type == "vst" }, "no vst in the track ${it.Id}")
        }
    }

    @Test
    fun compile3() {
        val model = AugeneCompiler()
        model.loadProjectFile("../../samples/mars/mars_sfizz.augene")
        model.compile()
        model.edit.Tracks.filterIsInstance<TrackElement>().forEach {
            assertTrue(it.Plugins.any { it.Type == "vst" }, "no vst in the track ${it.Id}")
        }
    }

    @Test
    fun compileSpectra() {
        val model = AugeneCompiler()
        model.projectFileName = "../../samples/TestStub.augene"
        model.project = AugeneProject()
        model.dryRun = true
        model.project.mmlStrings.add("1   c1.  <b1.  >c1.  d1.<b4>  E_127,0,2,1^1^1,16c1^1^1^1")
        model.compile()
    }

    @Test
    fun zeroLengthNotes() {
        val errors = mutableListOf<String>()
        val model = AugeneCompiler()
        model.projectFileName = "../../samples/TestStub.augene"
        model.project = AugeneProject()
        model.dryRun = true
        model.project.mmlStrings.add("Q4 l12 c")
        model.reporter = { errorId, message -> errors.add("$errorId: $message") }
        model.compile()
        assertEquals(0, errors.size, "warning reports")
    }

    @Test
    fun importTimeSignature() {
        val model = AugeneCompiler()
        model.projectFileName = "../../samples/TestStub.augene"
        model.project = AugeneProject()
        model.dryRun = true
        model.project.mmlStrings.add("1   BEAT5,2 BEAT5,4 BEAT5,8")
        model.compile()
        val ts = model.edit.TempoSequence!!.TimeSignatures
        assertEquals(3, ts.size, "size")
        assertEquals(5, ts[0].Numerator, "0.numerator")
        assertEquals(2, ts[0].Denominator, "0.denominator")
        assertEquals(5, ts[1].Numerator, "1.numerator")
        assertEquals(4, ts[1].Denominator, "1.denominator")
        assertEquals(5, ts[2].Numerator, "2.numerator")
        assertEquals(8, ts[2].Denominator, "2.denominator")
    }

    @Test
    fun importTempo() {
        val model = AugeneCompiler()
        model.projectFileName = "../../samples/TestStub.augene"
        model.project = AugeneProject()
        model.dryRun = true
        model.project.mmlStrings.add("1   BEAT5,2 r1 t130 r1 t125 r1 BEAT5,4 r1 t140")
        model.compile()
        val ts = model.edit.TempoSequence!!.Tempos
        assertEquals(5, ts.size, "size")
        assertEquals(240, ts[0].Bpm.roundToInt(), "0.bpm")
        assertEquals(260, ts[1].Bpm.roundToInt(), "1.bpm")
        assertEquals(250, ts[2].Bpm.roundToInt(), "2.bpm")
        assertEquals(125, ts[3].Bpm.roundToInt(), "3.bpm")
        assertEquals(140, ts[4].Bpm.roundToInt(), "4.bpm")
    }

    @Test
    fun automationSetPlugin() {
        val mml = """
#macro AUDIO_PLUGIN_USE nameLen:number, ident:string {  __MIDI #F0, #7D, "augene-ng", ${'$'}nameLen, ${'$'}ident, #F7 }
#macro AUDIO_PLUGIN_PARAMETER parameterID:number, val:number { \
    __MIDI #F0, #7D, "augene-ng", 0, \
    ${'$'}parameterID % #80, ${'$'}parameterID / #80, ${'$'}val % #80, ${'$'}val / #80 } 
#macro SIMPLEREVERB { AUDIO_PLUGIN_USE 8, "52b9494c" }
#macro SIMPLEREVERB_FREEZE val { SIMPLEREVERB AUDIO_PLUGIN_PARAMETER 4, ${'$'}val }
1   SIMPLEREVERB_FREEZE 7 n64
        """
        val model = AugeneCompiler()
        model.projectFileName = "../../samples/TestStub.augene"
        model.project = AugeneProject()
        model.dryRun = true
        model.project.mmlStrings.add(mml)
        model.compile()
        val track = model.edit.Tracks.filterIsInstance<TrackElement>().first()
        assertTrue(track.AutomationTracks.any(), "missing AutomationTrack")
    }
}