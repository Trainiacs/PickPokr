import * as React from "react";
import * as ReactDOM from "react-dom";

import {RootPanel} from "../components/panels";
import {
	EventBackend,
	GameManager,
	LobbyManager,
	GamePlayer,
} from "./";

type ApplicationState = "start" | "signIn" | "waiting" | "lobby" | "game" | "showPin" | "showPlayers" | "info" | "winner" | "looser";

export class Application {
	private _state: ApplicationState;
	private _backend: EventBackend;

	private _lobbyManager: LobbyManager;
	private _gameManager: GameManager;
	private _message: string;
	private _playerSelf: GamePlayer;

	private _exchangePin: string;

	constructor(backend: EventBackend) {
		this._backend = backend;
		this._backend.listen("error", (a: any) => console.log(a));
		this._backend.listen("roaster", this._onRosterEvent.bind(this));
		this._backend.listen("challenge", this._onChallangeEvent.bind(this));
		this._backend.listen("exchangePin", this._onExchangePinEvent.bind(this));
		this._backend.listen("winner", this._onWinnerEvent.bind(this));
		this._backend.listen("badGuess", this._onBadGuessEvent.bind(this));

		this._lobbyManager = new LobbyManager();
		this._gameManager = new GameManager();

		this._exchangePin = "";
	}

	async init() {
		this._state = "start";
		this._connect("nick_" + Math.floor(Math.random() * 999));
	}

	private _connect(nick: string) {
		this._setWaiting("Waiting for connection");
		this._playerSelf = {
			name: nick,
			ready: true,
		};
		this._backend.connect(nick);
	}

	private _onWinnerEvent(ev: {type: "winner", payload: {nick: string, keyword: string}}) {
		this._setState(ev.payload.nick === this._playerSelf.name ? "winner" : "looser");
	}

	private _onBadGuessEvent(ev: {type: "winner", payload: {nick: string, keyword: string}}) {
		this._setInfo("Bad guess \"" + ev.payload.keyword + "\" by " + ev.payload.nick);
	}

	private _onExchangePinEvent(ev: {type: "exchangePin", payload: string}) {
		this._exchangePin = ev.payload;
		if (this._state === "waiting") {
			this._state = "showPin";
		}
		this._render();
	}

	private _onRosterEvent(ev: {type: "roaster", payload: string[]}) {
		this._lobbyManager.setPlayers(LobbyManager.importPlayers(ev.payload));
		if (this._state === "waiting") {
			this._state = "lobby";
		}
		console.log(this._state, ev);
		this._render();
	}

	private _onChallangeEvent(ev: {type: "challenge", payload: {answerLength: number, question?: string}[]}) {
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

	private _setInfo(message: string) {
		this._message = message;
		this._state = "info";
		this._render();
	}

	private _onEvent(event: {type: string, meta: any}) {
		console.log(event);
		switch(event.type) {
			case "setRoute": {
				this._setState(event.meta);
				break;
			}
			case "shareAll": {
				this._setWaiting("Waiting for share PIN");
				this._backend.send({type: "exchangePinRequest", payload:{}});
				break;
			}
			case "makeGuess": {
				this._backend.send({type: "guess", payload: event.meta.toLowerCase()});
				this._setState("game");
				break;
			}
		}
		
	}

	private _setState(state: ApplicationState) {
		this._state = state;
		console.log(this._state);
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
				exchangePin: this._exchangePin,
				onEvent: this._onEvent.bind(this),
			}),
			document.getElementById("root")
		);
	}
}