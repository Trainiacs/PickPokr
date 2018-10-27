import {
	Application,
	WebSocketBackend,
	MockBackend
} from "./modules";



async function _startApplication(socketHost: string, systemDataPath: string) {
	let systemId: number | null = null;

	try {
		let json = await fetch(systemDataPath).then((response: Response) => {
			return response.text();
		});
		let icomeraData = JSON.parse(json.replace("(", "").replace(");", ""));
		systemId = icomeraData.system_id;
	} catch(e) {
		console.log("Loading without system id: ");
	}

	let backend = new WebSocketBackend(socketHost + (systemId || "1234"));
	let application = new Application(backend);
	application.init();
}

_startApplication("ws://192.168.10.23:8080/ws/", "http://www.google.com");
