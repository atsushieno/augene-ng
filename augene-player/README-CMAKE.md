# Building with CMake

If you are on Linux, you need libgtk-3.0-dev (it is required by juce-gui-extra which is referenced by tracktion_engine, but JUCE CMake module itself is not clever enough to automatically add gtk3 deps).

Once you are done with it, then run `mkdir -p build; cd build; cmake ..; make`
