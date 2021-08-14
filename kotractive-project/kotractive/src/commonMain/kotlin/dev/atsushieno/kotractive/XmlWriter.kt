package dev.atsushieno.kotractive

enum class XmlSpace {
    None,
    Default,
    Preserve
}

enum class WriteState {
    Start,
    Prolog,
    Element,
    Attribute,
    Content,
    Closed,
    Error,
}

abstract class XmlWriter {
    companion object {
        fun create(output: StringBuilder) = XmlTextWriter(output)
    }

    abstract val writeState : WriteState

    open val xmlLang : String? = null

    open val xmlSpace = XmlSpace.None

    abstract fun close()

    abstract fun lookupPrefix(ns: String) : String?

    abstract fun writeStartDocument(encoding: String? = null, standalone: Boolean? = null)

    abstract fun writeEndDocument()

    abstract fun writeDoctype(name: String, publicId: String?, systemId: String?, internalSubset: String?)

    fun writeStartElement(name: String) = writeStartElement("", name, "")
    fun writeStartElement(localName: String, namespaceUri: String) = writeStartElement(null, localName, namespaceUri)
    abstract fun writeStartElement(prefix: String?, localName: String, namespaceUri: String)

    abstract fun writeEndElement()

    abstract fun writeFullEndElement() // awkward name...

    fun writeStartAttribute(name: String) = writeStartAttribute("", name, "")
    abstract fun writeStartAttribute(prefix: String?, localName: String, namespaceUri: String)

    abstract fun writeEndAttribute()

    abstract fun writeCData(text: String)

    abstract fun writeEntityRef(name: String) // awkward name...

    abstract fun writeComment(comment: String)

    abstract fun writeProcessingInstruction(name: String, value: String?)

    open fun writeQualifiedName(localName: String, namespaceUri: String) {
        val prefix = lookupPrefix(namespaceUri)
        writeString(if (prefix != null && !prefix.isEmpty()) "$prefix:$localName" else localName)
    }

    abstract fun writeRaw(text: String)

    abstract fun writeString(text: String)

    abstract fun writeWhitespace(text: String)

    open fun writeAttributes(reader: XmlReader, writeDefaultAttributes: Boolean) {
        TODO("Implement")
    }

    fun writeAttributeString(name: String, value: String) = writeAttributeString("", name, "", value)
    fun writeAttributeString(localName: String, namespaceUri: String, value: String) = writeAttributeString(null, localName, namespaceUri, value)
    fun writeAttributeString(prefix: String?, localName: String, namespaceUri: String, value: String) {
        writeStartAttribute(prefix, localName, namespaceUri)
        writeString(value)
        writeEndAttribute()
    }

    fun writeElementString(name: String, value: String) = writeElementString("", name, "", value)
    fun writeElementString(localName: String, namespaceUri: String, value: String) = writeElementString(null, localName, namespaceUri, value)
    fun writeElementString(prefix: String?, localName: String, namespaceUri: String, value: String) {
        writeStartElement(prefix, localName, namespaceUri)
        writeString(value)
        writeEndElement()
    }


}

class XmlTextWriter(private val output: StringBuilder) : XmlWriter() {

    private var state = WriteState.Start

    private var openAttribute = false
    private var openElement = false
    private val openTags = mutableListOf<String>()

    var namespaces = true
    var quoteChar = '"'

    val nsmgr = XmlNamespaceManager()

    override val writeState: WriteState
        get() = state

    override fun close() {
        TODO("Not yet implemented")
    }

    override fun lookupPrefix(ns: String): String? =
        if (!namespaces) null else nsmgr.lookupPrefix(ns)

    override fun writeStartDocument(encoding: String?, standalone: Boolean?) {
        if (state != WriteState.Start)
            throw XmlException("XmlTextWriter is not at Start state ($state)")
        state = WriteState.Prolog

        output.append("<?xml")
        if (encoding != null)
            output.append(" encoding").append(quoteChar).append(encoding).append(quoteChar)
        if (standalone != null)
            output.append(" standalone").append(quoteChar).append(if (standalone) "yes" else "no").append(quoteChar)
        output.append(" ?>")
    }

    private fun checkState() {
        if (state == WriteState.Error)
            throw XmlException("XmlTextWriter is at Error state")
        if (state == WriteState.Closed)
            throw XmlException("XmlTextWriter is already at Closed state")
    }

    override fun writeEndDocument() {
        checkState()

        while (openTags.any())
            writeEndElement()

        state = WriteState.Closed
    }

