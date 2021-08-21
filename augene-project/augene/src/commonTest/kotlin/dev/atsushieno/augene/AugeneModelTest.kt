package dev.atsushieno.augene

import kotlin.test.Test
import kotlin.test.assertEquals


class AugeneModelTest {
    @Test
    fun load() {
        val json = """
{"audioGraphs":[{"id":"1","source":"test1.filtergraph"}],"mmlFiles":["test-augene-ng.mugene"]}
"""
        val model = AugeneModel()
        model.loadProjectJson(json)
        assertEquals(1, model.project.audioGraphs.size, "audioGraphs size")
        assertEquals(1, model.project.mmlFiles.size, "mmlFiles size")
    }
}