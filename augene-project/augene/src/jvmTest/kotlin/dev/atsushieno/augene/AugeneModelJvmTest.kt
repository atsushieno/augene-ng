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
}