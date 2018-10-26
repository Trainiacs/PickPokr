import * as React from "react";
import {GamePanel} from "./GamePanel";

import {GameWord} from "../../modules";

interface Props {

}

type ViewMode = "find" | "add";

interface State {
	filterString: string;
	viewMode: ViewMode;
}

export class RootPanel extends React.Component<Props, State> {
	constructor(props: Props) {
		super(props);
		this.state = {
			filterString: "",
			viewMode: "find",
		};
	}

	render() {
		let viewMode = this.state.viewMode;
		let wordList: GameWord[] = [
			{question: "Kamp", value: "strid"},
			{value: "trav"},
			{value: "uggla"},
			{value: "grav"},
			{value: "agitatör"}
		];

		return (
			<div className="panel root-panel">
				<GamePanel wordList={wordList}/>
			</div>
		);
	}
}