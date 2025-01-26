
# (mostly atsushieno's) Plugin presets

These `.augene` files can be used as plugin presets, `<Include>d` by the actual `.augene` song projects.

## SF2Banks.augene

We use [Birch-san/juicysfplugin](https://github.com/Birch-san/juicysfplugin) to demonstrate uses of SF2 SoundFonts in augene-ng.

Since juicysfplugin saves full path to the SF2 files, I had to hardcode my local paths to those folders (it's rather how VST3 works).

All those sf2 file references point to: `/home/atsushi/.local/share/sounds/sf2`. We are not sure which directory is the best to store sfz (to reference from those `*.filtergraph`s), so things are tentative yet.

Referenced SF2 soundfonts in this bank:

- [Realistic Soundfont V2](https://musical-artifacts.com/artifacts/1035) - You can use program change to point to various GM-compatible sounds.


## SfzBanks.augene

We use [sfztools/sfizz](https://github.com/sfztools/sfizz/) to demonstrate uses of sfz samplers in augene-ng.

The same problem as SF2 happens to SFZ too (LV2 has better path
support to record file paths though).

All those sfz file references point to: `/home/atsushi/.local/share/sounds/sfz`. We are not sure which directory is the best to store sfz (to reference from those `*.filtergraph`s), so things are tentative yet.

Referenced sfz samples in this bank:

- [Virtual Playing Orchestra](http://virtualplaying.com/)
- [City Piano](https://bigcatinstruments.blogspot.com/2015/09/all-keyboard-instruments.html)
- [ATC1 Bass](https://www.synth.in/p/da-real-110.html)
- [Da Real 110](https://www.synth.in/p/da-real-110.html)
- [Ixox Flute](https://github.com/sfzinstruments/Ixox.Flute)
- [UI Standard Guitar](https://unreal-instruments.wixsite.com/unreal-instruments/standard-guitar)
- [UI Metal GTX](https://unreal-instruments.wixsite.com/unreal-instruments/metal-gtx)

Connected effector plugins:

- [mda](https://github.com/elk-audio/mda-vst3)
- [szkkng/SimpleReverb](https://github.com/szkkng/SimpleReverb)
- [olegkapitonov/KPP-VST3](https://github.com/olegkapitonov/KPP-VST3)


## SurgeBanks.augene

It is a plugin bank that makes use of [surge](https://github.com/surge-synthesizer/surge). There would not be any need for further explanation...

(It would not depend on the target platform, but it might be then we have no idea if we can fix it.)

