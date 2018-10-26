import {
	Application,
	MockBackend,
} from "./modules";

let backend = new MockBackend();
let application = new Application(backend);
application.init();