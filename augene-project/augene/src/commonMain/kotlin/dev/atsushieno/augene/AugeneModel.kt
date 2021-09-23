package dev.atsushieno.augene

import dev.atsushieno.kotractive.*
import dev.atsushieno.ktmidi.MidiCC
import dev.atsushieno.missingdot.xml.XmlReader
import dev.atsushieno.missingdot.xml.XmlTextReader
import dev.atsushieno.mugene.MmlCompiler
import dev.atsushieno.mugene.MmlInputSource
import okio.ExperimentalFileSystem
import okio.Path.Companion.toPath
import kotlin.random.Random

open class AugeneModel
{
	companion object {
		private const val TracktionProgramChange = 4097

		fun toTracktion (src: Iterable<AugenePluginSpecifier>):  Iterable<PluginElement> {
			return src.map { a ->
				PluginElement().apply {
					Filename = a.filename
					Enabled = true
					Uid = a.uid
					Type = a.type ?: "vst"
					Name = a.name
					Manufacturer = a.manufacturer
					State = a.state
					Volume = 1.0 // maybe? at least we have to avoid default 0.0
				}
			}
		}
	}

	var project = AugeneProject ()
	var projectFileName: String? = null

	var outputEditFileName: String? = null
	var dryRun = false

	var lastProjectFile: String? = null

	@OptIn(ExperimentalFileSystem::class)
	val projectDirectory : String?
		get() = if (projectFileName == null) null else projectFileName!!.toPath().parent.toString()

	open fun reportError (errorId: String, msg: String) {
		// FIXME: appropriate error reporting
		println ("$errorId: $msg")
	}

	@OptIn(ExperimentalFileSystem::class)
	fun getItemFileRelativePath (itemFilename: String) : String {
		var filenameRelative = itemFilename
		if (projectFileName != null)
			filenameRelative = (projectFileName!!.toPath() / itemFilename.toPath()).toString ()
		return filenameRelative
	}

	@OptIn(ExperimentalFileSystem::class)
	fun getItemFileAbsolutePath (itemFilename: String) =
		(projectFileName!!.toPath().parent!! / itemFilename.toPath()).toString()

	fun loadProjectFile (xmlFile: String) =
		setProject(AugeneProject.load(xmlFile), xmlFile)

	fun loadProjectJson (text: String, baseFileName: String = ".") =
		setProject(AugeneProject.loadJson (text), baseFileName)

	fun setProject(newProject: AugeneProject, baseFileName: String) {
		project = newProject
		projectFileName = baseFileName
		lastProjectFile = projectFileName
		onProjectLoaded()
	}
	var onProjectLoaded : () -> Unit = {}

	fun addNewTrack (filename: String) {
		var newTrackId = 1 + project.tracks.size
		while (project.tracks.any { t -> t.id == newTrackId.toString () })
			newTrackId++
		project.tracks.add (AugeneTrack().apply {
			id = newTrackId.toString ()
			audioGraph = getItemFileRelativePath (filename)
		})

		onTrackAdded()
	}
	var onTrackAdded : () -> Unit = {}

	fun deleteTracks (trackIdsToRemove: Iterable<String>) {
		val tracksRemaining = project.tracks.filter { t -> !trackIdsToRemove.contains (t.id) }
		project.tracks.clear ()
		project.tracks.addAll (tracksRemaining)

		onTracksDeleted()
	}
	var onTracksDeleted : () -> Unit = {}

	fun addNewAudioGraph (filename: String) {
		var newGraphId = 1 + project.audioGraphs.size
		while (project.tracks.any { t -> t.id == newGraphId.toString () })
			newGraphId++
		project.audioGraphs.add (AugeneAudioGraph().apply {
			id = newGraphId.toString ()
			source = getItemFileRelativePath (filename)
		})

		onAudioGraphAdded()
	}
	var onAudioGraphAdded : () -> Unit = {}

