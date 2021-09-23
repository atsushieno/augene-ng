
## SfzBanks.augene

We use [sfztools/sfizz](https://github.com/sfztools/sfizz/) to demonstrate uses of sfz sampler plugins in augene-ng.

Since sfizz saves full path to the sfz files, I had to hardcode my local
paths to those folders (it's rather how VST3 works; LV2 has better path
support to record file paths).

All those sfz file references point to: `/home/atsushi/.local/share/sounds/sfz`. We are not sure which directory is the best to store sfz (to reference from those `*.filtergraph`s), so things are tentative yet.

Referenced sfz samples in this set:

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


