import {
    InputEvents,
    OutputEvents,
    EventBackend,
} from "./BackendTypes";

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