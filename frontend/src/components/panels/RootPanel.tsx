import * as React from "react";
import Button from "@material-ui/core/Button";

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
				<Button variant="contained" color="primary">
					Hello World!
				</Button>
			</div>
		);
	}
}