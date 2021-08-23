# Building with CMake

It is now the supported way to build AugenePlayer. It is complicated a little bit though.

First, you have to add a symlink from ../external/tracktion_engine/modules/juce to `juce-symlink` which is referenced in `CMakeLists.txt`.

Second, you need libgtk-3.0-dev. Maybe you don't on Mac (it is required by juce-gui-extra which is referenced by tracktion_engine, but JUCE CMake module itself is not clever enough to automatically add gtk3 stuff).

Once you are done with both, then run `mkdir -p build; cd build; cmake ..; make`
