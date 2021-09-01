package dev.atsushieno.kotractive

/*
* Naming rules:
* 
* - Elements end with "Element".
*   - It is to differentiate type name and member property name to avoid possible conflict e.g. <PITCH pitch="..." />
* - Simple type properties are serialized as attributes.
* - All those elements are UPPERCASED, and attributes are camelCased,
*   with an exception that '_' indicates namespace prefix (... sort of) splitter.
* 
*/   

enum class DataType
{
	Unknown,
	String,
	UnixTime,
	Id,
	Length,
	Number,
	Integer,
	BooleanInt,
	Color,
	HexBinary,
	Base64Binary
}

class ControlType
{
	companion object {
		const val ProgramChange = 0x1000 + 1
		const val PAf = 0x1000 + 4
		const val PitchBend = 0x1000 + 5
		const val CAf = 0x1000 + 7
	}
}

@Target(AnnotationTarget.FIELD)
annotation class DataTypes(val dataType: DataType)


class EditElement
{
	// attributes
	var ProjectID : String? = null
	var AppVersion : String? = null
	@DataTypes (DataType.UnixTime)
	var CreationTime : Long = 0
	var ModifiedBy : String? = null
	@DataTypes(DataType.Id)
	var MediaId : String? = null
	var LastSignificantChange : String? = null

	// elements
	var Transport : TransportElement? = null
	var MacroParameters : MacroParametersElement? = null
	var TempoSequence : TempoSequenceElement? = null
	var PitchSequence : PitchSequenceElement? = null
	var Video : VideoElement? = null
	var ViewState : ViewStateElement? = null
	var AutoMapXml : AutoMapXmlElement? = null
	var ClickTrack : ClickTrackElement? = null
	var Id3VorbisMetadata : Id3VorbisMetadataElement? = null
	var MasterVolume : MasterVolumeElement? = null
	// new
	var Racks : RacksElement? = null
	// old
	var RackFilters : RackFiltersElement? = null
	// new
	var MasterPlugins : MutableList<PluginElement> = mutableListOf<PluginElement>()
	// old
	var MasterFilters : MutableList<FilterElement> = mutableListOf<FilterElement>()
	var AuxBusNames : AuxBusNamesElement? = null
	// new
	var InputDevices : InputDevicesElement? = null
	// old
	var DevicesEx : DevicesExElement? = null
	var TrackComps : TrackCompsElement? = null
	var AraDocument : AraDocumentElement? = null
	var ControllerMappings : ControllerMappingsElement? = null
	var EditMixGroups : EditMixGroupsElement? = null
	var AudioEditing : AudioEditingElement? = null
	var MidiViewState : MidiViewStateElement? = null
	var ArrangeView : ArrangeViewElement? = null

	var Tracks : MutableList<AbstractTrackElement> = mutableListOf<AbstractTrackElement>()
}


class TransportElement
{
	var Position : Double? = null
	var ScrubInterval : Double? = null
	var LoopPoint1 : Double? = null
	var LoopPoint2 : Double? = null
}


class MacroParametersElement
{
	var Id : String? = null // new
	@DataTypes(DataType.Id)
	var MediaId : String? = null // old
}


class TempoSequenceElement
{
	var Tempos : MutableList<TempoElement> = mutableListOf<TempoElement>()
	var TimeSignatures : MutableList<TimeSigElement> = mutableListOf<TimeSigElement>()
}


class TempoElement
{
	@DataTypes(DataType.Length)
	var StartBeat : Double = 0.0
	var Bpm : Double = 0.0
	var Curve : Double = 0.0
}


class TimeSigElement
{
	var Numerator : Int = 0
	var Denominator : Int = 0
	@DataTypes(DataType.Length)
	var StartBeat : Double = 0.0
}


class PitchSequenceElement
{
	var Pitches : MutableList<PitchElement> = mutableListOf<PitchElement>()
}


class PitchElement
{
	// new
	@DataTypes(DataType.Length)
	var StartBeat : Double = 0.0
	// old
	@DataTypes(DataType.Length)
	var Start : Double = 0.0
	var Pitch : Double = 0.0
}


class VideoElement
{
}


