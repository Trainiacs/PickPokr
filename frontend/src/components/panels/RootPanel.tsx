import * as React from "react";
import {GamePanel} from "./GamePanel";
import {AnswerPanel} from "./AnswerPanel";
import {LobbyPanel} from "./LobbyPanel";

import {
	GameWord,
	GamePlayer,
} from "../../modules";

interface Props {

}

interface State {
	currentWordIndex?: number;
	answerList: string[];
	playerSelf: GamePlayer;
}

export class RootPanel extends React.Component<Props, State> {
	constructor(props: Props) {
		super(props);
		this.state = {
			currentWordIndex: 0,
			answerList: [],
			playerSelf: {
				name: "Mattias",
				ready: false,
			}
		};
	}

	render() {
		let currentWordIndex = this.state.currentWordIndex;

		let wordList: GameWord[] = [
			{question: "Kampen om en sak som gjorde något och det var roligt när det blev knasigt för det var inte som det brukar eller hur?", value: "strid"},
			{value: "trav"},
			{value: "uggla"},
			{value: "grav"},
			{value: "agitatör"}
		];
		let playerList: GamePlayer[] = [
			{name: "Dirk", ready: true},
			{name: "McNeil", ready: true},
			{name: "Stuge", ready: true},
			{name: "Elminster", ready: true},
			{name: "Woody", ready: false},
		];
		let playerSelf = this.state.playerSelf;
		let answerList: string[] = this.state.answerList;
		let currentPanel = "lobby";//currentWordIndex === undefined ? "game" : "answer";

		return (
			<div className="panel root-panel">
				{((panel: string) => {
					switch(panel) {
						case "game": return (
							<GamePanel 
								wordList={wordList}
								answerList={wordList.map((w: GameWord, i: number) => answerList[i] || "")}
								onWordSelected={this._onWordSelected.bind(this)}
							/>
						);
						case "answer": return (
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
						default: return (
							<div />
						);
					}
				})(currentPanel)}
				
			</div>
		);
	}

	private _onPlayerReadyStateChanged(ready: boolean) {
		let playerSelf: GamePlayer = Object.assign({}, this.state.playerSelf);
		playerSelf.ready = ready;
		this.setState({playerSelf: playerSelf});
	}

	private _onWordSelected(word: GameWord, wordIndex: number) {
		this.setState({currentWordIndex: wordIndex});
	}

	private _onWordValueChanged(wordIndex: number, value: string) {
		let answerList = this.state.answerList.slice(0, this.state.answerList.length);
		answerList[wordIndex] = value;
		this.setState({answerList: answerList});
	}

	private _onAnswerSubmit(wordIndex: number, value: string) {
		this._onWordValueChanged(wordIndex, value);
		this.setState({currentWordIndex: undefined});
	}
}