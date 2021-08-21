package dev.atsushieno.kotractive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EditModelWriterTest {
    @Test
    fun write1() {
        val writer = EditModelWriter()
        val sb = StringBuilder()
        writer.write(sb, EditElement())
        val xml = "<EDIT creationTime=\"0\" />"
        assertEquals(xml, sb.toString())
    }

    @Test
    fun writeTemplate() {
        val writer = EditModelWriter()
        val sb = StringBuilder()
        writer.write(sb, EditModelTemplate.CreateNewEmptyEdit())

        assertTrue(sb.toString().length > 100, "unexpectedly short string output: $sb")
        println(sb)

        val xr = XmlTextReader(sb.toString())
        while (xr.read())
            println("XmlReader: ${xr.depth} ${xr.nodeType} ${xr.name} ${xr.value}")
    }
}
