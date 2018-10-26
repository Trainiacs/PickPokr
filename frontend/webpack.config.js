const common = require("./webpack.common.js");

const distPath = __dirname + "/../../../devsite0/pickpokr";
const mode = "development"; // "production"

module.exports = common.buildConfig(mode, distPath);