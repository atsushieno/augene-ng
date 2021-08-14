package dev.atsushieno.kotractive

import kotlin.test.Test
import kotlin.test.assertEquals

class XmlWriterTest {
    @Test
    fun write1() {
        val sb = StringBuilder()
        val xw = XmlWriter.create(sb)
        xw.quoteChar = '\''
        assertEquals(WriteState.Start, xw.writeState, "start state")
        xw.writeStartDocument()
        xw.writeComment("--TEST--")
        xw.writeStartElement("root")
        xw.writeStartAttribute("a")
        xw.writeString("A")
        xw.writeEndAttribute()
        xw.writeAttributeString("b", "", "B")
        xw.writeAttributeString("xmlns", "c", XmlNamespaceManager.Xmlns2000, "urn:foo")
        xw.writeAttributeString("c", "c", "urn:foo", "C")
        xw.writeStartElement("child")
        xw.writeCData("TEST]]>")
        xw.writeStartElement("descendant")
        xw.writeAttributeString("d", "D")
        xw.writeEndDocument()
        assertEquals("<?xml ?><!---&#x2D;TEST-&#x2D;--><root a='A' b='B' xmlns:c='urn:foo' c:c='C'><child><![CDATA[TEST]]&gt;]]><descendant d='D' /></child></root>", sb.toString(), "XML")
    }
}

