const common = require("./webpack.common.js");

let distPath = __dirname + "/../www";
let mode = "production";
try {
	let localConfig = require("./webpack.local.json");
	distPath = localConfig && localConfig.distPath || distPath;
	mode = localConfig && localConfig.mode || mode;
} catch (e) {
}



module.exports = common.buildConfig(mode, distPath);