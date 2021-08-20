package dev.atsushieno.kotractive

/*

TODOs:

- Name character checks are not really done.
- Support for DoctypeDecl is incomplete.
- Support for XML Declaration is missing (if required; it is processed as PI).

CDATA and Whitespaces are treated as Text node. Instead, isCDATA property is introduced.

 */

interface IXmlLineInfo {
    val lineNumber : Int
    val linePosition : Int
}

class XmlException(message: String? = "XML error", innerException: Exception? = null, val lineNumber: Int = 0, val linePosition: Int = 0) : Exception(message, innerException) {
}

enum class XmlNodeType {
	Document,
	Doctype,
	Element,
	EndElement,
	Attribute,
	Text,
	Comment,
	ProcessingInstruction
}

abstract class XmlReader : IXmlLineInfo {
	companion object {
		fun create(text: String) : XmlReader =
			XmlTextReader(text)
	}

    abstract fun read() : Boolean

    abstract val eof : Boolean
    abstract val depth : Int
    abstract val localName: String
    abstract val namespaceUri: String
    abstract val prefix: String
    open val name
    	get() = if (prefix.isNotEmpty()) "$prefix:$localName" else localName
    abstract val nodeType: XmlNodeType
    abstract val value: String
    abstract val isEmptyElement: Boolean
	abstract val isCDATA: Boolean
    abstract val attributeCount: Int

    abstract fun close()

    open fun moveToContent() : Boolean {
		moveToElement()
    	while (true) {
			when (nodeType) {
				XmlNodeType.Element, XmlNodeType.EndElement -> return true
				XmlNodeType.Text -> if (isCDATA || value.all { " \t\r\n".indexOf(it) < 0 }) return true
				else -> {}
			}
			if (!read())
				return false
		}
    }

    fun readStartElement() = readStartElement(null)
    fun readStartElement(name: String?) = readStartElement(name, null)
    fun readStartElement(localName: String?, namespaceUri: String?) {
        moveToContent()
        if (nodeType != XmlNodeType.Element)
            throw IllegalStateException("XmlReader is not at element: $nodeType")
        if (localName != null && localName != this.localName || namespaceUri != null && namespaceUri != this.namespaceUri)
            throw IllegalStateException("Expecting XmlReader at '$localName' element in '$namespaceUri' namespace, got '${this.localName}' element in '${this.namespaceUri}' instead")
        read()
    }

    fun readElementContentAsString() : String {
        if (isEmptyElement) {
            read()
            return ""
        }
        var content = "" // somewhat inefficient but there is usually one single text node.
        val startDepth = depth
        readStartElement()
        while(startDepth < depth) {
			if (nodeType == XmlNodeType.Text)
				content += value
			read()
		}
		readEndElement()
        return content
    }

	open fun readEndElement() {
		if (nodeType != XmlNodeType.EndElement)
			throw XmlException("Unexpected call to readEndElement() at $nodeType node.")
		read()
	}

	abstract fun moveToElement()
	abstract fun moveToFirstAttribute() : Boolean
	abstract fun moveToNextAttribute() : Boolean
}

class XmlTextReader(text: String, baseUri: String? = null) : XmlReader() {

	private class Source(val text: String) : IXmlLineInfo {

		private var line = 0
		private var column = -1

		override val lineNumber: Int
			get() = line
		override val linePosition: Int
			get() = column

		var index: Int = -1

		val canRead : Boolean
			get() = index + 1 < text.length

		fun peekChar() = text[index + 1]

		fun readChar() : Char {
			if (!canRead)
				throw XmlException("Attempt to read XML stream beyond its input length: $index")
			++index
			column++
			if (text[index] == '\n') {
				line++
				column = 0
			}
			return text[index]
		}
	}

	private class TokenReader(val source: Source) {

		fun expect(vararg chars: Char) {
			for (ech in chars) {
				val ch = source.readChar()
				if (ch != ech)
					throw XmlException("Unexpected character: '$ch' (expected '$ech')", null, source.lineNumber, source.linePosition)
			}
		}

		var nameBuffer = CharArray(128)
		fun readName() : String {
			var pos = 0
			while (true) {
				val ch = source.peekChar()
				// FIXME: full name character validation is missing
				if (ch in '0'..'9' || ch in 'a'..'z' || ch in 'A'..'Z' || ch >= Char(0x100)) {
					if (nameBuffer.size == pos)
						nameBuffer = CharArray(nameBuffer.size * 2).also { nameBuffer.copyInto(it, 0, nameBuffer.size) }
					nameBuffer[pos++] = source.readChar()
				}
				else
					return nameBuffer.concatToString(0, pos)
			}
		}

		fun expectAny(error: String, vararg candidates: Char) {
			val ch = source.peekChar()
			if (!candidates.contains(ch))
				throw XmlException(error, null, source.lineNumber, source.linePosition)
		}

