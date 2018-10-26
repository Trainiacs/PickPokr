import * as React from "react";

import {GameWord} from "../../modules";

interface Props {
	word: GameWord;
	value: string;
	onValueChange(value: string): void;
	onAnswerSubmit(value: string): void;
}

interface State {

}

export class AnswerPanel extends React.Component<Props, State> {
	public render() {
		let word = this.props.word;
		let value = this.props.value;
		let onAnswerSubmit = this.props.onAnswerSubmit;

		return (
			<div className="answer-panel panel">
				<div className="question">
					<div className="content">
						{word.question === undefined ? "???" : word.question}
					</div>
				</div>
				<div className="answer">
					<input type="text" value={value.toUpperCase()} maxLength={word.value.length} onChange={this._onAnswerChange.bind(this)} />
					<button onClick={() => onAnswerSubmit(value)}>Answer</button>
				</div>
			</div>
		);
	}

	private _onAnswerChange(event: any) {
		let onValueChange = this.props.onValueChange;
		let word = this.props.word;
		let value = event.target.value.toUpperCase().substring(0, word.value.length);
		onValueChange(value);
	}
}