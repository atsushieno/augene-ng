/*
  ==============================================================================

    This file was auto-generated!

  ==============================================================================
*/

#include "MainComponent.h"
#if ANDROID
#include "../../../../modules/juceaap_audio_plugin_processors/juce_android_audio_plugin_format.h"
#include "aap/android-context.h"
#include <android/asset_manager.h>
#include <android/asset_manager_jni.h>

namespace juce {
    extern JNIEnv *getEnv() noexcept;
}
#else
#include "jlv2_host/jlv2_host.h"
#endif

//==============================================================================

void showAudioDeviceSettings (tracktion_engine::Engine& engine)
{
    DialogWindow::LaunchOptions o;
#if ANDROID
    o.useNativeTitleBar             = false; // explicitly needed
#else
    o.useNativeTitleBar             = true;
#endif
    o.dialogTitle = TRANS("Audio Settings");
    o.dialogBackgroundColour = LookAndFeel::getDefaultLookAndFeel().findColour (ResizableWindow::backgroundColourId);
    engine.getDeviceManager().initialise(0, 2);
    o.content.setOwned (new AudioDeviceSelectorComponent (engine.getDeviceManager().deviceManager,
                                                          0, 2, 1, 2, false, false, true, true));
    o.content->setSize (400, 600);
#if ANDROID
    o.launchAsync()->setTopLeftPosition(0, 100); // we want title bar to show close button.
#else
    o.launchAsync();
#endif
}

const char* escapeJson(const juce::String src) { return src.replace("\"", "\\\"").toRawUTF8(); }

enum PseudoHeadlessCommand {
    NONE = 0,
    SCAN = 1,
    EXPORT_MML = 2,
    RENDER_WAV = 3
};