class ViewStateElement
{
	// attributes
	var MinimalTransportBar : Boolean = false
	var ScrollWhenPlaying : Boolean = false
	var HiddenClips : String? = null
	var LockedClips : String? = null
	var EnabledTrackTags : String? = null
	var DisabledSearchLibraries : String? = null
	var CurrentSidePanel : String? = null
	@DataTypes(DataType.Length)
	var MarkIn : Double = 0.0
	@DataTypes(DataType.Length)
	var MarkOut : Double = 0.0
	var Tracktop : Double = 0.0
	@DataTypes(DataType.Length)
	var Cursorpos : Double = 0.0
	@DataTypes(DataType.Length)
	var Viewleft : Double = 0.0
	@DataTypes(DataType.Length)
	var Viewright : Double = 0.0
	var MidiEditorShown : Boolean = false
	var EndToEnd : Boolean = false
	var SidePanelsShown : Boolean = false
	var MixerPanelShown : Boolean = false
	var MidiEditorHeight : Double = 0.0
	// elements
	var FacePlateView : FacePlateViewElement? = null
	var TrackEditors : TrackEditorsElement? = null
}


class TrackEditorsElement
{
}


class FacePlateViewElement
{
	var EditModeActive : Boolean? = null
}


class AutoMapXmlElement
{
}


class ClickTrackElement
{
	var Level : Double = 0.0
}


class Id3VorbisMetadataElement
{
	var TrackNumber : Double = 0.0
	@DataTypes(DataType.UnixTime)
	var Date : String? = null
}


class MasterVolumeElement
{
	// new
	var Plugins : MutableList<PluginElement> = mutableListOf<PluginElement>()
	// old
	var Filters : MutableList<FilterElement> = mutableListOf<FilterElement>()
}


open class RackElementBase
{
}


class RackFiltersElement : RackElementBase()
{
}


class RacksElement : RackElementBase()
{
}

open class MasterPluginsElementBase
{
}


class MasterFiltersElement : MasterPluginsElementBase()
{
}


class MasterPluginsElement : MasterPluginsElementBase()
{
}


class AuxBusNamesElement
{
}


open class InputDevicesElementBase
{
	var InputDevices : MutableList<InputDeviceElement> = mutableListOf<InputDeviceElement>()
}

// new

class InputDevicesElement : InputDevicesElementBase()
{		
}

// old
// I hope it is not the other casing...

class DevicesExElement : InputDevicesElementBase()
{
}


class InputDeviceElement
{
	var Name : String? = null
	@DataTypes(DataType.Id)
	var TargetTrack : String? = null
	var TargetIndex : Int = 0
}


class TrackCompsElement
{
}


class AraDocumentElement
{
}


class ControllerMappingsElement
{
}


abstract class AbstractViewElement
{
	var Width : Double? = null
	var Height : Double? = null
	var VerticalOffset : Double? = null
	var VisibleProportion : Double? = null
}


abstract class AbstractTrackElement : AbstractViewElement()
{
	var Name : String? = null
	// new
	var Id : String? = null
	// old
	@DataTypes(DataType.Id)
	var MediaId : String? = null

	var MacroParameters : MacroParametersElement? = null
	var Modifiers : ModifiersElement? = null
}

abstract class AbstractContentTrackElement : AbstractTrackElement()
{
	var AutomationTracks : MutableList<AutomationTrackElement> = mutableListOf<AutomationTrackElement>()
	// new
	var Plugins : MutableList<PluginElement> = mutableListOf<PluginElement>()
	// old
	var Filters : MutableList<FilterElement> = mutableListOf<FilterElement>()
}

class AutomationTrackElement : AbstractTrackElement()
{
	var Colour : String? = null
	var CurrentAutoParamPluginID : Int? = null
	var CurrentAutoParamTag : Int? = null
}

// used by both folder track and submix track.
class FolderTrackElement : AbstractContentTrackElement()
{
	var Expanded : Boolean? = null
	
	var Tracks : MutableList<AbstractTrackElement> = mutableListOf<AbstractTrackElement>()
}


class TempoTrackElement : AbstractTrackElement()
{
}


class ModifiersElement
{
	var Modifiers : MutableList<AbstractModifierElement> = mutableListOf<AbstractModifierElement>()
}

// These elements are used both as definitions and as uses...
abstract class AbstractModifierElement
{
	// definitions
	var Id : String? = null
	var RemapOnTempoChange : Boolean = false
	var Colour : String? = null
	var Base64_Parameters : String? = null

	// uses
	var Source : Int? = null
	var ParamID : String? = null
	var Value : Double? = null
	
	// definitions
	var ModifierAssignments : ModifierAssignmentsElement? = null
}

class LFOElement : AbstractModifierElement()
{
	var Rate : Double? = null
	var RateType : Double? = null
	var SyncType : Double? = null
	var Wave : Double? = null
}

class StepElement : AbstractModifierElement()
{
	// definitions
	var SyncType : Double? = null
	var NumSteps : Double? = null
}

