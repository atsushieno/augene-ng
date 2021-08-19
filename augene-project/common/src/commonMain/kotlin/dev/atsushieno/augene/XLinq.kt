package dev.atsushieno.augene

import dev.atsushieno.kotractive.XmlNodeType
import dev.atsushieno.kotractive.XmlReader
import dev.atsushieno.kotractive.XmlWriter

/*

- Skip XName and XNamespace

 */

abstract class XNode internal constructor(val localName: String, val namespaceUri: String, val nodeType: XmlNodeType) {
    internal var internalParent: XContainer? = null
    val parent: XContainer? = internalParent

    override fun toString(): String {
        val sb = StringBuilder()
        val writer = XmlWriter.create(sb)
        writeXNode(writer, this)
        return sb.toString()
    }
}

abstract class XContainer internal constructor(localName: String, namespaceUri: String, nodeType: XmlNodeType)
    : XNode(localName, namespaceUri, nodeType) {

    private val children = mutableListOf<XNode>()

    val nodes : Iterable<XNode>
        get() = children

    val firstNode : XNode?
        get() = nodes.firstOrNull()

    val lastNode : XNode?
        get() = nodes.lastOrNull()

    val nextNode : XNode?
        get() = parent?.nodes?.dropWhile { it != this }?.drop(1)?.firstOrNull()

    fun element(localName: String) = elements(localName).firstOrNull()

    fun element(localName: String, namespaceUri: String) = elements(localName, namespaceUri).firstOrNull()

    fun elements() : Iterable<XElement> = nodes.filterIsInstance<XElement>()

    fun elements(localName: String) = elements(localName, "")

    fun elements(localName: String, namespaceUri: String) = elements().filter { it.localName == localName && it.namespaceUri == namespaceUri }

    fun add(node: XNode) {
        node.internalParent = this
        children.add(node)
    }

    fun add(index: Int, node: XNode) {
        node.internalParent = this
        children.add(index, node)
    }

    fun remove(node: XNode) {
        node.internalParent = null
        children.remove(node)
    }

    fun removeAt(index: Int) {
        children[index].internalParent = null
        children.removeAt(index)
    }
}

private fun readXNode(reader: XmlReader) : XNode {
    return when (reader.nodeType) {
        XmlNodeType.Element -> XElement.load(reader)
        XmlNodeType.Comment -> XComment(reader.value).also { reader.read() }
        XmlNodeType.ProcessingInstruction -> XProcessingInstruction(reader.value).also { reader.read() }
        XmlNodeType.Doctype -> TODO("DocumentType node is not supported yet")
        XmlNodeType.EndElement -> throw IllegalStateException("XmlReader is positioned at EndElement")
        XmlNodeType.Text -> XText(reader.value).also { reader.read() }
        else -> throw IllegalStateException("XmlReader is positioned at ${reader.nodeType}")
    }
}

private fun writeXNode(writer: XmlWriter, node: XNode) {
    when (node.nodeType) {
        XmlNodeType.Document ->
            (node as XDocument).nodes.forEach { writeXNode(writer, it) }
        XmlNodeType.Element -> {
            val el = node as XElement
            writer.writeStartElement(el.localName, el.namespaceUri)
            el.attributes().forEach { writer.writeAttributeString(it.localName, it.namespaceUri, it.value) }
            el.nodes.forEach { writeXNode(writer, it) }
            writer.writeEndElement()
        }
        XmlNodeType.Text -> writer.writeString((node as XText).value)
        XmlNodeType.Comment -> writer.writeComment((node as XComment).value)
        XmlNodeType.ProcessingInstruction -> (node as XProcessingInstruction).also {
            writer.writeProcessingInstruction(it.localName, it.value)
        }
        else -> {}
    }
}

class XDocument : XContainer("", "", XmlNodeType.Document) {
    companion object {
        fun load(reader: XmlReader) : XDocument {
            val doc = XDocument()
            while(reader.nodeType != XmlNodeType.Element)
                doc.add(readXNode(reader))
            doc.add(XElement.load(reader))
            return doc
        }
    }

    var root : XElement?
        get() = nodes.filterIsInstance<XElement>().firstOrNull()
        set(n) {
            val r = root
            if (r != null) {
                val index = nodes.indexOf(r)
                removeAt(index)
                if (n != null)
                    add(index, n)
            }
            else if (n != null)
                add(n)
        }
}

class XElement(localName: String, ns: String = ""): XContainer(localName, ns, XmlNodeType.Element) {
    companion object {
        fun load(reader: XmlReader) : XElement {
            val el = XElement(reader.localName, reader.namespaceUri)
            if (reader.moveToFirstAttribute()) {
                while (true) {
                    el.addAttribute(XAttribute(reader.localName, reader.namespaceUri, reader.value))
                    if (!reader.moveToNextAttribute())
                        break
                }
                reader.moveToElement()
            }
            if (reader.isEmptyElement) {
                reader.read()
            } else {
                reader.read()
                while (reader.nodeType != XmlNodeType.EndElement)
                    el.add(readXNode(reader))
                reader.readEndElement()
            }
            return el
        }
    }

    private val atts = mutableListOf<XAttribute>()

    fun attributes() : List<XAttribute> = atts

    // This does not exist in .NET
    fun addAttribute(attribute: XAttribute) {
        atts.add(attribute)
    }

    fun attribute(localName: String) = attribute(localName, "")

    fun attribute(localName: String, namespaceUri: String) : XAttribute? =
        atts.firstOrNull { it.localName == localName && it.namespaceUri == namespaceUri }

    val value : String =
        nodes.map {
            when (it.nodeType) {
                XmlNodeType.Element -> (it as XElement).value
                XmlNodeType.Text -> (it as XText).value
                else -> ""
            }
        }.joinToString { "" }
}

class XComment(val value: String) : XNode("", "", XmlNodeType.Comment)

class XProcessingInstruction(val value: String) : XNode("", "", XmlNodeType.ProcessingInstruction)

class XAttribute(val localName: String, val namespaceUri: String, val value: String)

class XText(val value: String) : XNode("", "", XmlNodeType.Text)