MainComponent::MainComponent()
{
    setSize (600, 400);

    engine.getDeviceManager().deviceManager.initialise(0, 2, nullptr, true);

    selectFileButton.onClick = [this] { startLoadEdit(); };

    playPauseButton.onClick = [this] {
        if (edit)
            togglePlay (*edit);
    };
    stopButton.onClick = [this] {
        if (!edit)
            return;
        auto & t = edit->getTransport();
        t.stop(false, false);
        t.setCurrentPosition(0);
    };

    auto &formatManager = engine.getPluginManager().pluginFormatManager;
#if ANDROID
    aap::getPluginHostPAL()->setPluginListCache(aap::getPluginHostPAL()->getInstalledPlugins());
    auto format = new juceaap::AndroidAudioPluginFormat();
    formatManager.addFormat (format);
#else
    formatManager.addFormat (new jlv2::LV2PluginFormat());
#endif

    PseudoHeadlessCommand pseudoHeadlessCommand{NONE};

    for (auto &cmdarg : JUCEApplication::getCommandLineParameterArray()) {
        if (cmdarg.startsWith("-NSDocumentRevisionsDebugMode"))
            continue;
        if (cmdarg == "--scan-plugins")
            pseudoHeadlessCommand = SCAN;
        else if (cmdarg == "--export-mml")
            pseudoHeadlessCommand = EXPORT_MML;
        else if (cmdarg == "--render-wav")
            pseudoHeadlessCommand = RENDER_WAV;
        else
            editFilePath = cmdarg;
    }

    // Show the plugin scan dialog
    // If you're loading an Edit with plugins in, you'll need to perform a scan first
    pluginsButton.onClick = [this]
    {
        DialogWindow::LaunchOptions o;
        o.dialogTitle                   = TRANS("Plugins");
        o.dialogBackgroundColour        = Colours::black;
        o.escapeKeyTriggersCloseButton  = true;
#if ANDROID
        o.useNativeTitleBar             = false; // explicitly needed
#else
        o.useNativeTitleBar             = true;
#endif
        o.resizable                     = true;
        o.useBottomRightCornerResizer   = true;

        auto v = new PluginListComponent (engine.getPluginManager().pluginFormatManager,
                                          engine.getPluginManager().knownPluginList,
                                          engine.getTemporaryFileManager().getTempFile ("PluginScanDeadMansPedal"),
                                          tracktion_engine::getApplicationSettings());
        activePluginListComponent.reset(v);
        v->setSize (800, 600);
        o.content.setOwned (v);
#if ANDROID
        o.launchAsync()->setTopLeftPosition(0, 100); // we want title bar to show close button.
#else
        o.launchAsync();
#endif
    };

    settingsButton.onClick  = [this] { showAudioDeviceSettings (engine); };
    exportButton.onClick = [this, &formatManager] { exportPluginSettings(formatManager); };
    renderButton.onClick = [this] { startRendering(); };

    watchFileToggleButton.setToggleState(true, NotificationType::dontSendNotification);
    watchFileToggleButton.onClick = [&] { watchFileChanges = watchFileToggleButton.getToggleState(); };
    hotReloadToggleButton.setToggleState(true, NotificationType::dontSendNotification);
    hotReloadToggleButton.onClick = [&] { enableHotReload = hotReloadToggleButton.getToggleState(); };

    updatePlayButtonText();
    editNameLabel.setJustificationType (Justification::centred);
    addAndMakeVisible(&selectFileButton);
    addAndMakeVisible(&pluginsButton);
    addAndMakeVisible(&settingsButton);
    addAndMakeVisible(&playPauseButton);
    addAndMakeVisible(&stopButton);
    addAndMakeVisible(&renderButton);
    addAndMakeVisible(&exportButton);
    addAndMakeVisible(&watchFileToggleButton);
    addAndMakeVisible(&hotReloadToggleButton);

    const File editFile (editFilePath);
    if (editFile.existsAsFile())
        loadEditFile();

    if (pseudoHeadlessCommand == SCAN) {
        auto v = new PluginListComponent(engine.getPluginManager().pluginFormatManager,
                                                       engine.getPluginManager().knownPluginList,
                                                       engine.getTemporaryFileManager().getTempFile ("PluginScanDeadMansPedal"),
                                                       tracktion_engine::getApplicationSettings());
        v->setHeadlessScanning(true);
        activePluginListComponent.reset(v);
        nextFormatForPseudoHeadlessScanning = 0;
        startTimer(100);
    }
    else if (pseudoHeadlessCommand == EXPORT_MML) {
        exportPluginSettings(formatManager);
        JUCEApplication::getInstance()->invokeDirectly(StandardApplicationCommandIDs::quit, true);
    }
    else if (pseudoHeadlessCommand == RENDER_WAV) {
        if (editFile.exists())
            startRendering();
        else
            puts("--render-wav command requires file argument to render.");
        JUCEApplication::getInstance()->invokeDirectly(StandardApplicationCommandIDs::quit, true);
    }
}

MainComponent::~MainComponent()
{
    if (edit) {
        if (edit->getTransport().isPlaying())
            edit->getTransport().stop(true, true);
    }
    engine.getTemporaryFileManager().getTempDirectory().deleteRecursively();
}

//==============================================================================
void MainComponent::paint (Graphics& g)
{
    g.fillAll (getLookAndFeel().findColour (ResizableWindow::backgroundColourId));
}

void MainComponent::resized()
{
    auto r = getLocalBounds();
    auto firstRowR = r.removeFromTop (30);
    auto secondRowR = r.removeFromTop (30);
    auto thirdRowR = r.removeFromTop(30);
    selectFileButton.setBounds (firstRowR.removeFromLeft (firstRowR.getWidth() / 3).reduced (2));
    pluginsButton.setBounds (firstRowR.removeFromLeft (firstRowR.getWidth() / 2).reduced (2));
    settingsButton.setBounds (firstRowR.reduced (2));
    playPauseButton.setBounds (secondRowR.removeFromLeft (secondRowR.getWidth() / 3).reduced (2));
    stopButton.setBounds (secondRowR.removeFromLeft (secondRowR.getWidth() / 2).reduced (2));
    exportButton.setBounds (secondRowR.reduced (2));
    renderButton.setBounds(thirdRowR.removeFromLeft(thirdRowR.getWidth() / 3).reduced (2));
    watchFileToggleButton.setBounds(thirdRowR.removeFromLeft(thirdRowR.getWidth() / 2).reduced (2));
    hotReloadToggleButton.setBounds(thirdRowR.reduced (2));
    editNameLabel.setBounds (r);
}