class EnvelopeFollowerElement : AbstractModifierElement()
{
	// definitions
	var Enabled : Double? = null // looks like every parameter is number-based
	var GainDb : Double? = null
	var Attack : Double? = null
	var Hold : Double? = null
	var Release : Double? = null
}

class RandomElement : AbstractModifierElement()
{
	var Type : Double? = null
	var Shape : Double? = null
	var SyncType : Double? = null
	var Rate : Double? = null
}

class MidiTrackerElement : AbstractModifierElement()
{
	var Nodes : NodesElement? = null
}

class NodesElement
{
	var Nodes : MutableList<NodeElement> = mutableListOf<NodeElement>()
}

class NodeElement
{
	var Midi : Int = 0
	var Value : Double = 0.0
}


class MarkerTrackElement : AbstractTrackElement()
{
	var TrackType : Int = 0
}


open class PluginElementBase
{
	// attributes
	var Type : String? = null
	@DataTypes(DataType.Id)
	var Uid : String? = null
	var Filename : String? = null
	var Name : String? = null
	var Manufacturer : String? = null
	@DataTypes(DataType.Id)
	var Id : String? = null
	var Enabled : Boolean = false
	var ProgramNum : Int = 0
	@DataTypes(DataType.Base64Binary)
	var State : String? = null
	@DataTypes(DataType.Base64Binary)
	var Base64_Layout : String? = null
	var Volume : Double = 0.0
	var WindowX : Double? = null
	var WindowY : Double? = null
	var WindowLocked : Boolean? = null
	var RemapOnTempoChange : Boolean? = null
	var Dry : Double? = null
	@DataTypes(DataType.Base64Binary)
	var Base64_Parameters : String? = null

	// elements
	var MacroParameters : MacroParametersElement? = null
	var ModifierAssignments : ModifierAssignmentsElement? = null
	var FacePlate : FacePlateElement? = null
	var AutomationCurves : MutableList<AutomationCurveElement> = mutableListOf()
}


class FilterElement : PluginElementBase()
{
}

class PluginElement : PluginElementBase()
{
}

class AutomationCurveElement
{
	var ParamID : Int = 0
	var Points : MutableList<PointElement> = mutableListOf()
}

class PointElement
{
	var t : Double = 0.0
	var v : Double = 0.0
	var c : Double = 0.0
}

class ModifierAssignmentsElement
{
	var Modifiers : MutableList<AbstractModifierElement> = mutableListOf<AbstractModifierElement>()
}


class FacePlateElement : AbstractViewElement()
{
	var AutoSize : Boolean = false
	var AssignEnabled : Boolean? = null

	var Background : BackgroundElement? = null
	var Contents : MutableList<FacePlateContentBase> = mutableListOf<FacePlateContentBase>()
}

abstract class FacePlateContentBase
{
	// HACK: it should be IList<int>
	var Bounds : String? = null
	// HACK: it should be IList<int>
	var ParameterIDs : String? = null
}

class BackgroundElement
{
	var ImageAlpha : Double = 0.0
}

class ParameterElement : FacePlateContentBase()
{
}

class ButtonElement : FacePlateContentBase()
{
}

class XYElement : FacePlateContentBase()
{
}


class ChordTrackElement : AbstractTrackElement()
{
}


class TrackElement : AbstractContentTrackElement()
{
	// extended property to assign an InstrumentName from MIDI META event.
	var Extension_InstrumentName : String? = null
	
	var MidiVProp : Double? = null
	var MidiVOffset : Double? = null
	var Colour : String? = null
	var Solo : Boolean? = null
	var Mute : Boolean? = null

	val Clips : MutableList<ClipElementBase> = mutableListOf<ClipElementBase>()
	var OutputDevices : OutputDevicesElement? = null
	var TrackSnapshots : TrackSnapshotsElement? = null
}

class TrackSnapshotsElement
{
}

// old

class ClipElement : MidiClipElementBase()
{
	var MidiSequence : MidiSequenceElement? = null
}

// new

class MidiClipElement : MidiClipElementBase()
{
	var Sequence : SequenceElement? = null
}


abstract class MidiClipElementBase : ClipElementBase()
{
	var Type : String? = null
	var Sync : Double? = null
	var ShowingTakes : Boolean = false
	var MpeMode : Boolean = false
	var VolDb : Double = 0.0
	var OriginalLength : Double = 0.0
	var SendProgramChange : Boolean = false
	var SendBankChange : Boolean = false
}


