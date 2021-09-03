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
    console.log("#macro AUDIO_PLUGIN_USE nameLen:number, ident:string {  __MIDI #F0, #7D, \"augene-ng\", $nameLen, $ident, #F7 }");
    console.log("#macro AUDIO_PLUGIN_PARAMETER parameterID:number, val:number { \\");
    console.log("    __MIDI #F0, #7D, \"augene-ng\", 0, \\");
    console.log("    $parameterID % #80, $parameterID / #80, $val % #80, $val / #80 } ");
    console.log("");
    console.log("#macro " + name + " { AUDIO_PLUGIN_USE " + uniqueId.length + ", \"" + uniqueId + "\" }");
    plugin.parameters.forEach(para => {
        var paraName = escapeName(para.name);
        console.log("#macro " + name + "_" + paraName + " val { AUDIO_PLUGIN_PARAMETER " + para.index + ", $val }");    
    });
}
