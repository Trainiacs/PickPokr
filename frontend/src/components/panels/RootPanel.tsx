import * as React from "react";
import {GamePanel} from "./GamePanel";
import {AnswerPanel} from "./AnswerPanel";
import {LobbyPanel} from "./LobbyPanel";
import {InfoPanel} from "./InfoPanel";
import {TopBarPanel} from "./TopBarPanel";

import {
	GameWord,
	GamePlayer,
	GameManager,
	LobbyManager,
} from "../../modules";

type Props = {
	route: string;
	message: string;
	gameManager: GameManager;
	lobbyManager: LobbyManager;
	playerSelf: GamePlayer;
	exchangePin: string;
	onEvent(ev: {type: string, meta: any}): void;
}

interface State {
	currentWordIndex?: number;
}

export class RootPanel extends React.Component<Props, State> {
	constructor(props: Props) {
		super(props);
		this.state = {
		};
	}

	render() {
		let currentWordIndex = this.state.currentWordIndex;
		let wordList = this.props.gameManager.words;
		let answerList = this.props.gameManager.answers;
		let exchangePin = this.props.exchangePin;

		let playerList = this.props.lobbyManager.players;
		let playerSelf = this.props.playerSelf;
		let route = this.props.route;
		let message = this.props.message;

		return (
			<div className="panel root-panel">
				{route === "game" ? (
					<TopBarPanel items={["share-all", "show-players"]} onItemClicked={this._onItemClicked.bind(this)}/>
				) : null}
				{((panel: string) => {
					switch(panel) {
						case "game": return currentWordIndex === undefined ? (
							<GamePanel 
								wordList={wordList}
								answerList={wordList.map((w: GameWord, i: number) => answerList[i] || "")}
								onWordSelected={this._onWordSelected.bind(this)}
							/>
						) : (
							<AnswerPanel 
								word={wordList[currentWordIndex]}
								value={answerList[currentWordIndex] || ""}
								onValueChange={(value: string) => this._onWordValueChanged(currentWordIndex, value)}
								onAnswerSubmit={(value: string) => this._onAnswerSubmit(currentWordIndex, value)}
							/>
						);
						case "lobby": return (
							<LobbyPanel
								playerList={playerList}
								playerSelf={playerSelf}
								onReadyStateChanged={this._onPlayerReadyStateChanged.bind(this)}
							/>
						);
						case "showPlayers": return (
							<LobbyPanel
								playerList={playerList}
								playerSelf={playerSelf}
								onClose={() => this._triggerEvent("setRoute", "game")}
							/>
						);
						case "waiting": return (
							<InfoPanel
								type="waiting"
								message={message}
							/>
						);
						case "showPin": return (
							<InfoPanel
								type="pin"
								message={"PIN: " + exchangePin}
								onConfirm={() => this._triggerEvent("setRoute", "game")}
							/>
						);
						default: return (
							<div />
						);
					}
				})(route)}
				
			</div>
		);
	}

	private _onItemClicked(item: string) {
		console.log(item);
		switch(item) {
			case "show-players": {
				this._triggerEvent("setRoute", "showPlayers");
				break;
			}
			case "share-all": {
				this._triggerEvent("shareAll", null);
				break;
			}
		}
	}

	private _triggerEvent(type: string, meta: any) {
		let onEvent = this.props.onEvent;
		onEvent({type: type, meta: meta});
	}

	private _onPlayerReadyStateChanged(ready: boolean) {
		//let playerSelf: GamePlayer = Object.assign({}, this.state.playerSelf);
		//playerSelf.ready = ready;
		//this.setState({playerSelf: playerSelf});
	}

	private _onWordSelected(word: GameWord, wordIndex: number) {
		this.setState({currentWordIndex: wordIndex});
	}

	private _onWordValueChanged(wordIndex: number, value: string) {
		this.props.gameManager.setAnswer(wordIndex, value);
		this.forceUpdate();
	}

	private _onAnswerSubmit(wordIndex: number, value: string) {
		this._onWordValueChanged(wordIndex, value);
		this.setState({currentWordIndex: undefined});
	}
}