
#pragma once

#include "../JuceLibraryCode/JuceHeader.h"
#include "efsw/efsw.hpp"

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

    void fileUpdated(String fullPath)
    {
        if(edit == nullptr)
            return;
        // The filewatcher implementation is weird.
        // It keeps sending the event until undefined-ish time has passed.
        // To avoid such a mess, we just disable the entire watcher and recreate ones every time.
        // It's stupid, but not in a critical performance issue.
        fileWatcher->removeWatch(watchID);

        MessageManager::callAsync([&](){
            auto& transport = edit->getTransport();
            bool wasPlaying = transport.isPlaying();
            if (wasPlaying)
                togglePlay(*edit.get());
            transport.stop(true, true);
            augeneWatchListener.reset(nullptr);
            fileWatcher.reset(nullptr);
            unloadEditFile();
            loadEditFile();
            if (wasPlaying)
                togglePlay(*edit.get()); // note that this "edit" is another instance than above.
        });
    }

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
                owner->fileUpdated(fullPath);
        }
    };

    //==============================================================================
    tracktion_engine::Engine engine { ProjectInfo::projectName };
    std::unique_ptr<tracktion_engine::Edit> edit;
    String editFilePath;
    std::unique_ptr<efsw::FileWatcher> fileWatcher;
    std::unique_ptr<AugeneWatchListener> augeneWatchListener;
    efsw::WatchID watchID;
    int32_t projectItemIDSource{0};

    TextButton selectFileButton { "Open File" }, pluginsButton { "Plugins" },
        settingsButton { "Settings" }, exportButton { "Export Plugin Metadata" },
        playPauseButton { "Play" }, stopButton { "Stop" };
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

    void unloadEditFile()
    {
        auto& transport = edit->getTransport();
        if (transport.isPlaying())
            transport.stop (true, false);
        edit.reset(nullptr);
        File editFile{editFilePath};
    }

    void loadEditFile()
    {
        File editFile{editFilePath};
        auto itemId = tracktion_engine::ProjectItemID::createNewID(++projectItemIDSource);
        edit = std::make_unique<tracktion_engine::Edit> (engine, tracktion_engine::loadEditFromFile (engine, editFile, itemId), tracktion_engine::Edit::forEditing, nullptr, 0);
        auto& transport = edit->getTransport();
        transport.addChangeListener (this);

        if (!augeneWatchListener.get()) {
            fileWatcher = std::make_unique<efsw::FileWatcher>();
            fileWatcher->watch();
            augeneWatchListener = std::make_unique<AugeneWatchListener>(this);
            watchID = fileWatcher->addWatch(editFile.getParentDirectory().getFullPathName().toStdString(),
                                            augeneWatchListener.get());
        }

        editNameLabel.setText (editFile.getFileNameWithoutExtension(), dontSendNotification);

        /*
        for (auto track : edit->getTrackList()) {
            if (!track->isAudioTrack())
                continue;
            dynamic_cast<tracktion_engine::AudioTrack*>(track)->freezeTrackAsync();
        }

        for (auto track : edit->getTrackList()) {
            while (true) {
                Thread::sleep(50);
                if (track->isFrozen(tracktion_engine::Track::FreezeType::anyFreeze))
                    break;
            }
        }
        */
    }

    JUCE_DECLARE_NON_COPYABLE_WITH_LEAK_DETECTOR (MainComponent)
};