Random randomInstance{};

void MainComponent::startFileWatcher()
{
    if (!augeneWatchListener.get()) {
        File editFile{editFilePath};
        fileWatcher = std::make_unique<efsw::FileWatcher>();
        fileWatcher->watch();
        augeneWatchListener = std::make_unique<AugeneWatchListener>(this);
        watchID = fileWatcher->addWatch(editFile.getParentDirectory().getFullPathName().toStdString(),
                                        augeneWatchListener.get());
    }
}

void MainComponent::loadEditFile()
{
    File editFile{editFilePath};
    if (projectItemIDSource == 0)
        projectItemIDSource = editFilePath.hashCode();
    auto itemId = tracktion_engine::ProjectItemID::createNewID(projectItemIDSource);
    edit = std::make_unique<tracktion_engine::Edit> (engine, tracktion_engine::loadEditFromFile (engine, editFile, itemId), tracktion_engine::Edit::forEditing, nullptr, 0);
    edit->state.setProperty(tracktion_engine::IDs::appVersion, String{"0.1.0"}, nullptr);
    auto& transport = edit->getTransport();
    transport.addChangeListener (this);

    startFileWatcher();

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

void MainComponent::unloadEditFile()
{
    auto& transport = edit->getTransport();
    if (transport.isPlaying())
        transport.stop (true, false);
    edit.reset(nullptr);
    File editFile{editFilePath};
}

void MainComponent::processFileWatcherDetectedUpdate(String fullPath)
{
    if(edit == nullptr || !watchFileChanges)
        return;
    // The filewatcher implementation is weird.
    // It keeps sending the event until undefined-ish time has passed.
    // To avoid such a mess, we just disable the entire watcher and recreate ones every time.
    // It's stupid, but not in a critical performance issue.
    fileWatcher->removeWatch(watchID);

    MessageManager::callAsync([&](){
        augeneWatchListener.reset(nullptr);
        fileWatcher.reset(nullptr);

        auto& transport = edit->getTransport();
        bool wasPlaying = transport.isPlaying();
        if (wasPlaying)
            transport.stop(true, true);

        if (enableHotReload)
            tryHotReloadEdit();
        else
            loadEditFile();

        auto& newTransport = edit->getTransport();
        newTransport.addChangeListener (this);
        newTransport.setCurrentPosition(0);
        if (wasPlaying)
            newTransport.play(true);

        startFileWatcher();
    });
}

void MainComponent::tryHotReloadEdit()
{
    if (projectItemIDSource == 0) {
        // No previously loaded edit. Give up.
        loadEditFile();
        return;
    }
    auto hash = editFilePath.hashCode();
    if (projectItemIDSource != hash) {
        // Previous edit was different file. Give up.
        loadEditFile();
        return;
    }

    File editFile{editFilePath};
    auto itemId = tracktion_engine::ProjectItemID::createNewID(projectItemIDSource);
    auto newEdit = std::make_unique<tracktion_engine::Edit> (engine, tracktion_engine::loadEditFromFile (engine, editFile, itemId), tracktion_engine::Edit::forExamining, nullptr, 0);

    std::vector<tracktion_engine::Track*> tracksFoundInNewEdit{};
    for (auto& trackNE : newEdit->getTrackList()) {
        std::cerr << trackNE->getName() << std::endl;
        auto trackEX = std::find_if(edit->getTrackList().begin(), edit->getTrackList().end(), [trackNE](tracktion_engine::Track* t) {
            return t->getName() == trackNE->getName();
        });
        if (trackEX != edit->getTrackList().end()) {
            tracktion_engine::ClipTrack* clipTrackNE{nullptr};
            auto clipTrackEX = dynamic_cast<tracktion_engine::ClipTrack*>(*trackEX);
            if (clipTrackEX != nullptr) {
                clipTrackNE = dynamic_cast<tracktion_engine::ClipTrack *>(trackNE);
                if (clipTrackNE == nullptr)
                    // it became different kind of track. Do not treat it as identical.
                    continue;
            }

            tracksFoundInNewEdit.emplace_back(*trackEX);
            auto tempoTrack = dynamic_cast<tracktion_engine::TempoTrack*>(*trackEX);
            if (tempoTrack != nullptr)
                continue; // we handle this in tempoSequence.
            if (clipTrackEX != nullptr) {
                // Replace clip content and automation tracks
                const auto& clips = clipTrackEX->getClips();
                clipTrackEX->deleteRegion(tracktion_engine::EditTimeRange(0, edit->getLength()), nullptr);
                for (auto* clip : clipTrackNE->getClips())
                    clipTrackEX->addClip(clip);
                for (auto subTrack : clipTrackEX->getAllSubTracks(true))
                    edit->deleteTrack(subTrack);
                for (auto subTrack : clipTrackNE->getAllSubTracks(true)) {
                    tracktion_engine::TrackInsertPoint tip{clipTrackEX, nullptr};
                    edit->insertTrack(tip, subTrack->state, nullptr);
                }
            }
        } else {
            std::cerr << "Track " << trackNE->getName() << " was not found in old edit. Adding." << std::endl;
            edit->insertTrack(trackNE->state, edit->state, edit->state.getChild(0), nullptr);
            edit->getTrackList().objects.add(trackNE);
        }
    }
    std::vector<tracktion_engine::Track*> tracksToRemove{};
    for (auto& trackEX : edit->getTrackList())
        if (std::find(tracksFoundInNewEdit.begin(), tracksFoundInNewEdit.end(), trackEX) == tracksFoundInNewEdit.end())
            tracksToRemove.emplace_back(trackEX);
    for (auto trackEX : tracksToRemove)
        edit->deleteTrack(trackEX);

    edit->tempoSequence.deleteRegion(tracktion_engine::EditTimeRange(0, edit->getLength()));
    edit->tempoSequence.copyFrom(newEdit->tempoSequence);

    edit->flushState();
}

void MainComponent::exportPluginSettings(juce::AudioPluginFormatManager &formatManager) {
    auto& pluginManager = engine.getPluginManager();
    auto& deviceManager = engine.getDeviceManager();
    File configDir{File::getSpecialLocation(File::userHomeDirectory).getFullPathName() + "/.config/augene-ng/"};
    if (!configDir.exists())
        configDir.createDirectory();
    File pluginDataXml{configDir.getFullPathName() + "/plugin-metadata.json"};
    if (pluginDataXml.exists())
        pluginDataXml.deleteFile();
    auto outputStream = pluginDataXml.createOutputStream();

    outputStream->writeText("[\n", false, false, nullptr);
    bool subsequentPlugin{false};
    for (auto& pluginInfo : pluginManager.knownPluginList.getTypes()) {
        auto format = String::formatted(
                "Failed to instantiate " + pluginInfo.name + " (" + pluginInfo.fileOrIdentifier + ")");
        auto pluginStartElement = String::formatted("%s  {\"type\": \"%s\", \"name\": \"%s\", \"unique-id\": \"%d\",\n   \"file\": \"%s\", \"parameters\": [",
                                                    subsequentPlugin ? ",\n" : "",
                                                    escapeJson(pluginInfo.pluginFormatName),
                                                    escapeJson(pluginInfo.name), pluginInfo.uid,
                                                    escapeJson(pluginInfo.fileOrIdentifier));
        auto instance = formatManager.createPluginInstance(pluginInfo, deviceManager.getSampleRate(),
                                                           deviceManager.getBlockSize(), format);
        if (!instance)
            continue;
        subsequentPlugin = true;
        outputStream->writeText(pluginStartElement, false, false, nullptr);

        bool subsequentParameter{false};
        for (auto &para: instance->getParameters()) {
            auto msg = String::formatted("%s\n    {\"index\": \"%d\", \"name\": \"%s\"}",
                                         subsequentParameter ? "," : "",
                                         para->getParameterIndex(),
                                         escapeJson(para->getName(4096)));
            outputStream->writeText(msg, false, false, nullptr);
            subsequentParameter = true;
        }
        outputStream->writeText("\n  ]}", false, false, nullptr);
    }
    outputStream->writeText("\n]\n", false, false, nullptr);
    outputStream->flush();
}

void MainComponent::startRendering() {
    const juce::File wavFile = juce::File(editFilePath).withFileExtension(".wav");
    const juce::String taskName{"augene-ng renderer"};
    /*
    juce::Array<tracktion_engine::Clip*> allClips{};
    edit->visitAllTopLevelTracks([&](tracktion_engine::Track &track) {
        auto clipTrack = dynamic_cast<tracktion_engine::ClipTrack*>(&track);
        if (clipTrack)
            allClips.addArray(clipTrack->getClips());
        return true;
    });
    */

    juce::BigInteger trackBits{0};
    trackBits.setRange(0, edit->getTrackList().size(), true);

    tracktion_engine::Renderer::renderToFile(taskName, wavFile, *edit,
                                             tracktion_engine::EditTimeRange(edit->getFirstClipTime(), edit->getLength()),
                                             trackBits, true, {}, false);
}

void MainComponent::startLoadEdit() {
#if ANDROID
    ApplicationProperties applicationProperties;
    PropertiesFile::Options options{};
    options.applicationName = String{"AugenePlayer"};
    applicationProperties.setStorageParameters(options);
    auto settingsDir = applicationProperties.getUserSettings()->getFile().getParentDirectory().getParentDirectory();
    auto editFile = settingsDir.getChildFile("AugeneDemo.tracktionedit");
    if (editFile.existsAsFile())
        editFile.deleteFile();
    auto amgr = aap::get_android_asset_manager(juce::getEnv());
    auto asset = AAssetManager_open(amgr, "AugeneDemo.tracktionedit", AASSET_MODE_BUFFER);
    auto len = AAsset_getLength(asset);
    {
        void *buf = calloc(len, 1);
        AAsset_read(asset, buf, len);
        auto out = FileOutputStream{editFile};
        out.write(buf, len);
        free(buf);
    }
    editFilePath = editFile.getFullPathName();
    loadEditFile();
#else
    FileChooser fc{"Open tracktionedit File", File{}, "*.tracktionedit"};
    if (fc.browseForFileToOpen()) {
        editFilePath = fc.getResult().getFullPathName();
        loadEditFile();
    }
#endif
}

class ConsoleLogger : public Logger {
    void logMessage (const String &message) override {
        puts(message.toRawUTF8());
    }
};

static Logger* getLogger() {
    if (Logger::getCurrentLogger() == nullptr)
        Logger::setCurrentLogger(new ConsoleLogger());
    return Logger::getCurrentLogger();
}

void MainComponent::timerCallback()
{
    if (!activePluginListComponent->isScanning()) {
        auto &formatManager = engine.getPluginManager().pluginFormatManager;
        auto formats = formatManager.getFormats();
        if (nextFormatForPseudoHeadlessScanning == formats.size())
            JUCEApplication::getInstance()->invokeDirectly(StandardApplicationCommandIDs::quit, true);
        else {
            auto format = formats[nextFormatForPseudoHeadlessScanning++];
            getLogger()->outputDebugString("Scanning next plugin" + format->getName());
            activePluginListComponent->scanFor(*format);
        }
    }
}
