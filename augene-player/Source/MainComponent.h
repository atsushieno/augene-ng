
#pragma once

#include <juce_core/juce_core.h>
#include <juce_audio_basics/juce_audio_basics.h>
#include <juce_graphics/juce_graphics.h>
#include <tracktion_engine/tracktion_engine.h>
#include "efsw/efsw.hpp"

using namespace juce;

class MainComponent
    : public Component, public ChangeListener
{
public:
    //==============================================================================
    MainComponent();
    ~MainComponent();

    //==============================================================================
    void paint (Graphics&) override;
    void resized() override;

    void togglePlay (tracktion_engine::Edit& edit)
    {
        auto& transport = edit.getTransport();

        if (transport.isPlaying())
            transport.stop(false, false);
        else {
            transport.play(false);
        }
    }

    void processFileWatcherDetectedUpdate(String fullPath);

private:
    class AugeneWatchListener : public efsw::FileWatchListener {
        MainComponent* owner;
    public:
        AugeneWatchListener(MainComponent* owner) : owner(owner) {}

        void handleFileAction(efsw::WatchID watchid, const std::string& dir, const std::string& filename, efsw::Action action, std::string oldFilename = "" ) override {
            File file{owner->editFilePath};
            if (file.getFileName() != filename.c_str())
                return;
            String dirJ = String{dir};
            String filenameJ = String{filename};
            String fullPath = File{dirJ}.getChildFile(filenameJ).getFullPathName();
            if (action == efsw::Actions::Modified || action == efsw::Actions::Delete)
                owner->processFileWatcherDetectedUpdate(fullPath);
        }
    };

    //==============================================================================
    tracktion_engine::Engine engine { "augene-player" };
    std::unique_ptr<tracktion_engine::Edit> edit;
    String editFilePath;
    std::unique_ptr<efsw::FileWatcher> fileWatcher;
    std::unique_ptr<AugeneWatchListener> augeneWatchListener;
    efsw::WatchID watchID;
    int32_t projectItemIDSource{0};
    bool watchFileChanges{true}, enableHotReload{true};

    TextButton selectFileButton { "Open File" }, pluginsButton { "Plugins" },
        settingsButton { "Audio Settings" }, exportButton { "Export Plugin Metadata" },
        playPauseButton { "Play" }, stopButton { "Stop" };
    ToggleButton watchFileToggleButton{"Watch File Changes"}, hotReloadToggleButton{"Enable Hot Reload"};
    Label editNameLabel { "No Edit Loaded" };

    //==============================================================================
    void updatePlayButtonText()
    {
        if (edit != nullptr)
            playPauseButton.setButtonText (edit->getTransport().isPlaying() ? "Pause" : "Play");
    }

    void changeListenerCallback(ChangeBroadcaster*) override
    {
        updatePlayButtonText();
    }

    void unloadEditFile();

    void loadEditFile();

    void tryHotReloadEdit();

    void startFileWatcher();

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (MainComponent)
};
