package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.name

annotation class XmlAttribute()
annotation class XmlIgnore()
annotation class XmlArrayItem(val itemName: String)

class AugeneProject {
	companion object {
		fun Load(filename: String): AugeneProject {
			val serializer = XmlSerializer<AugeneProject>()
			val xmlString = Files.readString(Path.of(filename))
			return serializer.Deserialize(XmlReader.create(xmlString)) as AugeneProject
		}

		fun Save(project: AugeneProject, filename: String) {
			// sanitize absolute paths
			for (track in project.Tracks)
				if (Path.of(track.AudioGraph!!).isAbsolute)
					track.AudioGraph = Path.of(filename).relativize(Path.of(track.AudioGraph!!)).name

			val serializer = XmlSerializer<AugeneProject>()
			val sb = StringBuilder()
			serializer.Serialize(sb, project)
			Files.writeString(Path.of(filename), sb.toString())
		}
	}

	@XmlArrayItem("Include")
	var Includes: MutableList<AugeneInclude>? = null

	@XmlArrayItem("AudioGraph")
	var AudioGraphs: MutableList<AugeneAudioGraph> = mutableListOf()

	@XmlArrayItem("AudioGraph")
	var MasterPlugins: MutableList<String> = mutableListOf()

	var Tracks: MutableList<AugeneTrack> = mutableListOf()

	@XmlArrayItem("MmlFile")
	var MmlFiles: MutableList<String> = mutableListOf()

	@XmlArrayItem("MmlString")
	var MmlStrings: MutableList<String> = mutableListOf()

	fun CheckIncludeValidity(
		includedAncestors: MutableList<String>,
		resolveAbsPath: (String) -> String,
		errors: MutableList<String>
	) {
		for (inc in this.Includes!!) {
			if (inc.Source == null)
				continue
			val absPath = resolveAbsPath(inc.Source!!)
			if (includedAncestors.any { it.equals(absPath, true) })
				errors.add("Recursive inclusion was found: $absPath")
			val child = Load(absPath)
			child.CheckIncludeValidity(includedAncestors, resolveAbsPath, errors)
		}
	}

	fun AudioGraphsExpandedFullPath(
		resolveAbsPath: (String) -> String,
		bankMsb: String?,
		bankLsb: String?
	): Sequence<AugeneAudioGraph> {
		val project = this
		return sequence {
			project.CheckIncludeValidity(mutableListOf(), resolveAbsPath, mutableListOf())
			var count = 0
			for (item in project.AudioGraphs)
				yield(AugeneAudioGraph().apply {
					BankMsb = bankMsb
					BankLsb = bankLsb
					Program = count++.toString()
					Id = item.Id
					Source = resolveAbsPath(item.Source!!)
				})
			for (include in project.Includes!!) {
				val src: String = include.Source ?: continue
				val absPath = resolveAbsPath(src)
				val resolveNestedAbsPath = { s: String -> Path.of(absPath).absolute().parent.resolve(s).name }
				val msb = include.BankMsb ?: include.Bank
				val lsb = include.BankLsb
				for (nested in Load(resolveAbsPath(src)).AudioGraphsExpandedFullPath(resolveNestedAbsPath, msb, lsb))
					yield(nested)
			}
		}
	}
}

class AugeneInclude
{
	@XmlAttribute
	var Bank : String? = null // equivalent to BankMsb
	@XmlAttribute
	var BankMsb : String? = null
	@XmlAttribute
	var BankLsb : String? = null
	@XmlAttribute
	var Source : String? = null
}

class AugeneAudioGraph
{
	@XmlIgnore
	internal var Program: String? = null
	@XmlIgnore
	internal var BankMsb: String? = null
	@XmlIgnore
	internal var BankLsb: String? = null

	@XmlAttribute
	var Id : String? = null
	@XmlAttribute
	var Source : String? = null
}

