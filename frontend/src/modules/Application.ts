import * as React from "react";
import * as ReactDOM from "react-dom";

import {RootPanel} from "../components/panels";
import {EventBackend} from "./Backend";

type ApplicationState = "start" | "failedSignIn" | "signedIn";

export class Application {
	private _state: ApplicationState;
	private _backend: EventBackend;

	constructor(backend: EventBackend) {
		this._backend = backend;
		this._backend.listen("error", (a: any) => console.log(a));
		this._backend.connect("test_user");
		this._state = "start";
	}

	async init() {
		this._setState("start");
	}

	private _setState(state: ApplicationState) {
		this._state = state;
		this._render();
	}

	private async _signIn(username: string, password: string) {
		let status = true; //= await this._dataSource.init(username, password);
		this._setState(status ? "signedIn" : "failedSignIn");
	}

	private _getPanel(state: ApplicationState): any {
		switch(this._state) {
			case "start": {
				return React.createElement(RootPanel, {
				});
			}
		}
	}

	private _render() {
		ReactDOM.render(
			this._getPanel(this._state),
			document.getElementById("root")
		);
	}
}