	fun deleteAudioGraphs (audioGraphIdsToRemove: Iterable<String>) {
		val graphsRemaining = project.audioGraphs.filter { t -> !audioGraphIdsToRemove.contains (t.id) }
		project.audioGraphs.clear ()
		project.audioGraphs.addAll (graphsRemaining)

		onAudioGraphsDeleted()
	}
	var onAudioGraphsDeleted : () -> Unit = {}

	fun addNewMmlFile (filename: String) {
		project.mmlFiles.add (getItemFileRelativePath (filename))

		onMmlFileAdded()
	}
	var onMmlFileAdded : () -> Unit = {}

	fun unregisterMmlFiles (filesToUnregister: Iterable<String>) {
		val filesRemaining = project.mmlFiles.filter { f -> !filesToUnregister.contains (f) }
		project.mmlFiles.clear ()
		project.mmlFiles.addAll (filesRemaining)

		onMmlFilesDeleted()
	}
	var onMmlFilesDeleted : () -> Unit = {}

	fun addNewMasterPluginFile (filename: String) {
		project.masterPlugins.add (getItemFileRelativePath (filename))

		onMasterPluginAdded()
	}
	var onMasterPluginAdded : () -> Unit = {}

	fun unregisterMasterPluginFiles (filesToUnregister: Iterable<String>) {
		val filesRemaining = project.masterPlugins.filter { f -> !filesToUnregister.contains (f) }
		project.masterPlugins.clear ()
		project.masterPlugins.addAll (filesRemaining)

		onMasterPluginsDeleted()
	}
	var onMasterPluginsDeleted : () -> Unit = {}