		fun readWhitespaces() : String {
			var pos = 0
			while(source.canRead) {
				when (source.peekChar()) {
					' ', '\t', '\r', '\n' -> {
						if (valueBuffer.size == pos)
							valueBuffer = CharArray(valueBuffer.size * 2).also { valueBuffer.copyInto(it, 0, nameBuffer.size) }
						valueBuffer[pos++] = source.readChar()
					}
					else -> break
				}
			}
			return valueBuffer.concatToString(0, pos)
		}

		var valueBuffer = CharArray(1024)
		// If ch2 is empty, then it does not consume ch1.
		// If ch2 is NOT empty, then it does consume ch1, but not ch2.
		// It is due to peek limitation.
		fun readUntil(ch1: Char, ch2: Char = '\u0000') : String {
			var pos = 0
			while (true) {
				if (valueBuffer.size == pos)
					valueBuffer = CharArray(valueBuffer.size * 2).also { valueBuffer.copyInto(it, 0, nameBuffer.size) }
				if (source.peekChar() != ch1) {
					valueBuffer[pos++] = source.readChar()
				} else {
					// optional check; if it is \0 then do not consume the first character here.
					// (Due to the way how we consume the characters, it is impossible to read
					// 3 or more chars with this function.)
					if (ch2 == '\u0000')
						break
					source.readChar()
					if (source.peekChar() != ch2)
						valueBuffer[pos++] = ch1
					else
						break
				}
			}
			return valueBuffer.concatToString(0, pos)
		}
	}

	override fun close() {} // nothing particular to do

	override val lineNumber
		get() = reader.source.lineNumber
	override val linePosition
		get() = reader.source.linePosition

	var namespaces = true

	private val reader = TokenReader(Source(text))
	private val nsmgr = XmlNamespaceManager()

	override val localName: String
		get() = currentNode.localName
	override val namespaceUri: String
		get() {
			if (!namespaces)
				return ""
			if (nodeType == XmlNodeType.Attribute)
				if (prefix == "" && localName == "xmlns" || prefix == "xmlns")
					return XmlNamespaceManager.Xmlns2000
			return nsmgr.lookupNamespace(prefix) ?: ""
		}
	override val prefix: String
		get() = currentNode.prefix
	override val nodeType: XmlNodeType
		get() = currentNode.nodeType
	override val value: String
		get() = unescapeCharacterEntities(currentNode.value)
	override val isEmptyElement: Boolean
		get() = currentNode.isEmptyElement
	override val isCDATA: Boolean
		get() = currentNode.isCDATA
	override val attributeCount: Int
		get() = attCount

	class XmlNodeState {
		var localName = ""
		var prefix = ""
		var value = ""
		var nodeType = XmlNodeType.Document
		var isEmptyElement = false
		var isCDATA = false
		var lineNumber = 0
		var linePosition = 0
	}

	private val nodes = mutableListOf(XmlNodeState())
	private val attributes = mutableListOf<XmlNodeState>()
	private var currentAttribute: Int = -1
	private var attCount = 0

	private val currentNode : XmlNodeState
		get() = if (currentAttribute >= 0) attributes[currentAttribute] else nodes.lastOrNull() ?: throw XmlException("This XmlReader is not read yet.")

	override val eof
		get() = !reader.source.canRead

	override val depth
		get() = nodes.size - 1 + if (currentAttribute >= 0) 1 else 0

	override fun moveToElement() {
		currentAttribute = -1
	}

	override fun moveToFirstAttribute(): Boolean {
		if (attributeCount == 0)
			return false
		currentAttribute = 0
		return true
	}

	override fun moveToNextAttribute(): Boolean {
		if (attCount == currentAttribute + 1)
			return false
		currentAttribute++
		return true
	}

	override fun read(): Boolean {
		moveToElement()
		if (nodes.size == 0 || (currentNode.nodeType == XmlNodeType.Element && !currentNode.isEmptyElement)) {
			nsmgr.pushScope()
			nodes.add(XmlNodeState()) // new slot for a child node
		}
		else {
			if (nodeType == XmlNodeType.EndElement)
				nsmgr.popScope()
			else if (isEmptyElement)
				nsmgr.clearInScopeNamespaces()
		}
		currentNode.lineNumber = reader.source.lineNumber
		currentNode.linePosition = reader.source.linePosition
		currentNode.isEmptyElement = false
		currentNode.isCDATA = false
		attCount = 0

		val ws = reader.readWhitespaces()
		if (!reader.source.canRead)
			return false

		if (reader.source.peekChar() == '<') {
			reader.source.readChar()
			when (reader.source.peekChar()) {
				'!' -> {
					reader.source.readChar()
					if (reader.source.peekChar() == '-')
						internalReadComment()
					else if (reader.source.peekChar() == '[')
						internalReadCDATA()
					else
						internalReadDoctypeDecl()
				}
				'?' -> internalReadPIOrXmlDeclaration()
				'/' -> internalReadEndElement()
				else -> internalReadStartElement()
			}
		}
		else
			internalReadText(ws)
		return true
	}

