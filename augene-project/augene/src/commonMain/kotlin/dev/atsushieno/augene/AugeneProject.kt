package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okio.ExperimentalFileSystem
import okio.FileSystem
import okio.Path.Companion.toPath

annotation class XmlAttribute()
annotation class XmlIgnore()
annotation class XmlArrayItem(val itemName: String)

@Serializable
class AugeneProject {
	companion object {
		fun load(filename: String): AugeneProject =
			load(FileSupport(filename).readString(filename))

		fun loadString(text: String): AugeneProject =
			Json.decodeFromString(text)

		@OptIn(ExperimentalFileSystem::class)
		fun save(project: AugeneProject, filename: String) {
			// sanitize absolute paths
			for (track in project.tracks)
				if (track.audioGraph!!.toPath().isAbsolute)
					track.audioGraph = (filename.toPath() / track.audioGraph!!.toPath()).toString()

			val json = Json.encodeToString(project)
			FileSupport(filename).writeString(filename, json)
		}

		init {

		}
	}

	var includes: MutableList<AugeneInclude> = mutableListOf()

	var audioGraphs: MutableList<AugeneAudioGraph> = mutableListOf()

	var masterPlugins: MutableList<String> = mutableListOf()

	var tracks: MutableList<AugeneTrack> = mutableListOf()

	var mmlFiles: MutableList<String> = mutableListOf()

	var mmlStrings: MutableList<String> = mutableListOf()

	private fun checkIncludeValidity(
		includedAncestors: MutableList<String>,
		resolveAbsPath: (String) -> String,
		errors: MutableList<String>
	) {
		for (inc in this.includes) {
			if (inc.source == null)
				continue
			val absPath = resolveAbsPath(inc.source!!)
			if (includedAncestors.any { it.equals(absPath, true) })
				errors.add("Recursive inclusion was found: $absPath")
			val child = load(absPath)
			child.checkIncludeValidity(includedAncestors, resolveAbsPath, errors)
		}
	}