	@OptIn(ExperimentalFileSystem::class)
	fun compile () {
		if (projectFileName == null)
			throw IllegalStateException ("To compile the project, ProjectFileName must be specified in prior")

		val fileSupport = FileSupport(projectFileName!!)
		val abspath = { s:String? -> fileSupport.resolvePathRelativeToProject(s!!) }

		// compile into MidiMusic.
		val compiler = MmlCompiler.create()
		val mmlFilesAbs = project.mmlFiles.map { f -> abspath (f) }
		val mmls = mmlFilesAbs.map { f -> MmlInputSource (f, fileSupport.readString(f)) } +
			project.mmlStrings.map { s -> MmlInputSource ("(no file)", s) }
		val music = compiler.compile (false, mmls.toTypedArray())

		// load filtergraphs here so that they can be referenced at importing.
		val audioGraphs = project.expandedAudioGraphsFullPath (abspath, null, null).asIterable ().toMutableList()
		val juceAudioGraphs = audioGraphs.filter { it.source != null && it.id != null }.map {
			val text = fileSupport.readString(abspath(it.source))
			it.id!! to JuceAudioGraph.load(XmlReader.create(text)).toList()
		}.toMap()

		// prepare tracktionedit
		val edit = EditElement ()
		val converter = MidiToTracktionEditConverter (MidiImportContext (music, edit, audioGraphs, juceAudioGraphs))
		converter.importMusic ()
		val dstTracks = edit.Tracks.filterIsInstance<TrackElement>()

		// Assign numeric IDs to those unnamed tracks.
		for (n in dstTracks.indices)
			if (edit.Tracks [n].Id == null)
				edit.Tracks [n].Id = (n + 1).toString ()

		// Step 1: assign audio graphs by Bank Select and Program Change, if any.
		// Such a track must not contain more than one program change, bank select MSB and bank select LSB.
		for (track in edit.Tracks.filterIsInstance<TrackElement> ()) {
			val sequenceContainer = track.Clips.filterIsInstance<MidiClipElement> ()
				.filter { c -> c.Sequence != null }.toList()
			val programs = sequenceContainer.flatMap { c -> c.Sequence!!.Events.filterIsInstance<ControlElement> ()
					.filter { e -> e.Type == TracktionProgramChange } }
			val banks = sequenceContainer.flatMap { c -> c.Sequence!!.Events.filterIsInstance<ControlElement> ()
					.filter { e -> e.Type == MidiCC.BANK_SELECT } }
			val bankLSBs = sequenceContainer.flatMap { c -> c.Sequence!!.Events.filterIsInstance<ControlElement> ()
					.filter { e -> e.Type == MidiCC.BANK_SELECT_LSB } }
			if (programs.size != 1 || banks.size > 1 || bankLSBs.size > 1)
				continue // ignore
			val msb = ((banks.firstOrNull ()?.Val ?: 0) / 128).toString ()
			val lsb = ((bankLSBs.firstOrNull ()?.Val ?: 0) / 128).toString ()
			val program = (programs.first ().Val / 128).toString ()
			val ag = audioGraphs.firstOrNull { a ->
				a.retrieveProgram() == program &&
				(a.retrieveBankMsb() == msb || a.retrieveBankMsb() == null && msb == "0") &&
				(a.retrieveBankLsb() == lsb || a.retrieveBankLsb() == null && lsb == "0") }
			if (ag != null) {
				val existingPlugins = track.Plugins.toTypedArray()
				track.Plugins.clear ()
				val text = fileSupport.readString(abspath (ag.source))
				val graph = JuceAudioGraph.load(XmlReader.create(text)).asIterable()
				for (p in toTracktion(AugenePluginSpecifier.fromAudioGraph(graph)))
					track.Plugins.add(p)
				// recover volume and level at the end.
				for (p in existingPlugins)
					track.Plugins.add (p)
			}
		}

		// there used to be Step 2: assign audio graphs by INSTRUMENTNAME (if named). It will overwrite bank mapping.
		// (Now it is done at Midi2TracktionEditConverter.importMusic() as PLUGIN elements should be already populated there.)

		// Step 3: assign audio graphs by TRACKNAME (if named). It will overwrite all above.
		for (track in project.tracks) {
			val dstTrack = dstTracks.firstOrNull { t -> t.Id == track.id } ?: continue
			val existingPlugins = dstTrack.Plugins.toTypedArray()
			dstTrack.Plugins.clear ()
			if (track.audioGraph != null) {
				// track's AudioGraph may be either a ID reference or a filename.
				val ag = audioGraphs.firstOrNull { a -> a.id == track.audioGraph }
				val agFile = ag?.source ?: track.audioGraph
				/* FIXME: maybe enable this proactive file check?
				if (!File (abspath (agFile)).exists()) {
					reportError ("AugeneAudioGraphNotFound", "AudioGraph does not exist: " + abspath (agFile))
					continue
				}*/
				val text = fileSupport.readString(abspath (agFile))
				val graph = JuceAudioGraph.load (XmlReader.create (text)).asIterable()
				for (p in toTracktion (AugenePluginSpecifier.fromAudioGraph (graph)))
				dstTrack.Plugins.add (p)
			}
			// recover volume and level at the end.
			for (p in existingPlugins)
				dstTrack.Plugins.add (p)
		}

		for (masterPlugin in project.masterPlugins) {
			// AudioGraph may be either an ID reference or a filename.
			val ag = audioGraphs.firstOrNull { a -> a.id == masterPlugin }
			val agFile = ag?.source ?: masterPlugin
			/* FIXME: maybe enable this proactive file check?
			if (!File (abspath (agFile)).exists()) {
				reportError ("AugeneAudioGraphNotFound", "AudioGraph does not exist: " + abspath (agFile))
				continue
			}
			*/
			val text = fileSupport.readString(abspath (agFile))
			val graph = JuceAudioGraph.load (XmlReader.create (text)).asIterable()
			for ( p in toTracktion (AugenePluginSpecifier.fromAudioGraph(graph)))
				edit.MasterPlugins.add (p)
		}

		fun changeExtension(path: String, ext: String) : String {
			val lastIndex = path.lastIndexOf('.')
			return if (lastIndex > 0) path.substring(0, lastIndex) + ext else path
		}

		// Project ID, 00nnnnnnh. It is identical with the one in .tracktionedit EDIT element's attribute.
		val projectId = Random.nextInt() and 0xFFFFFF
		edit.ProjectID = edit.ProjectID ?: "${projectId}/${Random.nextInt()}" // not sure if it is correct format

		val outfile = outputEditFileName ?: changeExtension(abspath (projectFileName!!), ".tracktionedit")
		val sb = StringBuilder()
		EditModelWriter().write(sb, edit)

		if (dryRun)
			return

		fileSupport.writeString(outfile, sb.toString())
		outputEditFileName = outfile

		createTracktionProjectBinary(outfile, projectId, getFileNameWithoutExtension(projectFileName!!))
	}

