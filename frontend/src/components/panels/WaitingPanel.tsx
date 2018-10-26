import * as React from "react";

interface Props {
	message: string;
}

interface State {};

export class WaitingPanel extends React.Component<Props, State> {

	public render() {
		let message = this.props.message;
		return (
			<div className="waiting-panel panel">
				<div className="waiting">
					<div className="message">{message}</div>
				</div>
			</div>
		);
	}
}