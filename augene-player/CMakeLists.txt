
cmake_minimum_required(VERSION 3.12)

project(AUGENE_PLAYER VERSION 0.1.0)

find_package(PkgConfig REQUIRED)

pkg_check_modules(GTK3 gtk+-x11-3.0)
if(NOT ANDROID)
pkg_check_modules(LILV lilv-0)
pkg_check_modules(SUIL suil-0)
endif()

add_subdirectory(juce-symlink)

juce_add_gui_app(AugenePlayer
	PRODUCT_NAME "AugenePlayer")

target_compile_definitions(AugenePlayer PRIVATE
    JUCE_ALSA=1
    JUCE_DIRECTSOUND=1
    JUCE_PLUGINHOST_LADSPA=0
    JUCE_PLUGINHOST_VST=0
    JUCE_PLUGINHOST_VST3=1
    JUCE_USE_CAMERA=0
    JUCE_USE_CDBURNER=0
    JUCE_USE_CDREADER=0
    JUCE_USE_CURL=0
    JUCE_USE_FLAC=1
    JUCE_USE_OGGVORBIS=1
    JUCE_WASAPI=1
    JUCE_WEB_BROWSER=0)

juce_add_module(../external/tracktion_engine/modules/tracktion_engine)
if(NOT ANDROID)
juce_add_module(../external/jlv2/modules/jlv2_host)
target_include_directories(AugenePlayer PRIVATE
	${LILV_INCLUDE_DIRS}
	${SUIL_INCLUDE_DIRS}
	)
endif()

target_include_directories(AugenePlayer PRIVATE
	${GTK3_INCLUDEDIR}
        ../external/jlv2/modules
	../external/tracktion_engine/modules/
	../external/efsw/include/
	../external/efsw/src/)

target_sources(AugenePlayer PRIVATE
../external/efsw/src/efsw/platform/posix/SystemImpl.cpp
../external/efsw/src/efsw/platform/posix/ThreadImpl.cpp
../external/efsw/src/efsw/platform/posix/FileSystemImpl.cpp
../external/efsw/src/efsw/platform/posix/MutexImpl.cpp
../external/efsw/src/efsw/platform/win/SystemImpl.cpp
../external/efsw/src/efsw/platform/win/ThreadImpl.cpp
../external/efsw/src/efsw/platform/win/FileSystemImpl.cpp
../external/efsw/src/efsw/platform/win/MutexImpl.cpp
../external/efsw/src/efsw/DirWatcherGeneric.cpp
../external/efsw/src/efsw/WatcherGeneric.cpp
../external/efsw/src/efsw/FileWatcherCWrapper.cpp
../external/efsw/src/efsw/Debug.cpp
../external/efsw/src/efsw/FileInfo.cpp
../external/efsw/src/efsw/Log.cpp
../external/efsw/src/efsw/DirectorySnapshot.cpp
../external/efsw/src/efsw/Watcher.cpp
../external/efsw/src/efsw/FileSystem.cpp
../external/efsw/src/efsw/DirectorySnapshotDiff.cpp
../external/efsw/src/efsw/WatcherInotify.cpp
../external/efsw/src/efsw/System.cpp
../external/efsw/src/efsw/FileWatcherGeneric.cpp
../external/efsw/src/efsw/FileWatcherKqueue.cpp
../external/efsw/src/efsw/Thread.cpp
../external/efsw/src/efsw/FileWatcherFSEvents.cpp
../external/efsw/src/efsw/FileWatcherImpl.cpp
../external/efsw/src/efsw/WatcherKqueue.cpp
../external/efsw/src/efsw/FileWatcherInotify.cpp
../external/efsw/src/efsw/FileWatcher.cpp
../external/efsw/src/efsw/WatcherFSEvents.cpp
../external/efsw/src/efsw/FileWatcherWin32.cpp
../external/efsw/src/efsw/WatcherWin32.cpp
../external/efsw/src/efsw/Mutex.cpp
../external/efsw/src/efsw/String.cpp
	Source/Main.cpp
	Source/MainComponent.cpp)

target_link_libraries(AugenePlayer PRIVATE
	juce::juce_gui_extra
	tracktion_engine
	jlv2_host)

if(NOT ANDROID)
target_link_libraries(AugenePlayer PRIVATE
	${LILV_LIBRARIES}
	${SUIL_LIBRARIES}
	)
endif()