	private fun getFileNameWithoutExtension(fileName: String) : String {
		val name = fileName.toPath().name
		val lastIndex = name.lastIndexOf('.')
		return if (lastIndex < 0) name else name.substring(0, lastIndex)
	}

	private fun createTracktionProjectBinary(editFile: String, projectId: Int, projectName: String) {
		val tracktionFile = editFile.substring(0, editFile.lastIndexOf('.')) + ".tracktion"
		if (FileSupport(tracktionFile).exists(tracktionFile))
			return

		val bytes = mutableListOf<Byte>()
		bytes.addAll("TP01".encodeToByteArray().toTypedArray())
		bytes.addAll(projectId.toBytes())
		bytes.addAll(0.toBytes()) // object indexes begins fill later
		bytes.addAll(0.toBytes()) // end of contents? fill later
		// number of properties.
		// Tracktion Waveform creates "name" and "description", but we don't need latter (it's just the created date there either).
		bytes.addAll(1.toBytes())
		// for each property, repeat: null-terminated property name, numBytes including null terminator, utf8 string data
		bytes.addAll("name".encodeToByteArray().toTypedArray())
		bytes.add(0) // null-terminator
		bytes.addAll((projectName.length + 1).toBytes())
		bytes.addAll(projectName.encodeToByteArray().toTypedArray())
		bytes.add(0) // null-terminator

		val objectsOffset = bytes.size

		// Then Tracktion writes object items here. Contents first.
		// Once all contents are put, then Tracktion supplies ItemIDs and FileOffsets afterwards.

		// There is only one object in this template: edit file
		// - the file name without extension, null-terminated
		// - a string "edit", null-terminated
		// - a string "Created as the default edit for this project)|MediaObjectCategory|1", null-terminated
		// - the file name *with* extension, null-terminated

		bytes.addAll(getFileNameWithoutExtension(editFile).encodeToByteArray().toTypedArray())
		bytes.add(0) // null-terminator
		bytes.addAll("edit".encodeToByteArray().toTypedArray())
		bytes.add(0) // null-terminator
		bytes.addAll("Created as the default edit for this project)|MediaObjectCategory|1".encodeToByteArray().toTypedArray())
		bytes.add(0) // null-terminator
		bytes.addAll(editFile.toPath().name.encodeToByteArray().toTypedArray())
		bytes.add(0) // null-terminator

		// - 0000
		// - 0 0 28h 40h : still unknown

		val objectsFooterOffset = bytes.size

		bytes.addAll(1.toBytes()) // number of objects. The second item is the index list...?
		bytes.addAll(0x03166153.toBytes()) // itemID
		bytes.addAll(objectsOffset.toBytes()) // index at stream. In this template, objectsOffset is the location.

		val searchIndexesOffset = bytes.size

		// Then it writes "search indices". Number of indices first, then contents:
		// - word, null-terminated
		// - numIDs, short
		// - ids (don't know what those IDs actually point to)
		// Probably setting 0 means no index and safe to ignore everything else?
		bytes.addAll(0.toBytes())

		//	alter the sizes
		val result = bytes.take(8) + objectsFooterOffset.toBytes().toList() + searchIndexesOffset.toBytes().toList() + bytes.drop(16)
		FileSupport(editFile).writeBytes(tracktionFile, result.toByteArray())
	}
}

private fun Int.toBytes() : Array<Byte> {
	val ret = MutableList(4) { 0.toUByte() }
	var v = this.toUInt()
	for (i in 0 .. 3) {
		ret[i] = (v % 0x100u).toUByte()
		v /= 0x100u
	}
	return ret.map { u -> u.toByte() }.toTypedArray()
}
