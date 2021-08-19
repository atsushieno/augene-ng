package dev.atsushieno.augene

import org.junit.Test
import kotlin.test.assertEquals

class XLinqTest {
    @Test
    fun basicOperations() {
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
}