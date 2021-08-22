package dev.atsushieno.kotractive

import dev.atsushieno.missingdot.xml.XmlWriter
import dev.atsushieno.missingdot.xml.XmlTextWriter

class EditModelWriter {
    companion object {
        fun toCamelCase(pascalCase: String) =
            if (pascalCase.isEmpty()) pascalCase else pascalCase.substring(0, 1).lowercase() + pascalCase.substring(1)

        private fun toHexBinaryString(value: ByteArray) =
            value.map { v -> v.toString(16) }.joinToString { if (it.length == 1) "0$it" else it }

        fun toValueString(pi: PropertyInfo, obj: Any?): String? {
            if (obj == null)
                return null
            if (obj is ByteArray)
                return toHexBinaryString(obj)

			return when (pi.propertyMetaType.typeCode) {
				TypeCode.Boolean -> if (obj is Boolean) "1" else "0"
				else -> obj.toString()
			}
        }
    }

    private fun isIList(metaType: MetaType) = metaType.simpleName.contains("MutableList")

    private fun isListPropertyType(metaType: MetaType): Boolean {
        // only care about non-primitives. arrays and strings should not return true here.
        when (metaType.typeCode) {
            TypeCode.Object -> {
            }
            else -> return false
        }
        if (metaType.simpleName == "ByteArray")
            return false

        var t: MetaType? = metaType
        while (t != null) {
            // if you don't hack around it, it's going to be super messy...
            if (isIList(t))// || t.GetInterfaces ().any { x -> IsIList(x) })
                return true
            t = t.baseMetaType
        }
        return false
    }

    fun write(stringBuilder: StringBuilder, o: Any) {
        XmlTextWriter(stringBuilder).apply { namespaces = false }.also {
            write(it, o, null)
        }
    }

    fun write(writer: XmlWriter, o: Any?, hintProperty: PropertyInfo?) {
        if (o == null)
            return

        val typeName: String = o.getMetaType().simpleName
        if (typeName.endsWith("Element", false)) {
            val elementName = typeName.substring(0, typeName.length - "Element".length).uppercase()
            // write as element
            writer.writeStartElement(elementName)
            //attributes
            val listProps = o.getMetaType().properties.filter { p -> isListPropertyType(p.propertyMetaType) }
            val attProps = o.getMetaType().properties.filter { p -> !listProps.contains(p) }
                .filter { p -> !p.propertyMetaType.simpleName.endsWith("Element", true) }
            val nonListElementProps =
                o.getMetaType().properties.filter { p -> !attProps.contains(p) }.filter { p -> !listProps.contains(p) }
            for (prop in attProps) {
                // see EditModelReader.cs for XML namespace problem...
                val attrName = toCamelCase(prop.name).replace('_', ':')
                val value = prop.getValue(o)
                if (value != null)
                    writer.writeAttributeString(attrName, toValueString(prop, value) ?: "")
            }
            // elements
            for (prop in nonListElementProps) {
                write(writer, prop.getValue(o), prop)
            }
            for (prop in listProps) {
                val list = prop.getValue(o) as Iterable<*>
                for (item in list)
                    write(writer, item, prop)
            }
            writer.writeEndElement()
        } else
            throw IllegalArgumentException("For property '$hintProperty', unexpected object element appeared: ${o.getMetaType()}")
    }
}
