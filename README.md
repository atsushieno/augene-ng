
# augene-ng: MML compiler for audio plugins sequencer engine

augene(-ng, next gen) is an experimental compound music authoring toolchain that brings old-fashion MML (music macro language) compiler integrated into modern sequencer that is also used in Tracktion Waveform DAW (so far). It is nothing but a proof of concept so far, whilst I (@atsushieno) plan to use it for own production.

You can also have a quick glance at the project by [my slides for lightening talk at ADC 2019](https://speakerdeck.com/atsushieno/create-music-in-199x-language-for-2019-sequencer) for a bit more details.

The application consists of the following software and libraries behind:

- The project model implemented in this repository which contains a set of MML sources and associated audio plugin filter graphs, converts SMF to audio plugin based songs (.tracktionedit)
- MML compiler [mugene-ng](https://github.com/atsushieno/mugene-ng) - compiles MML into SMF.
- [JUCE](https://github.com/juce-framework/JUCE) AudioPluginHost for editing audio graph.
- [tracktion_engine](https://github.com/Tracktion/tracktion_engine/) - music playback engine.
- [Compose for Desktop](https://github.com/JetBrains/compose-jb), cross-platform desktop port of Jetpack Compose. (The application itself is desktop-only, so far, as it depends on a lot of desktop filesystem idioms.)


# Usage

NOTE: before using augene, you most likely have to build things (explained in the next section).

launch `augene-gui` application. It is a cross-platform Kotlin/JVM Compose for Desktop GUI application.

**TODO: rewrite here**

By default those lists are actually empty. It's a screenshot of the app that has loaded sample data that makes use of Collective (bundled with Tracktion [Waveform](https://www.tracktion.com/products/waveform)).

To use this app, there are couple of things to do - Configure the app. Namely paths to two external tools are needed:

- augene-player (JUCE app in this repository, which is mostly based on PlaybackDemo in tracktion_engine repository)
- AudioPluginHost (can be found in JUCE extras)

The next step is to build a list of locally installed audio plugins. Begin with "Plugins" button to start the process.

![build audio plugin list](docs/images/augene-player-plugin-list.png)
Once you are done with above, then you're ready to use the app. You can open a `*.tracktionedit` file and play it. Note that if you don't have the audio plugins specified in the edit file, you are unable to play it.

To compose your own music, create new audiograph and new MML for each list, which can be performed via the buttons on each tab. Then use "Compile" command from the FAB (floating action button).


# Building

## augene-player

There are two primary steps to build the whole "augene" application. The first step is "augene-player" part, which is a JUCE based C++ application. It is a typical JUCE application project so you can build it with the following steps:

- Build Projucer if you don't have it yet (follow JUCE documentation)
- launch Projucer, open `AugenePlayer.jucer`, and save projects, or run `Projucer --resave AugenePlayer.jucer`.
- Build the project for your platform. Project files are under `Build/*` e.g. `Build/LinuxMakefile`.

For Linux environment there is a shorthand script `build.sh` and for Mac environment there is `build-osx.sh` (not actively maintained, so it might need some fixes from time to time). Since LV2 integration via [lvtk/jlv2](https://github.com/lvtk/jlv2) is enabled, you'll need LV2 packages installed on MacOS too.

### Enabling VST2

If you have VST2 SDK and would like to add support for VST2, open AugenePlayer.jucer in Projucer (and probably AudioPluginHost.jucer if you once tried to build it from build.sh) on Projucer and select `juce_audio_processors` module and enable VST(2) there, then save project.

## kotractive, augene, and augene-gui

Another chunk of the application is the augene project builder (or "editor") which builds Compose for Desktop based GUI app/tool.

Due to current limitation of Kotlin Multiplatform project structure, there are 3 projects to just build one single app... :

- `kotractive`, which provides basic "tracktionedit" file data model using [`ksp`](https://github.com/google/ksp/), in Kotlin Multiplatform
- `augene`, which provides Augene project data model and manipulator API, in Kotlin Multiplatform
- `augene-gui`, which is a GUI application project using Compose for Desktop Multiplatform, JVM-only

```
$ cd kotracktive && ./gradlew publishToMavenLocal && cd ..
$ cd augene && ./gradlew publishToMavenLocal && cd ..
$ cd augene-gui && ./gradlew package && cd ..
```


## Android build

AugenePlayer is being ported to Android as [aap-juce-augene](https://github.com/atsushieno/aap-juce-augene). It still does not successfully run, and player only.

# Augene project data format

An augene project is a simple set of XML described in a project file which looks like this:

```
<AugeneProject xmlns:xsd="http://www.w3.org/2001/XMLSchema" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
  <Includes>
    <Include Bank="1" Source="Banks/SfzBanks.augene" />
    <Include Bank="2" Source="Banks/SF2Banks.augene" />
    <Include Bank="3" Source="Banks/SurgeBanks.augene" />
  </Includes>
  <MasterPlugins>
    <MasterPlugin>MasterPlugin1.filtergraph</MasterPlugin>
  </MasterPlugins>
  <AudioGraphs>
    <AudioGraph Id="GrandPiano1" Source="sfizz_city_piano_1.filtergraph" />
  </AudioGraphs>
  <Tracks>
    <AugeneTrack>
      <Id>1</Id>
      <AudioGraph>Unnamed.filtergraph</AudioGraph>
    </AugeneTrack>
  </Tracks>
  <MmlFiles>
    <MmlFile>foobar.mugene</MmlFile>
  </MmlFiles>
  <MmlStrings>
    <MmlString>![CDATA[ 1 @0 V110 v100 o5 l8 cegcegeg  > c1 ]]></MmlString>
  </MmlStrings>
</AugeneProject>
```

Here is a list of elements:

| Element | feature |
|-|-|
| AugeneProject | the root element |
| Includes | container of `Include` elements. |
| Include | include other project files. They can also be a bank list of AudioGraph. See description below. |
| AudioGraphs | container of `AudioGraph` elements. |
| AudioGraph | gives a filtergraph a name so that it can be referenced by `AudioGraph` element within `AugeneTrack` element. |
| MasterPlugins | holds a list of master plugins |
| MasterPlugin | specifies an AudioGraph file that is used as a master plugin |
| Tracks | holds a list of tracks |
| AugeneTrack | a track definition specifier which holds an Id and an AudioGraph file (so far only one plugin is specified. Rooms for improvements. |
| MmlFiles | holds a list of MML files |
| MmlFile | specifies an MML source file to be compiled and converted to the edit file. |
| MmlStrings | holds a list of MML strings |
| MmlString | specifies an MML string to be compiled and converted to the edit file. |

An Augene project can include other Augene project files using `Include` element. It is useful to represent a bank of preset filtergraphs. On an `Include` element, `Bank` and `BankMsb` attributes indicate bank select MSB, `BankLsb` attribute indicates bank select LSB (`Bank` is equivalent to `BankMsb` here). `Source` attribute indicates the *included* file path, relative to the *including* file path.

An `AudioGraph` can be referenced by its `Id` attribute, by (1) mugene MIDI track with `INSTRUMENTNAME` meta event, or (2) `AudioGraph` attribute on `AugeneTrack` elements.

All tracks in either MML format (file or string) are converted into tracktionedit. Then audio graphs in the project are interpreted and converted to `PLUGIN` element in tracktionedit and then for each defined track by `Tracks` elements, if there is any graph whose `Id` is identical to the track's `AudioGraph` then the audio graph is attached to the track.

One thing to note is that while mugene supports track number in double (floating point number) SMF does not have "track numbers" and numbers are counted only by sequential index (0, 1, 2...),  the mappings could be totally different. It is always to indicate audio graph by INSTRUMENTNAME meta event in mugene MML, or supplementally use `AugeneTrack`'s mappings.


# Authoring Tips

If you are Tracktion Waveform user, you would like to examine the output `*.tracktionedit` file with Waveform. To do so, you will have to manually create a tracktion project (it is a binary file that Augene.exe does not support generation) and let it point to the edit file. To make it happen, you will most likely have to name your `*.augene` project file as `(projectname) Edit 1.augene`, or rename your edit in the track directly to match your project file.


# License

The augene-player part (JUCE application) is released under the GPLv3 license.

The kotlin projects are released under the MIT license.


## Dependencies

There are couple of dependencies in this application:

- [JUCE](https://juce.com/) - GPLv3 (or commercial).
- [Tracktion/tracktion_engine](https://github.com/Tracktion/tracktion_engine/) - ditto.
- [lvtk/jlv2](https://github.com/lvtk/jlv2) - ditto (in jlv2_host).
- [SpartanJ/efsw](https://github.com/SpartanJ/efsw) - MIT.
