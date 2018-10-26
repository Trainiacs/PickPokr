import {GamePlayer} from "./";

export class LobbyManager {
	private _players: GamePlayer[];

	constructor() {
		this._players = [];
	}

	public get players() {
		return this._players;
	}

	public updatePlayer(player: GamePlayer) {
		this._players = this._players.map(p => player.name === p.name ? player : p);
	}

	public setPlayers(players: GamePlayer[]) {
		this._players = players;
	}

	public static importPlayers(players: string[]): GamePlayer[] {
		return players.map(name => ({
			name: name,
			ready: true,
		}));
	}
}