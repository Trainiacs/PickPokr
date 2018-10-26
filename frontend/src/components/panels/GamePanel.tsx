import * as React from "react";

import {GameWord} from "../../modules";

interface Props {
	wordList: GameWord[];
}

interface State {

}

export class GamePanel extends React.Component<Props, State> {

	public render() {
		let wordList = this.props.wordList;
		return (
			<div className="game-panel panel">
				<div className="game-container">
					<div className={"word-list grid-columns-" + wordList.length}>
						{wordList.map((word: GameWord, wk: number) => (
							<div className="word grid-columns-1" key={wk}>
								<div className="question">?</div>
								{word.value.split("").map((char: string, ck: number) => (
									<div className={"char " + (ck === 0 ? "primary" : "secondary")} key={ck}>
										{/*char.toUpperCase()*/"-"}
									</div>
								))}
							</div>
						))}
					</div>
				</div>
			</div>
		);
	}
}