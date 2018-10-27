import { Socket } from "net";

export interface Backend<I, O> {
	connect(nick: string): void;
	send<T extends keyof I>(message: {type: T, payload: I[T]}): void;
	listen<T extends keyof O>(type: T, callback: (message: {type: T, payload: O[T]}) => void): void;
}

type InputEvents = {
	guess: string;
	leave: {};
	exchangePinRequest: {};
	exchangeCommit: string;
}

type OutputEvents = {
	error: {
		message: string;
	};
	denied: {
		message: string;
	};
	exchangePin: string;
	roaster: string[];
	challenge: {
		index?: number;
		answerLength: number;
		question?: string;
	}[];
	winner: {
		nick: string;
		keyword: string;
	};
	badGuess: {
		nick: string;
		guess: string;
	};
};

export type EventBackend = Backend<InputEvents, OutputEvents>;

export class MockBackend implements EventBackend {
	private _players: string[];
	private _words: {
		answerLength: number;
		question?: string;
	}[];

	private _messageListeners: {[x: string]: (ev: {type: keyof OutputEvents, payload: OutputEvents[keyof OutputEvents]}) => void};

	constructor(data: string) {
		this._players = [
			{name: "Dirk", ready: true},
			{name: "McNeil", ready: true},
			{name: "Stuge", ready: true},
			{name: "Elminster", ready: true},
			{name: "Woody", ready: false},
		].map(w => w.name);
		this._words = [
			{question: "Kampen om en sak som gjorde något och det var roligt när det blev knasigt för det var inte som det brukar eller hur?", answerLength: 5},
			{answerLength: 7},
			{answerLength: 4},
			{answerLength: 5},
			{answerLength: 9}
		];
		this._messageListeners = {};
	}

	public connect(nick: string) {
		if (this._players.indexOf(nick) === -1) this._players.push(nick);
		setTimeout(() => {
			this._triggerMessage({type: "roaster", payload: this._players});
		}, 500);
		setTimeout(() => {
			this._triggerMessage({type: "challenge", payload: this._words});
		}, 2000)
	}

	public send<T extends keyof InputEvents>(message: {type: T, payload: InputEvents[T]}) {
		switch(message.type) {
			case "exchangePinRequest": {
				setTimeout(() => {
					this._triggerMessage({type: "exchangePin", payload: "1234"});
				}, 500);
				break;
			}
			case "guess": {
				setTimeout(() => {
					if (Math.random() > 0.5) {
						this._triggerMessage({type: "badGuess", payload: {
							nick: "Dirk",
							guess: message.payload.toString(),
						}});
					} else {
						this._triggerMessage({type: "winner", payload: {
							nick: "Dirk",
							keyword: "svaret",
						}});
					}
				}, 500);
				break;
			}
		}
	}

	public listen<T extends keyof OutputEvents>(type: T, callback: (message: {type: T, payload: OutputEvents[T]}) => void) {
		this._messageListeners[type] = callback;
	}

	private _triggerMessage(ev: {type: keyof OutputEvents, payload: OutputEvents[keyof OutputEvents]}) {
		if (this._messageListeners[ev.type as string] !== undefined) {
			this._messageListeners[ev.type as string](ev);
		}
	}
}

export class WebSocketBackend implements EventBackend {
	private _socket?: WebSocket;
	private _host: string;
	private _messageListeners: {[x: string]: (ev: {type: keyof OutputEvents, payload: OutputEvents[keyof OutputEvents]}) => void};

	constructor(host: string) {
		this._host = host;
		this._messageListeners = {};
	}

	public connect(nick: string) {
		this._createSocket(this._host + "/" + nick);
	}

	public send<T extends keyof InputEvents>(message: {type: T, payload: InputEvents[T]}) {
		this._socket!.send(JSON.stringify(message));
	}

	public listen<T extends keyof OutputEvents>(type: T, callback: (message: {type: T, payload: OutputEvents[T]}) => void) {
		this._messageListeners[type] = callback;
	}

	private _createSocket(host: string) {
		try {
			if (this._socket !== undefined) this._socket.close();
			this._socket = new WebSocket(host);
			this._socket.addEventListener("message", this._onMessage.bind(this));
			this._socket.addEventListener("error", this._onError.bind(this));
		} catch (e) {
			this._triggerMessage({
				type: "error",
				payload: {
					message: "Unable to create socket",
				}
			});
		}
	}

	private _onError(ev: MessageEvent) {
		this._triggerMessage({
			type: "error",
			payload: {
				message: "Unknown socket error"
			}
		});
	}

	private _onMessage(ev: MessageEvent) {
		let data: {type: keyof OutputEvents, payload: OutputEvents[keyof OutputEvents]} = {
			type: "error", 
			payload: {
				message: "Unknown error"
			}
		};
		try {
			data = JSON.parse(ev.data);
		} catch (e) {
			console.log(ev.data);
			data = {
				type: "error", 
				payload: {
					message: "Unable to parse json",
				}
			};
		}
		console.log(ev);
		this._triggerMessage(data);
	}

	private _triggerMessage(ev: {type: keyof OutputEvents, payload: OutputEvents[keyof OutputEvents]}) {
		if (this._messageListeners[ev.type as string] !== undefined) {
			this._messageListeners[ev.type as string](ev);
		} else {
			console.log(ev);
		}
	}
}