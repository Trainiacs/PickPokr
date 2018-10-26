import * as React from "react";
import * as ReactDOM from "react-dom";

import {RootPanel} from "../components/panels";

type ApplicationState = "start" | "failedSignIn" | "signedIn";

export class Application {
	private _state: ApplicationState;

	constructor() {
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