import * as React from "react";

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

		return (
			<div className="panel root-panel">

			</div>
		);
	}
}