abstract class ClipElementBase
{
	// attributes
	var Channel : Int = 0
	var Name : String? = null
	@DataTypes(DataType.Length)
	var Start : Double = 0.0
	@DataTypes(DataType.Length)
	var Length : Double = 0.0
	@DataTypes(DataType.Length)
	var Offset : Double = 0.0
	@DataTypes(DataType.Id)
	var Source : String? = null
	// new
	var Id : String? = null
	// old
	@DataTypes(DataType.Id)
	var MediaId : String? = null
	var Colour : String? = null
	var CurrentTake : Int = 0
	var Speed : Double = 0.0
	var Mute : Boolean = false
	var LinkID : Double? = null
	var LoopStartBeats : Double? = null
	var LoopLengthBeats : Double? = null

	// elements
	var Quantisation : QuantisationElement? = null
	var Groove : GrooveElement? = null
	var PatternGenerator : PatternGeneratorElement? = null
}

class StepClipElement : ClipElementBase()
{
	var Sequence : Double = 0.0
	
	var Channels : ChannelsElement? = null
	var Patterns : PatternsElement? = null
}

class ChannelsElement
{
	var Channels : MutableList<ChannelElement> = mutableListOf<ChannelElement>()
}

// HACK: <CHANNEL> can appear in both <CHANNELS> and <PATTERN>.
// This library does not provide "decent" way to distinguish serialization names, so define both members in this type.
class ChannelElement
{
	// for ChannelsElement
	var Channel : Int = 0
	var Note : Int = 0
	var Velocity : Int = 0
	var Name : String? = null
	// for PatternElement
	// HACK: they should all be arrays.
	var Pattern : String? = null // "1000101010001000"...
	var Velocities : String? = null
	var Gates : String? = null
}

class PatternsElement
{
	var Patterns : MutableList<PatternElement> = mutableListOf<PatternElement>()
}

class PatternElement
{
	var NumNotes : Int = 0
	var NoteLength : Double = 0.0
	var Channels : MutableList<ChannelElement> = mutableListOf<ChannelElement>()
}

// new

class SequenceElement : SequenceElementBase()
{
}

// old

class MidiSequenceElement : SequenceElementBase()
{
}

open class SequenceElementBase
{
	var Ver : Int = 0
	var ChannelNumber : Int = 0
	val Events : MutableList<AbstractMidiEventElement> = mutableListOf<AbstractMidiEventElement>()
}


abstract class AbstractMidiEventElement
{
	@DataTypes(DataType.Length)
	var B : Double = 0.0
}


class ControlElement : AbstractMidiEventElement()
{
	var Type : Int = 0
	var Val : Int = 0
	var Metadata : Int? = null
}


class NoteElement : AbstractMidiEventElement()
{
	var P : Int = 0
	@DataTypes(DataType.Length)
	var L : Double = 0.0
	var V : Int = 0
	var C : Int = 0
	var InitialTimbre : Double? = null
	var InitialPressure : Double? = null
	var InitialPitchbend : Double? = null
}


class SysexElement : AbstractMidiEventElement()
{
	@DataTypes(DataType.Length)
	var Time : Double = 0.0
	@DataTypes(DataType.HexBinary)
	var Data : Array<Byte>? = null
}


class QuantisationElement
{
	var Type : String? = null
	var Amount : Double? = null
}


class GrooveElement
{
	var Current : String? = null
}


class PatternGeneratorElement
{
	var Progression : ProgressionElement? = null
}


class ProgressionElement
{
}


class OutputDevicesElement
{
	val OutputDevices: MutableList<DeviceElement> = mutableListOf<DeviceElement>()
}


class DeviceElement
{
	var Name : String? = null
}


class MidiViewStateElement : AbstractViewElement()
{
	@DataTypes(DataType.Length)
	var LeftTime : Double = 0.0
	@DataTypes(DataType.Length)
	var RightTime : Double = 0.0
}


class ArrangeViewElement
{
	var MixerViewState : MixerViewStateElement? = null
}


class MixerViewStateElement
{
	@DataTypes(DataType.BooleanInt)
	var OverviewVisible : Boolean = false
	@DataTypes(DataType.BooleanInt)
	var SmallMetersVisible : Boolean = false
	@DataTypes(DataType.BooleanInt)
	var BigMetersVisible : Boolean = false
	@DataTypes(DataType.BooleanInt)
	var OutputsVisible : Boolean = false
	@DataTypes(DataType.BooleanInt)
	var ModifiersVisible : Boolean = false
	@DataTypes(DataType.BooleanInt)
	var PluginsVisible : Boolean = false
}


class EditMixGroupsElement
{
}


class AudioEditingElement
{
}


