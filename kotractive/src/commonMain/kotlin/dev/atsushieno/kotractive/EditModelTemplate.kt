package dev.atsushieno.kotractive

import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.asTimeSource
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.yearsUntil
import kotlin.random.Random

class EditModelTemplate {
	companion object {
		const val GlobalMediaPart = "0/"

		private fun newHash() = (Random.nextInt() % 0x10000000).toString(16)

		fun CreateNewEmptyEdit(): EditElement {
			var newIdFrom = 1001

			val instant = kotlin.time.Clock.System.now()

			val projectIdPart = "" + (Random.nextInt() % 1000000).toString() + '/' // FIXME: format "D06"
			val mediaFilePart = "" + (Random.nextInt() % 1000000).toString() + '/'
			return EditElement().apply {
				AppVersion = "Waveform 10.0.26"
				ProjectID = projectIdPart + newHash()
				CreationTime = instant.epochSeconds
				Transport = TransportElement()
				MacroParameters = MacroParametersElement().apply { Id = newIdFrom++.toString() }
				TempoSequence = TempoSequenceElement().apply {
					TimeSignatures = mutableListOf(
						TimeSigElement().apply {
							Numerator = 4
							Denominator = 4
							StartBeat = 0.0
						}
					)
					Tempos = mutableListOf(
						TempoElement().apply {
							StartBeat = 0.0
							Bpm = 120.0
						}
					)
				}
				PitchSequence = PitchSequenceElement().apply {
					Pitches = mutableListOf(PitchElement().apply {
						StartBeat = 0.0
						Pitch = 60.0
					})
				}
				Video = VideoElement()
				AutoMapXml = AutoMapXmlElement()
				ClickTrack = ClickTrackElement().apply { Level = 0.60 }
				Id3VorbisMetadata = Id3VorbisMetadataElement().apply {
					TrackNumber = 1.0
					Date = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.UTC).year.toString()
				}
				MasterVolume = MasterVolumeElement().apply {
					Plugins = mutableListOf(
						PluginElement().apply {
							Type = "volume"
							Id = newIdFrom++.toString()
							Enabled = true
							Volume = 0.666
							MacroParameters = MacroParametersElement().apply {
								Id = newIdFrom++.toString()
							}
							ModifierAssignments = ModifierAssignmentsElement()
						}
					)
				}
				Racks = RacksElement()
				AuxBusNames = AuxBusNamesElement()
				InputDevices = InputDevicesElement()
				TrackComps = TrackCompsElement()
				Tracks = mutableListOf(
					TempoTrackElement().apply {
						Name = "Global"
						Id = newIdFrom++.toString()
						MacroParameters = MacroParametersElement().apply {
							Id = newIdFrom++.toString()
						}
						Modifiers = ModifiersElement()
					},
					MarkerTrackElement().apply {
						Id = newIdFrom++.toString()
						MacroParameters = MacroParametersElement().apply {
							Id = newIdFrom++.toString()
						}
						Modifiers = ModifiersElement()
					},
					ChordTrackElement().apply {
						Name = "Chords"
						Id = newIdFrom++.toString()
					},

					TrackElement().apply {
						Clips.add(
							MidiClipElement().apply {
								Length = 8.0
								Id = newIdFrom++.toString()
								Sequence = SequenceElement().apply {
									Events.add(
										NoteElement().apply {
											P = 60
											V = 100
											B = 1.0
											L = 0.25
										}
									)
								}
								Name = "MIDI Clip 1"
								Offset = 0.0
								Type = "midi"
								Colour = "#00008000"
							}
						)
						Colour = "#00008000"
						Height = 60.0
						MacroParameters = MacroParametersElement()
						Id = newIdFrom++.toString()
						Modifiers = ModifiersElement()
						Name = "track 1"
						Plugins = mutableListOf(
							PluginElement().apply {
								Type = "volume"
								Id = newIdFrom++.toString()
								Enabled = true
								Volume = 0.666
								MacroParameters = MacroParametersElement().apply {
									MediaId = mediaFilePart + newHash()
								}
							},

							PluginElement().apply {
								Type = "level"
								Id = newIdFrom++.toString()
								Enabled = true
								Volume = 0.666
								MacroParameters = MacroParametersElement().apply {
									MediaId = mediaFilePart + newHash()
								}
							}
						)
						OutputDevices = OutputDevicesElement().apply {
							OutputDevices.add(
								DeviceElement().apply {
									Name = "(default audio output)"
								}
							)
						}
					}
				)
			}
		}
	}
}

