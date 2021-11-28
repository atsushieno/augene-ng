#!/bin/bash

cd augene-player
ln -s ../external/tracktion_engine/modules/juce juce-symlink
mkdir build
cd build
cmake .. -G Ninja
ninja

