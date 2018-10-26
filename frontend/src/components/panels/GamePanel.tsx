import * as React from "react";

import {GameWord} from "../../modules";

interface Props {
	wordList: GameWord[];
	answerList: string[];
	onWordSelected(word: GameWord, wordIndex: number): void;
}

interface State {

}

export class GamePanel extends React.Component<Props, State> {

	public render() {
		let wordList = this.props.wordList;
		let answerList = this.props.answerList;
		let completeKey = answerList.every(a => a[0] !== undefined);
		return (
			<div className="game-panel panel">
				<div className="game-container">
					<div className={"word-list grid-columns-" + wordList.length}>
						{wordList.map((word: GameWord, wk: number) => (
							<div className={"word grid-columns-1 " + (word.value.toLowerCase() === answerList[wk].toLowerCase() ? "correct" : "")} key={wk}>
								<div className={"question " + (word.question === undefined ? "not-in-collection" : "")} style={{animationDelay: (0.05 * wk) + "s"}} onClick={() => this._onWordSelected(word, wk)}>?</div>
								{word.value.split("").map((char: string, ck: number) => (
									<div className={"char" + (ck === 0 ? " primary" : " secondary") + (completeKey ? " complete" : "")} style={{animationDelay: (0.05 * wk) + "s"}} key={ck}>
										{(answerList[wk][ck] || "-").toUpperCase()}
									</div>
								))}
							</div>
						))}
					</div>
				</div>
			</div>
		);
	}

	private _onWordSelected(word: GameWord, wordIndex: number) {
		let onWordSelected = this.props.onWordSelected;
		onWordSelected(word, wordIndex);
	}
}