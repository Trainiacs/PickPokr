import * as React from "react";

interface Props {

}

interface State {}

export class TopBarPanel extends React.Component<Props, State> {
	public render() {
		return (
			<div className="top-bar-panel panel">
				<div className="share-all" />
				<div className="exit-game" />
			</div>
		);
	}
}