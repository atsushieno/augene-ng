package dev.atsushieno.augene

import kotlin.test.Test
import kotlin.test.assertEquals


class AugeneModelJvmTest {
    @Test
    fun loadXml() {
        val model = AugeneModel()
        model.loadProjectFile("../../samples/automation/opnplug.augene")
    }

    @Test
    fun compile() {
        val model = AugeneModel()
        model.loadProjectFile("../../samples/automation/opnplug.augene")
        model.compile()
    }

    @Test
    fun compileSpectra() {
        val model = AugeneModel()
        model.projectFileName = "../../samples/TestStub.augene"
        model.project = AugeneProject()
        model.dryRun = true
        model.project.mmlStrings.add("c1.  <b1.  >c1.  d1.<b4>  E_127,0,2,1^1^1,16c1^1^1^1")
        model.compile()
    }
}