class AugeneTrack
{
	var Id : String? = null
	var AudioGraph : String? = null
}

class JuceAudioGraph {
	companion object {
		const val EmptyAudioGraph = """<FILTERGRAPH>
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

		fun Load ( reader:XmlReader) :  Sequence<JuceAudioGraph> =
			sequence {
				var ret = JuceAudioGraph ()
				val doc = XDocument.Load (reader)
				val input = doc.Root.Elements ("FILTER").firstOrNull { e ->
					e.Elements ("PLUGIN").any { p -> p.Attribute ("name")?.Value.equals("Midi Input", true) ?: false && // it is MIDI Input since Waveform11 (maybe)
													p.Attribute ("format")?.Value == "Internal" }
				}
				val output = doc.Root.Elements ("FILTER").firstOrNull { e ->
					e.Elements ("PLUGIN").any { p -> p.Attribute ("name")?.Value == "Audio Output" &&
													p.Attribute ("format")?.Value == "Internal" }
				}
				if (input == null || output == null)
					return@sequence
				var conn: XElement? = null
				var uid = input.Attribute ("uid")?.Value
				while (true) {
					conn = doc.Root.Elements ("CONNECTION").firstOrNull { e ->
						e.Attribute ("srcFilter")?.Value == uid }
					if (conn == null || conn == output)
						break
					if (uid != input.Attribute ("uid")?.Value) {
						val filter = doc.Root.Elements ("FILTER")
							.firstOrNull { e -> e.Attribute ("uid")?.Value == uid }
						if (filter == null)
							return@sequence
						val plugin = filter.Element ("PLUGIN")
						if (plugin == null)
							return@sequence
						val state = filter.Element ("STATE")
						val prog = plugin.Attribute ("programNum")
						yield (JuceAudioGraph().apply {
							File = plugin.Attribute ("file")?.Value
							Category = plugin.Attribute ("category")?.Value
							Manufacturer = plugin.Attribute ("manufacturer")?.Value
							Name = plugin.Attribute ("name")?.Value
							Uid = plugin.Attribute ("uid")?.Value
							ProgramNum = if (prog != null) prog.Value.toInt() else 0
							State = if (state != null) state.Value else null
						})
					}
					uid = conn.Attribute ("dstFilter")?.Value
				}
			}
		}

	//<PLUGIN name="Midi Input" descriptiveName="" format="Internal" category="I/O devices"
	//manufacturer="JUCE" version="1.0" file="" uid="cb5fde0b" isInstrument="0"
	//fileTime="0" infoUpdateTime="0" numInputs="0" numOutputs="0"
	//isShell="0"/>
	//<STATE>0.</STATE>
	//
	// For audio plugins there is something like `type="vst" ` too.
	var Name : String? = null
	var DescriptiveName : String? = null
	var Format : String? = null
	var Category : String? = null
	var Manufacturer : String? = null
	var Version : String? = null
	var File : String? = null
	var Uid : String? = null
	var IsInstrument = 0
	var FileTime = 0L
	var InfoUpdateTime = 0L
	var NumInputs = 0
	var NumOutputs = 0
	var IsShell = 0 // not Boolean?
	var State : String? = null
	var ProgramNum = 0 // ?
}

class AugenePluginSpecifier
{
	companion object {
		fun FromAudioGraph (audioGraph: Iterable<JuceAudioGraph> ) : Iterable<AugenePluginSpecifier> =
			audioGraph.map { src -> AugenePluginSpecifier().apply {
				Type = src.Format
				Uid = src.Uid
				Filename = src.File
				Name = src.Name
				Manufacturer = src.Manufacturer
				ProgramNum = src.ProgramNum
				State = src.State
			} }
	}

	// They are required by tracktionedit.
	var Type : String? = null
	var Uid : String? = null
	var Filename : String? = null
	var Name : String? = null
	var Manufacturer : String? = null
	var ProgramNum = 0
	var State : String? = null
}
