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

MainComponent::MainComponent()
{
    setSize (600, 400);

    engine.getDeviceManager().deviceManager.initialise(0, 2, nullptr, true);

    editFilePath = JUCEApplication::getCommandLineParameters().replace ("-NSDocumentRevisionsDebugMode YES", "").unquoted().trim();

    selectFileButton.onClick = [this] {
#if ANDROID
        ApplicationProperties applicationProperties;
        PropertiesFile::Options options{};
        options.applicationName = String{"AugenePlayer"};
        applicationProperties.setStorageParameters(options);
        auto settingsDir = applicationProperties.getUserSettings()->getFile().getParentDirectory().getParentDirectory();
        auto editFile = settingsDir.getChildFile("AugeneDemo.tracktionedit");
        if (true/*editFile.getFullPathName().isEmpty() || !editFile.existsAsFile()*/) { // always overwrite
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
    };

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
        v->setSize (800, 600);
        o.content.setOwned (v);
#if ANDROID
        o.launchAsync()->setTopLeftPosition(0, 100); // we want title bar to show close button.
#else
        o.launchAsync();
#endif
    };

    settingsButton.onClick  = [this] { showAudioDeviceSettings (engine); };

    exportButton.onClick = [this, &formatManager] {
        auto& pluginManager = engine.getPluginManager();
        auto& deviceManager = engine.getDeviceManager();
        File configDir{File::getSpecialLocation(File::userHomeDirectory).getFullPathName() + "/.local/augene-ng/"};
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
    };

    updatePlayButtonText();
    editNameLabel.setJustificationType (Justification::centred);
    addAndMakeVisible(&selectFileButton);
    addAndMakeVisible(&pluginsButton);
    addAndMakeVisible(&settingsButton);
    addAndMakeVisible(&playPauseButton);
    addAndMakeVisible(&stopButton);
    addAndMakeVisible(&exportButton);
    addAndMakeVisible(&editNameLabel);

    const File editFile (editFilePath);
    if (editFile.existsAsFile())
        loadEditFile();
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
    auto topR = r.removeFromTop (30);
    auto nextR = r.removeFromTop (30);
    selectFileButton.setBounds (topR.removeFromLeft (topR.getWidth() / 3).reduced (2));
    pluginsButton.setBounds (topR.removeFromLeft (topR.getWidth() / 2).reduced (2));
    settingsButton.setBounds (topR.reduced (2));
    playPauseButton.setBounds (nextR.removeFromLeft (nextR.getWidth() / 3).reduced (2));
    stopButton.setBounds (nextR.removeFromLeft (nextR.getWidth() / 2).reduced (2));
    exportButton.setBounds (nextR.reduced (2));
    editNameLabel.setBounds (r);
}
