#/bin/sh

PROJUCER=../external/tracktion_engine/modules/juce/extras/Projucer
PROJUCER_EXE=$PROJUCER/Builds/MacOSX/build/Debug/Projucer.app/Contents/MacOS/Projucer

APH=../external/tracktion_engine/modules/juce/extras/AudioPluginHost

git submodule update --init --recursive

pushd $PROJUCER/Builds/MacOSX && xcodebuild && popd

sed -e "$SEDCMDVST" $APH/AudioPluginHost.jucer > tmpout || exit 1
mv tmpout $APH/AudioPluginHost.jucer
$PROJUCER_EXE --resave $APH/AudioPluginHost.jucer
pushd $APH/Builds/MacOSX && xcodebuild && popd

# $PROJUCER_EXE --resave AugenePlayer.jucer
cd Builds/MacOSX && xcodebuild && cd ../..
