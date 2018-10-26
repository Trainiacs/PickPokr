import * as React from "react";
import {GamePanel} from "./GamePanel";
import {AnswerPanel} from "./AnswerPanel";

import {GameWord} from "../../modules";

interface Props {

}

interface State {
	currentWordIndex?: number;
	answerList: string[];
}

export class RootPanel extends React.Component<Props, State> {
	constructor(props: Props) {
		super(props);
		this.state = {
			currentWordIndex: 0,
			answerList: [],
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
		let answerList: string[] = this.state.answerList;
		let currentPanel = currentWordIndex === undefined ? "game" : "answer";

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
						default: return (
							<div />
						);
					}
				})(currentPanel)}
				
			</div>
		);
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