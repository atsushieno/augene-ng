#/bin/bash

if [ '$PROJUCER'=='' ] ; then
PROJUCER=../external/tracktion_engine/modules/juce/extras/Projucer
fi
if [ '$PROJUCER_EXE'=='' ] ; then
PROJUCER_EXE=$PROJUCER/Builds/LinuxMakefile/build/Projucer
fi
SEDCMDVST="s/JUCE_PLUGINHOST_LADSPA/JUCE_PLUGINHOST_VST='1' JUCE_PLUGINHOST_LADSPA/"
# workaround for https://github.com/WeAreROLI/JUCE/issues/602
SETCMDNTB="s/setUsingNativeTitleBar/\/\/setUsingNativeTitleBar/"

APH=../external/tracktion_engine/modules/juce/extras/AudioPluginHost

#git submodule update --init --recursive

make -C $PROJUCER/Builds/LinuxMakefile

sed -e "$SEDCMDVST" $APH/AudioPluginHost.jucer > tmpout || exit 1
mv tmpout $APH/AudioPluginHost.jucer
$PROJUCER_EXE --resave $APH/AudioPluginHost.jucer
make -C $APH/Builds/LinuxMakefile

sed -e "$SEDCMDVST" AugenePlayer.jucer > tmpout || exit 1
mv tmpout AugenePlayer.jucer
# sed -e "$SETCMDNTB" Source/Main.cpp > tmpout || exit 1
# mv tmpout Source/Main.cpp
$PROJUCER_EXE --resave AugenePlayer.jucer
make -C Builds/LinuxMakefile
