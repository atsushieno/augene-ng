package dev.atsushieno.augene

import org.junit.Test
import dev.atsushieno.kotractive.XmlReader
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class XLinqTest {
    @Test
    fun basicWriteOperations() {
        val doc = XDocument()
        val root = XElement("root")
        doc.root = root
        assertEquals("<root />", doc.toString(), "xml1")

        root.addAttribute(XAttribute("a", "", "v"))
        root.add(XElement("child1"))
        root.add(XElement("child2").also { it.add(XElement("child3")) })

        assertEquals("<root a=\"v\"><child1 /><child2><child3 /></child2></root>",
            doc.root.toString(), "xml2")
    }

    @Test
    fun basicReadOperation() {
        val xml = "<root a=\"v\"><child1 /><child2><child3 />text</child2></root>"
        val doc = XDocument.load(XmlReader.create(xml))
        val root = doc.root!!
        assertEquals("root", root.localName, "root name")
        val child1 = root.element("child1")!!
        assertEquals("child1", child1.localName, "child1")
        assertEquals("v", root.attribute("a")?.value, "a")
        val child2 = root.element("child2")!!
        assertEquals("child2", child2.localName, "child2")
        val child3 = child2.element("child3")!!
        assertEquals("child3", child3.localName, "child3")
        assertEquals("text", child2.value, "child2 value")
    }
}