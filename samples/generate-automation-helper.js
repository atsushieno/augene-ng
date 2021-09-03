const fs = require("fs");
const os = require("os");

main();

function main() {

    fs.readFile(os.homedir() + "/.local/augene-ng/plugin-metadata.json",  {}, (err, data) => {
        if (err)
            console.log(err);
        else {
            data = JSON.parse(data);
            data.forEach(plugin => {
                generatePluginMacro(plugin);
            });
        }
    });

}

function escapeName(s) {
    return s.toUpperCase()
        .replaceAll(/(\p{S}|\p{M}|\p{P}|\p{Z})/gu, "_")
        .replaceAll(/([0-9])/g, "\\$1")
        .replaceAll(' ', '_')
        .replaceAll("\\_", "_")
	;
}

function generatePluginMacro(plugin) {
    var name = escapeName(plugin.name);
    var uniqueId = plugin["unique-id"];
    var fsname = name.replaceAll("\\", "");

    var s = "#macro AUDIO_PLUGIN_USE nameLen:number, ident:string {  __MIDI #F0, #7D, \"augene-ng\", $nameLen, $ident, #F7 }\n" +
        "#macro AUDIO_PLUGIN_PARAMETER parameterID:number, val:number { \\\n" +
        "    __MIDI #F0, #7D, \"augene-ng\", 0, \\\n" +
        "    $parameterID % #80, $parameterID / #80, $val % #80, $val / #80 } \n" +
        "\n" +
        "#macro " + name + " { AUDIO_PLUGIN_USE " + uniqueId.length + ", \"" + uniqueId + "\" }\n";
    plugin.parameters.forEach(para => {
        if (para.name.match(/MIDI_CC_[0-9]+|[0-9]+/))
            return;
        var paraName = escapeName(para.name);
        s += "#macro " + name + "_" + paraName + " val { AUDIO_PLUGIN_PARAMETER " + para.index + ", $val }\n";
    });
    fs.writeFile("audio-plugins/" + plugin.type + "_" + fsname + ".mugene", s, {}, (err) => {
        if (err != null)
            console.log(err);
    });
}
