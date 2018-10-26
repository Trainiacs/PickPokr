import {
	Application,
	WebSocketBackend
} from "./modules";

let backend = new WebSocketBackend("http://snajs.se");
let application = new Application(backend);
application.init();