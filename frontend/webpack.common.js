const path = require("path");
const CopyWebpackPlugin = require('copy-webpack-plugin');

module.exports = {
	buildConfig: function(mode, distPath) {
		let development = mode === "development";
		let reactMode = development ? "development" : "production";
		let watch = development;
		let devtool = development ? "source-map" : undefined; 
		return({
			mode: mode,
			watch: watch,
			devtool: devtool,

			entry: [
				"./src/index.ts",
				"./src/sass/index.scss"
			],
			output: {
				filename: "bundle.js",
				path: distPath
			},

			resolve: {
				extensions: [".ts", ".tsx", ".js", ".json"]
			},

			module: {
				rules: [
					{ test: /\.tsx?$/, loader: "awesome-typescript-loader" },

					{ enforce: "pre", test: /\.js$/, loader: "source-map-loader"},

					{
						test: /\.scss$/,
						use: [
							"file-loader?name=styles.css",
							"sass-loader"
						]
					}
				]
			},

			plugins: [
				new CopyWebpackPlugin([
					{ from: './node_modules/react/umd/react.' + reactMode + '.js', to: distPath + '/vendor/react.js' },
					{ from: './node_modules/react-dom/umd/react-dom.' + reactMode + '.js', to: distPath + '/vendor/react-dom.js' },
					{ from: './index.html', to: distPath },
				])
			],

			externals: {
				"react": "React",
				"react-dom": "ReactDOM"
			},
		});
	}
}