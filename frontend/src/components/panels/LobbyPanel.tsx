import * as React from "react";

import {GamePlayer} from "../../modules";

interface Props {
	playerList: GamePlayer[];
	playerSelf: GamePlayer;
	onReadyStateChanged?: (ready: boolean) => void;
	onClose?: () => void;
}

interface State {

}

export class LobbyPanel extends React.Component<Props, State> {
	public render() {
		let playerList: GamePlayer[] = this.props.playerList.sort((a, b) => (a.ready ? 0 : 1) - (b.ready ? 0 : 1));
		let playerSelf: GamePlayer = this.props.playerSelf;
		let onClose = this.props.onClose;
		return (
			<div className="lobby-panel panel">
				<div className="player-self">
					<div className="name">{playerSelf.name}</div>
					<div className={"status" + (playerSelf.ready ? " ready": " waiting")} onClick={() => this._onReadyStateChanged(!playerSelf.ready)}/>
				</div>
				<div className="player-list">
					{playerList.map((player, pk) => (
						<div className="player" key={pk}>
							<div className="name">{player.name}</div>
							<div className={"status" + (player.ready ? " ready": " waiting")} />
						</div>
					))}
					{onClose ? (
						<button onClick={onClose}>Back to game</button>
					) : (null)}
				</div>
			</div>
		);
	}

	private _onReadyStateChanged(ready: boolean) {
		console.log("Ready state: ", ready);
		if (this.props.onReadyStateChanged) this.props.onReadyStateChanged(ready);
	}
}