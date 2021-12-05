#!/bin/bash

cd external/tracktion_engine/modules/juce/
patch -i ../../../../audiopluginhost-lv2.patch -p1
cd ../../../../
mkdir build-pluginhost
cd build-pluginhost
pwd
cmake -DCMAKE_BUILD_TYPE=Debug -DJUCE_BUILD_EXTRAS=ON ../external/tracktion_engine/modules/juce/
make AudioPluginHost
cd ..