	private fun internalReadPIOrXmlDeclaration() {
		reader.expect('?')
		currentNode.prefix = ""
		currentNode.localName = reader.readName()
		reader.readWhitespaces()
		currentNode.value = reader.readUntil('?', '>')
		reader.expect('>')
		currentNode.nodeType = XmlNodeType.ProcessingInstruction
	}

	private fun readQName(node: XmlNodeState) {
		if (!namespaces) {
			node.prefix = ""
			node.localName = ""
			// Without namespace support, the tag name can start with ':' and can occur many times...
			if (reader.source.peekChar() == ':') {
				reader.source.readChar()
				node.localName = ":"
			}
			while (true) {
				node.localName += reader.readName()
				if (reader.source.peekChar() == ':')
					node.localName += reader.source.readChar()
				else
					break
			}
		} else {
			val nameOrPrefix = reader.readName()
			if (reader.source.peekChar() == ':') {
				reader.source.readChar()
				node.prefix = nameOrPrefix
				node.localName = reader.readName()
			} else {
				node.prefix = ""
				node.localName = nameOrPrefix
			}
		}
	}

	private fun internalReadEndElement() {
		nodes.removeLast()

		reader.expect('/')
		readQName(currentNode)
		reader.readWhitespaces()
		reader.expect('>')

		currentNode.value = ""
		currentNode.nodeType = XmlNodeType.EndElement
	}

	private fun internalReadText(ws: String) {
		currentNode.prefix = ""
		currentNode.localName = ""
		currentNode.value = ws + reader.readUntil('<')
		currentNode.nodeType = XmlNodeType.Text
	}

	private fun internalReadStartElement() {
		currentNode.isEmptyElement = false
		readQName(currentNode)

		while (true) {
			val ws = reader.readWhitespaces()
			when (reader.source.peekChar()) {
				'/' -> {
					reader.source.readChar()
					currentNode.isEmptyElement = true
					break
				}
				'>' -> break
				else -> {
					if (ws.isEmpty())
						throw XmlException("Whitespaces are missing before attribute name", null, reader.source.lineNumber, reader.source.linePosition)
					readAttribute()
				}
			}
		}

		reader.expect('>')

		currentNode.value = ""
		currentNode.nodeType = XmlNodeType.Element

		// fill namespaces
		for (attr in 0 until attributeCount) {
			if (attributes[attr].prefix == "xmlns")
				nsmgr.addNamespace(attributes[attr].localName, attributes[attr].value)
			else if (attributes[attr].prefix == "" && attributes[attr].localName == "xmlns")
				nsmgr.addNamespace("", attributes[attr].value)
		}
	}

	private fun readAttribute() {
		if (attributes.size == attCount)
			attributes.add(XmlNodeState())
		val att = attributes[attCount]
		readQName(att)

		reader.readWhitespaces()
		reader.expect('=')
		reader.readWhitespaces()
		val wrapper = reader.source.peekChar()
		reader.expectAny("Attribute value must begin with single-quote character or double-quote character", '\'', '"')
		reader.source.readChar()
		att.value = reader.readUntil(wrapper)
		reader.source.readChar()
		att.nodeType = XmlNodeType.Attribute

		attCount++
	}

	private fun internalReadComment() {
		reader.expect('-', '-')
		currentNode.value = reader.readUntil('-', '-')
		currentNode.nodeType = XmlNodeType.Comment
		currentNode.prefix = ""
		currentNode.localName = ""
		reader.expect('-', '>')
	}

	private fun internalReadCDATA() {
		reader.expect('[', 'C', 'D', 'A', 'T', 'A', '[')
		// FIXME: this is a commonly known bug in XML parser that it should NOT skip sequence like `]]]>`
		currentNode.value = reader.readUntil(']', ']')
		currentNode.nodeType = XmlNodeType.Text
		currentNode.prefix = ""
		currentNode.localName = ""
		currentNode.isCDATA = true
		reader.expect(']', '>')
	}

	private fun internalReadDoctypeDecl() {
		// FIXME: it is incomplete
		reader.expect('D', 'O', 'C', 'T', 'Y', 'P', 'E')
		currentNode.value = reader.readUntil('>')
		currentNode.nodeType = XmlNodeType.Doctype
		currentNode.prefix = ""
		currentNode.localName = ""
		reader.source.readChar()
	}

	private fun unescapeCharacterEntities(s: String) =
		s.replace("&amp;", "&")
			.replace("&lt;", "<")
			.replace("&gt;", ">")
			.replace("&quot;", "\"")
			.replace("&apos;", "\'")
}
