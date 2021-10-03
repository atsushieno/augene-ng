package dev.atsushieno.augene

import kotlin.test.Test
import kotlin.test.assertEquals


class AugeneModelTest {
    @Test
    fun loadJson() {
        val json = """
{"audioGraphs":[{"id":"1","source":"test1.filtergraph"}],"mmlFiles":["test-augene-ng.mugene"]}
"""
        val model = AugeneCompiler()
        model.loadProjectJson(json)
        assertEquals(1, model.project.audioGraphs.size, "audioGraphs size")
        assertEquals(1, model.project.mmlFiles.size, "mmlFiles size")
    }
}