package dev.atsushieno.kotractive

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class XmlReaderTest {

    private fun assertNode(label: String, xr: XmlReader, depth: Int, nodeType: XmlNodeType, localName: String, ns: String, prefix: String, value: String) {
        assertEquals(depth, xr.depth, "$label depth")
        assertEquals(nodeType, xr.nodeType, "$label nodeType")
        assertEquals(localName, xr.localName, "$label localName")
        assertEquals(ns, xr.namespaceUri, "$label ns")
        assertEquals(prefix, xr.prefix, "$label prefix")
        assertEquals(value, xr.value, "$label value")
    }

    @Test
    fun read1() {
        val xr = XmlReader.create("<root/><!--comment-->")
        assertNode("initial", xr, 0, XmlNodeType.Document, "", "", "", "")
        assertTrue(xr.read(), "read1")
        assertNode("first", xr, 0, XmlNodeType.Element, "root", "", "", "")
        assertTrue(xr.isEmptyElement, "first isEmptyElement")
        assertEquals(0, xr.attributeCount, "first attributeCount")
        assertFalse(xr.moveToFirstAttribute(), "first moveToFirstAttribute")
        assertFalse(xr.moveToNextAttribute(), "first moveToNextAttribute")

        assertTrue(xr.read(), "read2")
        assertNode("second", xr, 0, XmlNodeType.Comment, "", "", "", "comment")
        assertFalse(xr.read(), "third read")
    }

    @Test
    fun read2() {
        val xr = XmlReader.create("<root a='1' b=\"2\" c='0\"1'></root>")
        assertTrue(xr.read(), "read1")
        assertNode("first", xr, 0, XmlNodeType.Element, "root", "", "", "")
        assertFalse(xr.isEmptyElement, "first isEmptyElement")
        assertEquals(3, xr.attributeCount, "first attributeCount")

        assertTrue(xr.moveToFirstAttribute(), "first moveToFirstAttribute")
        assertNode("first attr1", xr, 1, XmlNodeType.Attribute, "a", "", "", "1")

        assertTrue(xr.moveToNextAttribute(), "first moveToNextAttribute 1")
        assertNode("first attr2", xr, 1, XmlNodeType.Attribute, "b", "", "", "2")

        assertTrue(xr.moveToNextAttribute(), "first moveToNextAttribute 2")
        assertNode("first attr3", xr, 1, XmlNodeType.Attribute, "c", "", "", "0\"1")

        assertTrue(xr.read(), "second read")
        assertNode("second", xr, 0, XmlNodeType.EndElement, "root", "", "", "")

        assertFalse(xr.read(), "second read")
    }

    @Test
    fun read3() {
        val xr = XmlReader.create("<root><child /><child2></child2><child3>text&amp;&lt;&gt;&quot;&apos;</child3><![CDATA[cdata]]></root>")
        assertTrue(xr.read(), "1st read")
        assertNode("1st", xr, 0, XmlNodeType.Element, "root", "", "", "")
        assertFalse(xr.isEmptyElement, "1st isEmptyElement")
        assertEquals(0, xr.attributeCount, "1st attributeCount")

        assertTrue(xr.read(), "2nd read")
        assertNode("2nd", xr, 1, XmlNodeType.Element, "child", "", "", "")
        assertTrue(xr.isEmptyElement, "2nd isEmptyElement")

        assertTrue(xr.read(), "3rd read")
        assertNode("3rd", xr, 1, XmlNodeType.Element, "child2", "", "", "")
        assertFalse(xr.isEmptyElement, "3rd isEmptyElement")

        assertTrue(xr.read(), "4th read")
        assertNode("4th", xr, 1, XmlNodeType.EndElement, "child2", "", "", "")
        assertFalse(xr.isEmptyElement, "4th isEmptyElement")

        assertTrue(xr.read(), "5th read")
        assertNode("5th", xr, 1, XmlNodeType.Element, "child3", "", "", "")
        assertFalse(xr.isEmptyElement, "5th isEmptyElement")

        assertTrue(xr.read(), "6th read")
        assertNode("6th", xr, 2, XmlNodeType.Text, "", "", "", "text&<>\"'")
        assertFalse(xr.isEmptyElement, "6th isEmptyElement")
        assertFalse(xr.isCDATA, "6th isCDATA")

        assertTrue(xr.read(), "7th read")
        assertNode("7th", xr, 1, XmlNodeType.EndElement, "child3", "", "", "")
        assertFalse(xr.isEmptyElement, "7th isEmptyElement")

        assertTrue(xr.read(), "8th read")
        assertNode("8th", xr, 1, XmlNodeType.Text, "", "", "", "cdata")
        assertFalse(xr.isEmptyElement, "8th isEmptyElement")
        assertTrue(xr.isCDATA, "8th isCDATA")

        assertTrue(xr.read(), "9th read")
        assertNode("9th", xr, 0, XmlNodeType.EndElement, "root", "", "", "")
        assertFalse(xr.isEmptyElement, "9th isEmptyElement")

        assertFalse(xr.read(), "second read")
    }

    @Test
    fun lookupNamespace() {
        val xml = """
<root xmlns='urn:foo' xmlns:x='urn:x' x:a='A'>
  <child xmlns='urn:bar' xmlns:y='urn:y' x:b='B' y:c='C' />
  <child2 xmlns:y='urn:y2' y:d='D'>
    <child3 xmlns='urn:baz' y:e='E' />
    <y:child4 xmlns:y='urn:y3' />
    <y:child5 />
  </child2>
</root>
"""
        val xr = XmlReader.create(xml)
        assertTrue(xr.read(), "1st read")
        assertNode("1st", xr, 0, XmlNodeType.Element, "root", "urn:foo", "", "")
        assertTrue(xr.moveToFirstAttribute(), "1st move1")
        assertNode("1st att1", xr, 1, XmlNodeType.Attribute, "xmlns", "http://www.w3.org/2000/xmlns/", "", "urn:foo")
        assertTrue(xr.moveToNextAttribute(), "1st move2")
        assertNode("1st att2", xr, 1, XmlNodeType.Attribute, "x", "http://www.w3.org/2000/xmlns/", "xmlns", "urn:x")
        assertTrue(xr.moveToNextAttribute(), "1st move3")
        assertNode("1st att3", xr, 1, XmlNodeType.Attribute, "a", "urn:x", "x", "A")
        assertFalse(xr.moveToNextAttribute(), "1st move4")

        assertTrue(xr.read(), "2nd read")
        assertNode("2nd", xr, 1, XmlNodeType.Element, "child", "urn:bar", "", "")
        assertTrue(xr.moveToFirstAttribute(), "2nd move1")
        assertNode("2nd att1", xr, 2, XmlNodeType.Attribute, "xmlns", "http://www.w3.org/2000/xmlns/", "", "urn:bar")
        assertTrue(xr.moveToNextAttribute(), "2nd move2")
        assertNode("2nd att2", xr, 2, XmlNodeType.Attribute, "y", "http://www.w3.org/2000/xmlns/", "xmlns", "urn:y")
        assertTrue(xr.moveToNextAttribute(), "2nd move3")
        assertNode("2nd att3", xr, 2, XmlNodeType.Attribute, "b", "urn:x", "x", "B")
        assertTrue(xr.moveToNextAttribute(), "2nd move4")
        assertNode("2nd att4", xr, 2, XmlNodeType.Attribute, "c", "urn:y", "y", "C")
        assertFalse(xr.moveToNextAttribute(), "2nd move5")

        assertTrue(xr.read(), "3rd read")
        assertNode("3rd", xr, 1, XmlNodeType.Element, "child2", "urn:foo", "", "")
        assertTrue(xr.moveToNextAttribute(), "3rd move1")
        assertNode("3rd att1", xr, 2, XmlNodeType.Attribute, "y", "http://www.w3.org/2000/xmlns/", "xmlns", "urn:y2")
        assertTrue(xr.moveToNextAttribute(), "3rd move2")
        assertNode("3rd att2", xr, 2, XmlNodeType.Attribute, "d", "urn:y2", "y", "D")
        assertFalse(xr.moveToNextAttribute(), "3rd move3")

        assertTrue(xr.read(), "4th read")
        assertNode("4th", xr, 2, XmlNodeType.Element, "child3", "urn:baz", "", "")
        assertTrue(xr.moveToFirstAttribute(), "4th move1")
        assertNode("4th att1", xr, 3, XmlNodeType.Attribute, "xmlns", "http://www.w3.org/2000/xmlns/", "", "urn:baz")
        assertTrue(xr.moveToNextAttribute(), "4th move2")
        assertNode("4th att2", xr, 3, XmlNodeType.Attribute, "e", "urn:y2", "y", "E")
        assertFalse(xr.moveToNextAttribute(), "4th move3")

        assertTrue(xr.read(), "5th read")
        assertNode("5th", xr, 2, XmlNodeType.Element, "child4", "urn:y3", "y", "")
        assertTrue(xr.moveToFirstAttribute(), "5th move1")
        assertNode("5th att1", xr, 3, XmlNodeType.Attribute, "y", "http://www.w3.org/2000/xmlns/", "xmlns", "urn:y3")
        assertFalse(xr.moveToNextAttribute(), "5th move2")

        assertTrue(xr.read(), "6th read")
        assertNode("6th", xr, 2, XmlNodeType.Element, "child5", "urn:y2", "y", "")
        assertFalse(xr.moveToFirstAttribute(), "6th move1")
    }

    @Test
    fun namespacesFalse() {
        val xml = "<:::root::: :a:t:t:r='v'></:::root:::>"
        val xr = XmlTextReader(xml)
        xr.namespaces = false
        assertTrue(xr.read(), "1st read")
        assertNode("1st", xr, 0, XmlNodeType.Element, ":::root:::", "", "", "")
        assertTrue(xr.moveToFirstAttribute(), "1st move1")
        assertNode("1st att1", xr, 1, XmlNodeType.Attribute, ":a:t:t:r", "", "", "v")
        assertTrue(xr.read(), "2nd read")
        assertNode("2nd", xr, 0, XmlNodeType.EndElement, ":::root:::", "", "", "")
    }

    @Test
    fun readElementContentAsString() {
        val xml = "<root>test<child1>test2</child1>test3</root>"
        val xr = XmlTextReader(xml)
        xr.moveToContent()
        assertNode("1st", xr, 0, XmlNodeType.Element, "root", "", "", "")
        val text = xr.readElementContentAsString()
        assertEquals("testtest2test3", text, "text")
    }
}
