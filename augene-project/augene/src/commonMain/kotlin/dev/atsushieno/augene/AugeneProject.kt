package dev.atsushieno.augene

import dev.atsushieno.missingdot.xml.XDocument
import dev.atsushieno.missingdot.xml.XElement
import dev.atsushieno.missingdot.xml.XmlReader
import dev.atsushieno.missingdot.xml.XmlTextReader
import dev.atsushieno.missingdot.xml.XmlWriter
import dev.atsushieno.missingdot.xml.XmlTextWriter
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath

@Serializable
class AugeneProject {

	var includes: MutableList<AugeneInclude> = mutableListOf()
	var audioGraphs: MutableList<AugeneAudioGraph> = mutableListOf()
	var masterPlugins: MutableList<String> = mutableListOf()
	var tracks: MutableList<AugeneTrack> = mutableListOf()
	var mmlFiles: MutableList<String> = mutableListOf()
	var mmlStrings: MutableList<String> = mutableListOf()
}

@Serializable
class AugeneInclude
{
	var bank : String? = null // equivalent to BankMsb
	var bankMsb : String? = null
	var bankLsb : String? = null
	var source : String? = null
}

@Serializable
class AugeneAudioGraph
{
	// non-serialized
	internal var program: String? = null
	internal var bankMsb: String? = null
	internal var bankLsb: String? = null
	fun retrieveProgram() = program
	fun retrieveBankMsb() = bankMsb
	fun retrieveBankLsb() = bankLsb

	var id : String? = null
	var source : String? = null
}

@Serializable
class AugeneTrack
{
	var id : String? = null
	var audioGraph : String? = null
}

class AugenePluginSpecifier
{
	// They are required by tracktionedit.
	var type : String? = null
	var uid : String? = null
	var filename : String? = null
	var name : String? = null
	var manufacturer : String? = null
	var programNum = 0
	var state : String? = null
}

object AugeneProjectLoader {
	fun load(reader: XmlReader): AugeneProject {
		val doc = XDocument.load(reader)
		val ret = AugeneProject()
		val root = doc.root!!
		val includes = root.element("Includes")
		if (includes != null)
			ret.includes.addAll(includes.elements("Include").map {
				AugeneInclude().apply {
					source = it.attribute("Source")?.value
					bank = it.attribute("Bank")?.value
				}
			})
		val masterPlugins = root.element("MasterPlugins")
		if (masterPlugins != null)
			ret.masterPlugins.addAll(masterPlugins.elements("MasterPlugin").map { it.value })
		val audioGraphs = root.element("AudioGraphs")
		if (audioGraphs != null)
			ret.audioGraphs.addAll(audioGraphs.elements("AudioGraph").map {
				AugeneAudioGraph().apply {
					id = it.attribute("Id")?.value
					source = it.attribute("Source")?.value
				}
			})
		val tracks = root.element("Tracks")
		if (tracks != null)
			ret.tracks.addAll(tracks.elements("AugeneTrack").map {
				AugeneTrack().apply {
					id = it.element("Id")?.value
					audioGraph = it.element("AudioGraph")?.value
				}
			})
		val mmlFiles = root.element("MmlFiles")
		if (mmlFiles != null)
			ret.mmlFiles.addAll(mmlFiles.elements("MmlFile").map { it.value })
		val mmlStrings = root.element("MmlStrings")
		if (mmlStrings != null)
			ret.mmlStrings.addAll(mmlStrings.elements("MmlString").map { it.value })

		return ret
	}

	fun load(fileName: String): AugeneProject =
		load(XmlTextReader(FileSupport(fileName).readString(fileName)))

	fun loadJson(jsonText: String): AugeneProject = Json.decodeFromString(jsonText)
}

object AugeneProjectSaver {
	fun save(project: AugeneProject, writer:
	XmlWriter) {
		writer.writeStartElement("AugeneProject")
		writer.writeStartElement("Includes")
		project.includes.forEach {
			writer.writeStartElement("Include")
			writer.writeAttributeString("Bank", it.bank ?: "")
			writer.writeAttributeString("Source", it.source ?: "")
			writer.writeEndElement()
		}
		writer.writeEndElement()

		writer.writeStartElement("MasterPlugins")
		project.masterPlugins.forEach {
			writer.writeElementString("MasterPlugin", it)
		}
		writer.writeEndElement()

		writer.writeStartElement("AudioGraphs")
		project.audioGraphs.forEach {
			writer.writeStartElement("AudioGraph")
			writer.writeAttributeString("Id", it.id ?: "")
			writer.writeAttributeString("Source", it.source ?: "")
			writer.writeEndElement()
		}
		writer.writeEndElement()

		writer.writeStartElement("Tracks")
		project.tracks.forEach {
			writer.writeStartElement("AugeneTrack")
			writer.writeElementString("Id", it.id ?: "")
			writer.writeElementString("AudioGraph", it.audioGraph ?: "")
			writer.writeEndElement()
		}
		writer.writeEndElement()

		writer.writeStartElement("MmlFiles")
		project.mmlFiles.forEach {
			writer.writeElementString("MmlFile", it)
		}
		writer.writeEndElement()

		writer.writeStartElement("MmlStrings")
		project.mmlStrings.forEach {
			writer.writeElementString("MmlString", it)
		}
		writer.writeEndElement()
		writer.close()
	}

	private fun canBeRelative(basePath: String, candidatePath: String) =
		candidatePath.startsWith(basePath.toPath().parent.toString())
	private fun relativize(basePath: String, candidatePath: String) =
		candidatePath.substring(basePath.toPath().parent.toString().length + 1)

	@OptIn(ExperimentalFileSystem::class)
	fun save(project: AugeneProject, filename: String) {
		// sanitize absolute paths

		for (ag in project.audioGraphs)
			if (ag.source!!.toPath().isAbsolute)
			// FIXME: we want to have `Path.relativize()` but there is no such implementation in MPP world.
				if (canBeRelative(filename, ag.source!!))
					ag.source = relativize(filename, ag.source!!)
		project.mmlFiles.forEachIndexed { index, s ->
			if (s.toPath().isAbsolute)
			// FIXME: we want to have `Path.relativize()` but there is no such implementation in MPP world.
				if (canBeRelative(filename, s))
					project.mmlFiles[index] = relativize(filename, s)
		}
		project.masterPlugins.forEachIndexed { index, s ->
			if (s.toPath().isAbsolute)
			// FIXME: we want to have `Path.relativize()` but there is no such implementation in MPP world.
				if (canBeRelative(filename, s))
					project.masterPlugins[index] = relativize(filename, s)
		}

		val sb = StringBuilder()
		save(project, XmlTextWriter(sb).apply { indent = true })
		FileSupport(filename).writeString(filename, sb.toString()) // use of filename as FileSupport looks awkward, but anyways...
	}

	fun saveJson(project: AugeneProject) = Json.encodeToString(project)
}