	@OptIn(ExperimentalFileSystem::class)
	fun expandedAudioGraphsFullPath(
		resolveAbsPath: (String) -> String,
		bankMsb: String?,
		bankLsb: String?
	): Sequence<AugeneAudioGraph> {
		val project = this
		return sequence {
			project.checkIncludeValidity(mutableListOf(), resolveAbsPath, mutableListOf())
			var count = 0
			for (item in project.audioGraphs)
				yield(AugeneAudioGraph().apply {
					this.bankMsb = bankMsb
					this.bankLsb = bankLsb
					program = count++.toString()
					id = item.id
					source = resolveAbsPath(item.source!!)
				})
			for (include in project.includes) {
				val src: String = include.source ?: continue
				val absPath = resolveAbsPath(src)
				val resolveNestedAbsPath = { s: String -> (FileSupport.canonicalizePath(absPath).toPath().parent!! / s).toString() }
				val msb = include.bankMsb ?: include.bank
				val lsb = include.bankLsb
				for (nested in load(resolveAbsPath(src)).expandedAudioGraphsFullPath(resolveNestedAbsPath, msb, lsb))
					yield(nested)
			}
		}
	}
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

class JuceAudioGraph {
	companion object {
		const val emptyAudioGraph = """<FILTERGRAPH>
<FILTER uid='5' x='0.5' y='0.1'>
<PLUGIN name='Audio Input' descriptiveName='' format='Internal' category='I/O devices' manufacturer='JUCE' version='1.0' file='' uid='246006c0' isInstrument='0' fileTime='0' infoUpdateTime='0' numInputs='0' numOutputs='4' isShell='0'/>
<STATE>0.</STATE>
<LAYOUT>
  <INPUTS><BUS index='0' layout='disabled'/></INPUTS>
  <OUTPUTS><BUS index='0' layout='disabled'/></OUTPUTS>
</LAYOUT>
</FILTER>
<FILTER uid='6' x='0.25' y='0.1'>
<PLUGIN name='Midi Input' descriptiveName='' format='Internal' category='I/O devices' manufacturer='JUCE' version='1.0' file='' uid='cb5fde0b' isInstrument='0' fileTime='0' infoUpdateTime='0' numInputs='0' numOutputs='0' isShell='0'/>
<STATE>0.</STATE>
<LAYOUT>
  <INPUTS><BUS index='0' layout='disabled'/></INPUTS>
  <OUTPUTS><BUS index='0' layout='disabled'/></OUTPUTS>
</LAYOUT>
</FILTER>
<FILTER uid='7' x='0.5' y='0.9'>
<PLUGIN name='Audio Output' descriptiveName='' format='Internal' category='I/O devices' manufacturer='JUCE' version='1.0' file='' uid='724248cb' isInstrument='0' fileTime='0' infoUpdateTime='0' numInputs='0' numOutputs='0' isShell='0'/>
<STATE>0.</STATE>
<LAYOUT>
  <INPUTS><BUS index='0' layout='L R Ls Rs'/></INPUTS>
  <OUTPUTS><BUS index='0' layout='disabled'/></OUTPUTS>
</LAYOUT>
</FILTER>
</FILTERGRAPH>
"""

		fun load (reader:XmlReader) :  Sequence<JuceAudioGraph> =
			sequence {
				var ret = JuceAudioGraph ()
				val doc = XDocument.load (reader)
				val input = doc.root!!.elements ("FILTER").firstOrNull { e ->
					e.elements ("PLUGIN").any { p -> p.attribute ("name")?.value.equals("Midi Input", true) ?: false && // it is MIDI Input since Waveform11 (maybe)
													p.attribute ("format")?.value == "Internal" }
				}
				val output = doc.root!!.elements ("FILTER").firstOrNull { e ->
					e.elements ("PLUGIN").any { p -> p.attribute ("name")?.value == "Audio Output" &&
													p.attribute ("format")?.value == "Internal" }
				}
				if (input == null || output == null)
					return@sequence
				var conn: XElement? = null
				var uid = input.attribute ("uid")?.value
				while (true) {
					conn = doc.root!!.elements ("CONNECTION").firstOrNull { e ->
						e.attribute ("srcFilter")?.value == uid }
					if (conn == null || conn == output)
						break
					if (uid != input.attribute ("uid")?.value) {
						val filter = doc.root!!.elements ("FILTER")
							.firstOrNull { e -> e.attribute ("uid")?.value == uid } ?: return@sequence
						val plugin = filter.element ("PLUGIN")
						if (plugin == null)
							return@sequence
						val state = filter.element ("STATE")
						val prog = plugin.attribute ("programNum")
						yield (JuceAudioGraph().apply {
							file = plugin.attribute ("file")?.value
							category = plugin.attribute ("category")?.value
							manufacturer = plugin.attribute ("manufacturer")?.value
							name = plugin.attribute ("name")?.value
							this.uid = plugin.attribute ("uid")?.value
							programNum = if (prog != null) prog.value.toInt() else 0
							this.state = state?.value
						})
					}
					uid = conn.attribute ("dstFilter")?.value
				}
				yield(ret)
			}
		}

	//<PLUGIN name="Midi Input" descriptiveName="" format="Internal" category="I/O devices"
	//manufacturer="JUCE" version="1.0" file="" uid="cb5fde0b" isInstrument="0"
	//fileTime="0" infoUpdateTime="0" numInputs="0" numOutputs="0"
	//isShell="0"/>
	//<STATE>0.</STATE>
	//
	// For audio plugins there is something like `type="vst" ` too.
	var name : String? = null
	var descriptiveName : String? = null
	var format : String? = null
	var category : String? = null
	var manufacturer : String? = null
	var version : String? = null
	var file : String? = null
	var uid : String? = null
	var isInstrument = 0
	var fileTime = 0L
	var infoUpdateTime = 0L
	var numInputs = 0
	var numOutputs = 0
	var isShell = 0 // not Boolean?
	var state : String? = null
	var programNum = 0 // ?
}

class AugenePluginSpecifier
{
	companion object {
		fun fromAudioGraph (audioGraph: Iterable<JuceAudioGraph> ) : Iterable<AugenePluginSpecifier> =
			audioGraph.map { src -> AugenePluginSpecifier().apply {
				type = src.format
				uid = src.uid
				filename = src.file
				name = src.name
				manufacturer = src.manufacturer
				programNum = src.programNum
				state = src.state
			} }
	}

	// They are required by tracktionedit.
	var type : String? = null
	var uid : String? = null
	var filename : String? = null
	var name : String? = null
	var manufacturer : String? = null
	var programNum = 0
	var state : String? = null
}
