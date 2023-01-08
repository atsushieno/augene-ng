# augene-ng rationale

## What is MML, and why?

For brief history - MML, Music Macro Language here, is a classic language from 20th. century. It was mostly popular in Japan in composing music for computers. Computers here means PCs (MSX, X68000, NEC PC-88/98, FM-Towns etc.) and game consoles (NES..SNES and those arcade games) etc. The environment was unique in that they involve only on-device "instrumentable" sound chips (FM, PSG, or BEEP, or PCM later). MMLs have never been a single language. Many people and vendors developed their own MML variants that are tailored for their target sound devices. This trend lasted until MIDI and software synthesizers took over the popularity.

During MIDI ages some people still used MML for composing MIDI song files (the most popular compiler would be [text music sakura](https://sakuramml.com/)), but there were also other "advanced" tools like trackers, pianoroll editors and even score-based editors. They became increasingly popular as they look more "intuitive". MMLs needed somewhat programming-like skills with steep learning curve.

However some people still find MML most understandable and easy to tuckle. Most of them go for chiptune where MML used to dominate and is still easiest to achieve outcomes. But putting them aside, does this make sense in 202x? Well, probably (as I am one of those). Let's compare MML with other technologies that make it possible to compose music.

(1) Live coding languages - they are literally used to perform live coding. Typically, when you edit a line on their "editor" (can be a usual text editor with plugins) and "run" it, it is sent to the command server and anything corresponding happens. Previously sent commands on the lines are gone. MMLs are on the other hand, to generate some music data such as SMF. Then we can use any audio tool (music player, DAWs or any kind of converters) to process or play it. It is like REPL vs. static compilation.

(2) lilypond - to my understanding, lilypond has the closest syntax to MMLs. Yet it is primarily for authoring music scores, not instructing sound devices to perform music. Tiny differences on pitchbend, control changes,  velocity, and timeline position based on ticks do not matter on scores. Instructions for sound devices could even harm scoring ("this note should be c4 of 4th. length, but it's better with this instrument with shorter note length with long release rate").

(3) MusicXML - I'm not sure how broadly it can be used, but authoring music completely with XML syntax is not very productive. XML is in general for tools, with some bonus human readability. MusicXMLs had better be "generated" by tools rather than human work.

(3.1) bitwig/dawproject - it is another attempt to define a "common" representation for music production that show up recently (2020). Its feature set is mostly only for Bitwig itself so far, and as I placed it next to MusicXML, it is also primarily for tools. We need more intuitive way to author music.

(4) generic programming languages - we can take some music performance runtime (take [ossia](https://github.com/OSSIA/libossia) as a performance backend, or [SuperCollider](https://supercollider.github.io/) servers to perform our "musical" instructions (like [Tidal Cycles](https://tidalcycles.org/)). It seems completely doable, but we need some sequencer implementation, and the frontend language needs to be composition friendly.

Regardless of whether it is generic programming language or not, we would need to abstract away from the actual implementation, to not depend on certain backend technology, which is what MIDI once achieved at different layer.

(5) DAWs - there are lots of blackboxes with a bunch of app-specific operations, while they are not many. Data format tends to be binary, which is not comparable on version control systems. Copypasting in general does not work flexibly enough, at our own will. Good for preview and edits on the fly, but they are certainly "different".

## Modernizing MML production

MML was invented in 1980s and there are bunch of differences from modern music production. Most notably, there had never been a MML processor that was tailored for audio plugins. At best the target audio plugins had to be configured as a virtual MIDI device. General availability of those plugins is essential and that would mostly work, but then the potential is limited to whatever MIDI 1.0 messages can express.

augene-ng is built on top of mugene-ng, which used to provide only MIDI 1.0 outputs. Now it supports MIDI 2.0 UMPs, it can generate more refined control change values, velocity in high precision, per-note control changes (also pitchbend). mugene-ng is basically a binary generation tool that is tailored for track list with metadata (i.e. SMF-alike) with a bunch of macros that wrap primitive binary output operations (like `__MIDI {90, 0, 60, 120 }`), transforming it for MIDI 2.0 UMPs was a minor issue. Data precision wise, MIDI 2.0 in general provides whatever we edit on MIDI tracks (or "instrument tracks") on DAWs (unless you really think you need 64-bit precision).

MIDI 2.0 still does not provide ways to access to audio plugins directly, so we have to map MIDI tracks (a "MIDI track" does not exist in MIDI 2.0 specifications either, but we can borrow the concepts from SMF specification). How to deal with audio plugins is somewhat difficult problem to solve. augene-ng provides some ways to control them:

(1) defining audio plugin graph: augene-ng solves that problem simply by providing external "project" files. We define "filtergraphs" by simply reusing JUCE AudioPluginHost and its output `*.filtergraph` files. We depend on their binary state (represented as base64 in `*.filtergraph`s).

(2) controlling audiograph: it is done through system exclusive messages. They have to be then processed by player or converter to the destination. In augene-ng, MidiToTracktionEditConverter handles them to look up the target audio plugin, and generate `<AUTOMATIONTRACK>` to generate those parameter changes. The target plugins have to be defined as the macros in mugene MML, respectively.

(3) state: it is handled by AudioPluginHost as we cannot really generate them as only plugin editors have "controls" (on both UI technology context or audio processor context).

I haven't played with "audio tracks" in augene-ng yet, but I assume that the required operations over audio tracks would be expressed in much simpler and could be in non-intuitive languages for editing. (How complicated are `<AUDIOTRACK>`s in your `*.tracktionedit` ?)

## portable sequencer engine

It is kind of a concern that augene-ng currently depends totally on Tracktion Engine. But to my understanding, it is the only sequencer engine that I can think of that fill the following requirements:

- available as open source software
- runnable on Linux, Mac, Windows, Android and potentially other platforms.

Typical Linux DAWs do not meet the second requirements. I have been developing [Audio Plugins For Android](https://github.com/atsushieno/aap-core) and Tracktion Engine is one of the closest choice. (Note that it's not "working" yet, as we need some PoC song files that can be played there. `*.tracktionedit` files created by Tracktion Waveform definitely does not work).

## towards generative music production across platforms

What MIDI achieved in the past was ubiquitous music representation. And it once was achieved. It still works to some extent. The common transparent standard brought in generative music production. And in fact that was what MML used to achieve too. Like SVG and MathML over earlier proprietary technology.

Even for desktop, many commercial DAWs are not ready for cross-platform uses. Reaper, Bitwig, Tracktion, Ardour and Zrythm are good citizens there. And even among those DAWs, data portability is not usually achieved. It is partly by plugin framework nature. We need some higher level of representation of those audio plugins for portability.

Data location is another problem. Plugins do not really pay attention to making file paths portable. If your plugin saves the file path like `c:\Users\atsushi\sf2\FluidGM_Mono.sf3` directly in your state binary on Windows, it does not load on MacOS or Linux. Access to local file paths need to be abstracted away, relative to "candidate paths" per platform setup. [LV2 State specification](https://lv2plug.in/ns/ext/state) does excellent job here (`makePath`, `mapPath` etc.). 



