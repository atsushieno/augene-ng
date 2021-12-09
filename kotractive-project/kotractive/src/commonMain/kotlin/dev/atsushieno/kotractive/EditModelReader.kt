package dev.atsushieno.kotractive

import dev.atsushieno.missingdot.xml.IXmlLineInfo
import dev.atsushieno.missingdot.xml.XmlException
import dev.atsushieno.missingdot.xml.XmlNodeType
import dev.atsushieno.missingdot.xml.XmlReader
import dev.atsushieno.missingdot.xml.XmlTextReader

class EditModelReader {
    companion object {
        fun toPascalCase(camelCase: String): String {
            return if (camelCase.isEmpty()) camelCase else camelCase.substring(0, 1).uppercase() + camelCase.substring(1)
        }

        fun toHexBinary(value: String): Sequence<Byte> {
            return sequence {
                var i = 0
                while (i < value.length) {
                    yield(value.substring(i, i + 2).toByte(16))
                    i += 2
                }
            }
        }
    }

    fun GetTypedValue(pi: PropertyInfo, value: String, li: IXmlLineInfo?): Any? {
        val type = pi.dataType

        println("!!!!!! " + pi.name)
        println("!!!!!! " + pi.propertyMetaType)
        if (pi.propertyMetaType.simpleName == "ByteArray") {
            // Actually JUCE base64 serialization is weird, it fails to deserialize their base64 String.
            return when (type) {
                DataType.Base64Binary -> Base64.decode(value, 0)
                DataType.HexBinary -> toHexBinary(value).asIterable().toList().toByteArray()
                else -> throw XmlException(
                    "Missing DataType attribute on byte array.",
                    null,
                    li?.lineNumber ?: 0,
                    li?.linePosition ?: 0
                )
            }
        }

        when (pi.propertyMetaType.typeCode) {
            TypeCode.String ->
                return value
            TypeCode.Boolean -> {
                when (value) {
                    "1" -> return true
                    "0" -> return false
                }
                throw  XmlException("Invalid value for boolean", null, li?.lineNumber ?: 0, li?.linePosition ?: 0)
            }
            TypeCode.Double -> {
                val d = value.toDoubleOrNull()
                if (d != null)
                    return d
                throw  XmlException("Invalid value for number", null, li?.lineNumber ?: 0, li?.linePosition ?: 0)
            }
            TypeCode.Int32 -> {
                val i = value.toIntOrNull()
                if (i != null)
                    return i
                throw  XmlException("Invalid value for int", null, li?.lineNumber ?: 0, li?.linePosition ?: 0)
            }
            TypeCode.Int64 -> {
                val l = value.toLongOrNull(16)
                if (l != null)
                    return l
                throw  XmlException("Invalid value for long", null, li?.lineNumber ?: 0, li?.linePosition ?: 0)
            }
			else -> {}
        }

        throw XmlException("Unexpected data for $pi", null, li?.lineNumber ?: 0, li?.linePosition ?: 0)
    }

    fun read(reader: XmlReader): EditElement {
        return doRead(reader) as EditElement
    }

    fun doRead(reader: XmlReader): Any {
        println("!!!!!! " + reader.nodeType)
        val li = reader as IXmlLineInfo?
        reader.moveToContent()
        if (reader.nodeType == XmlNodeType.Element) {
            val typeName = reader.localName + "Element"
            val type = ModelCatalog.allTypes.firstOrNull { t -> t.simpleName.equals(typeName, true) }
                ?: throw XmlException("Type $typeName does not exist", null, li?.lineNumber ?: 0, li?.linePosition ?: 0)
            val obj = type.newInstance()
            if (reader.moveToFirstAttribute()) {
                do {
                    // JUCE XML is awkward and is invalid if XML namespace is enabled, because it lacks namespace declaration for "base64" prefix.
                    // To workaround that problem, we use XmlTextReader that can disable namespace handling (XmlTextReader.Namespaces = false).
                    // Therefore, "base64:layout" attribute is parsed as "prefix = '', localname='base64:layout'".
                    val propName = reader.localName.split(':').map { s -> toPascalCase(s) }.joinToString("_")
                    //var propName = (String.IsNullOrEmpty (reader.Prefix) ? "" : ToPascalCase (reader.Prefix) + '_') + ToPascalCase (reader.LocalName);
                    val prop = type.getProperty(propName)
                        ?: throw XmlException(
                            "In $type, property $propName not found.",
                            null,
                            li?.lineNumber ?: 0,
                            li?.linePosition ?: 0
                        )
                    prop.setValue(obj, GetTypedValue(prop, reader.value, li))
                } while (reader.moveToNextAttribute())
            }
            reader.moveToElement()

            println("!!!!!! " + reader.isEmptyElement)
            if (reader.isEmptyElement) {
                reader.read()
                return obj
            }
            reader.read()
            reader.moveToContent()
            while (reader.nodeType != XmlNodeType.EndElement) {
                println("!!!!!! EE")
                val propTypeName = reader.localName + "Element"
                var prop = type.properties.firstOrNull { p -> p.propertyMetaType.simpleName.equals(propTypeName, true) }
                if (prop != null)
                    prop.setValue(obj, doRead(reader))
                else {
                    val itemObj = doRead(reader)
                    prop = type.properties
                        .filter { p -> p.propertyMetaType.simpleName.contains("MutableList") }
                        .map { p ->
                            object {
                                val property = p
                                val itemType = p.listItemType!!
                            }
                        }
                        .firstOrNull { m -> m.itemType.isAssignableFrom(itemObj.getMetaType()) }?.property
                    if (prop == null)
                        throw  XmlException(
                            "In $type, property of collection of type $propTypeName not found.",
                            null,
                            li?.lineNumber ?: 0,
                            li?.linePosition ?: 0
                        )
                    val propValue = prop.getValue(obj)
                        ?: throw XmlException(
                            "In $type, property $prop has null value unexpectedly.",
                            null,
                            li?.lineNumber ?: 0,
                            li?.linePosition ?: 0
                        )
                    prop.addListItem(propValue, itemObj)
                }
                reader.moveToContent()
            }
            println("!!!!!!~ " + reader.nodeType)
            reader.readEndElement()
            return obj
        }
        throw XmlException(
            "Unexpected XML content ${reader.nodeType} ${reader.name}.",
            null,
            li?.lineNumber ?: 0,
            li?.linePosition ?: 0
        )
    }
}
