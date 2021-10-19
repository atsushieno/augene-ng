package dev.atsushieno.augene

import dev.atsushieno.kotractive.MidiTrackerElement
import dev.atsushieno.kotractive.TrackElement
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
    fun compileSpectra() {
        val model = AugeneCompiler()
        model.projectFileName = "../../samples/TestStub.augene"
        model.project = AugeneProject()
        model.dryRun = true
        model.project.mmlStrings.add("c1.  <b1.  >c1.  d1.<b4>  E_127,0,2,1^1^1,16c1^1^1^1")
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
}