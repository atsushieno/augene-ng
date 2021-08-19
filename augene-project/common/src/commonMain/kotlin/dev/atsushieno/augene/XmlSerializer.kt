package dev.atsushieno.augene

import dev.atsushieno.kotractive.XmlReader
import dev.atsushieno.kotractive.XmlWriter

class XmlSerializer<T> {
    fun deserialize(reader: XmlReader) : Any = TODO("Not implemented")
    fun serialize(stringBuilder: StringBuilder, obj: Any) =
        serialize(XmlWriter.create(stringBuilder), obj)
    fun serialize(writer: XmlWriter, obj: Any) {
        TODO("Not implemented")
    }
}