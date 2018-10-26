import * as React from "react";
import * as ReactDOM from "react-dom";

import {RootPanel} from "../components/panels";
import {
	EventBackend,
	GameManager,
	LobbyManager,
	GamePlayer,
} from "./";

type ApplicationState = "start" | "signIn" | "waiting" | "lobby" | "game";

export class Application {
	private _state: ApplicationState;
	private _backend: EventBackend;

	private _lobbyManager: LobbyManager;
	private _gameManager: GameManager;
	private _message: string;
	private _playerSelf: GamePlayer;

	constructor(backend: EventBackend) {
		this._backend = backend;
		this._backend.listen("error", (a: any) => console.log(a));
		this._backend.listen("roaster", this._onRosterEvent.bind(this));
		this._backend.listen("challange", this._onChallangeEvent.bind(this));

		this._lobbyManager = new LobbyManager();
		this._gameManager = new GameManager();
	}

	async init() {
		this._state = "start";
		this._connect("test_user3");
	}

	private _connect(nick: string) {
		this._setWaiting("Waiting for connection");
		this._playerSelf = {
			name: nick,
			ready: true,
		};
		this._backend.connect(nick);
	}

	private _onRosterEvent(ev: {type: "roaster", payload: string[]}) {
		this._lobbyManager.setPlayers(LobbyManager.importPlayers(ev.payload));
		if (this._state === "waiting") {
			this._state = "lobby";
		}
		console.log(this._state, ev);
		this._render();
	}

	private _onChallangeEvent(ev: {type: "challange", payload: {answerLength: number, question?: string}[]}) {
		let words = GameManager.importWords(ev.payload);
		this._gameManager.setWords(words);
		if (this._state === "lobby") {
			this._state = "game";
		}
		this._render();
	}

	private _setWaiting(message: string) {
		this._message = message;
		this._state = "waiting";
		this._render();
	}

	private _setState(state: ApplicationState) {
		this._state = state;
		this._render();
	}

	private async _signIn(username: string, password: string) {
		let status = true; //= await this._dataSource.init(username, password);
		
	}

	private _render() {
		ReactDOM.render(
			React.createElement(RootPanel, {
				route: this._state,
				message: this._message,
				gameManager: this._gameManager,
				lobbyManager: this._lobbyManager,
				playerSelf: this._playerSelf,
			}),
			document.getElementById("root")
		);
	}
}