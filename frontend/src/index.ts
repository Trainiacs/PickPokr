import {
	Application,
	WebSocketBackend,
	MockBackend
} from "./modules";

fetch("http://icomera.trainhack.com/api/jsonp/system/").then((response: Response) => {
	return response.text();
}).then((json) => {
	let icomeraData = JSON.parse(json.replace("(", "").replace(");", ""));
	console.log(icomeraData);

	let backend = new WebSocketBackend("ws://10.101.1.92:8080/" + (icomeraData.system_id || "1234"));
	let application = new Application(backend);
	application.init();
});