    override fun writeDoctype(name: String, publicId: String?, systemId: String?, internalSubset: String?) {
        checkState()
        if (state != WriteState.Start && state != WriteState.Prolog)
            throw XmlException("XmlTextWriter is already at $state state")
        state = WriteState.Element

        output.append("<!DOCTYPE")
        if (publicId != null)
            output.append(" PUBLIC ").append(quoteChar).append(publicId).append(quoteChar)
        if (systemId != null)
            output.append(" SYSTEM ").append(quoteChar).append(systemId).append(quoteChar)
        if (internalSubset != null)
            output.append(quoteChar).append(internalSubset).append(quoteChar)
        output.append(">")
    }

    private fun checkAndCloseStartTagIfOpen(skipTagClosing: Boolean = false) {
        checkState()
        if (openAttribute)
            writeEndAttribute()
        if (state == WriteState.Attribute && !skipTagClosing)
            output.append('>')
        state = WriteState.Content
        openElement = false
    }

    override fun writeStartElement(prefix: String?, localName: String, namespaceUri: String) {
        checkAndCloseStartTagIfOpen()

        if (namespaces && prefix != null && namespaceUri == XmlNamespaceManager.Xmlns2000)
            nsmgr.addNamespace(prefix, namespaceUri)

        val actualPrefix = prefix ?: if (namespaces) lookupPrefix(namespaceUri) ?: throw XmlException("No namespace prefix for \"$namespaceUri\" is declared in this XmlTextWriter.") else ""
        val tag = if (actualPrefix.isNotEmpty()) "$actualPrefix:$localName" else localName
        output.append("<").append(tag)
        openTags.add(tag)

        openElement = true
        state = WriteState.Attribute
    }

    override fun writeEndElement() {
        if (openElement) {
            checkAndCloseStartTagIfOpen(true)
            if (openTags.isEmpty())
                throw XmlException("Element is not started in this XmlTextWriter.")

            output.append(" />")
            openTags.removeLast()
        }
        else
            writeFullEndElement()
    }

    override fun writeFullEndElement() {
        checkAndCloseStartTagIfOpen()
        if (openTags.isEmpty())
            throw XmlException("Element is not started in this XmlTextWriter.")

        output.append("</").append(openTags.last()).append('>')
        openTags.removeLast()
    }

    override fun writeStartAttribute(prefix: String?, localName: String, namespaceUri: String) {
        checkState()
        if (openAttribute)
            throw XmlException("Attempt to write another XML attribute whilte writing an attribute.")
        state = WriteState.Attribute

        if (namespaces && prefix != null && namespaceUri == XmlNamespaceManager.Xmlns2000)
            nsmgr.addNamespace(prefix, namespaceUri)

        val actualPrefix = prefix ?: if (namespaces) lookupPrefix(namespaceUri) ?: throw XmlException("No namespace prefix for \"$namespaceUri\" is declared in this XmlTextWriter.") else ""
        val name = if (actualPrefix.isNotEmpty()) "$actualPrefix:$localName" else localName
        output.append(' ').append(name).append('=').append(quoteChar)

        openAttribute = true
    }

    override fun writeEndAttribute() {
        checkState()
        if (!openAttribute)
            throw XmlException("Attribute is not started in this XmlTextWriter.")
        openAttribute = false

        output.append(quoteChar)
    }

    override fun writeCData(text: String) {
        checkAndCloseStartTagIfOpen()

        output.append("<![CDATA[").append(text.replace("]]>", "]]&gt;")).append("]]>")
    }

    override fun writeComment(comment: String) {
        checkAndCloseStartTagIfOpen()

        output.append("<!--").append(comment.replace("--", "-&#x2D;")).append("-->")
    }

    override fun writeProcessingInstruction(name: String, value: String?) {
        checkState() // do not call checkAndCloseStartTagOfOpen() as it can stay at Start/Prolog/Element state.
        if (state == WriteState.Attribute)
            writeEndAttribute()

        output.append("<?").append(name)
        if (value != null)
            output.append(' ').append(quoteChar).append(escapeCharacterEntities(value)).append(quoteChar)
        output.append(" ?>")
    }

    override fun writeRaw(text: String) {
        checkState()
        if (state != WriteState.Attribute)
            state = WriteState.Content

        output.append(text)

        openElement = openElement && state == WriteState.Attribute
    }

    private fun escapeCharacterEntities(s: String) =
        s.replace("&", "&amp")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")

    override fun writeString(text: String) {
        writeRaw(escapeCharacterEntities(text))
    }

    override fun writeWhitespace(text: String) {
        if (text.any { " \t\r\n".indexOf(it) >= 0  })
            throw XmlException("Attempt to write non-whitespace string as whitespaces.")
        writeRaw(text)
    }

    override fun writeEntityRef(name: String) {
        // FIXME: check XML NameChars
        writeRaw("&$name;")
    }
}
