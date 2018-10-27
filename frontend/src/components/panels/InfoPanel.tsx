import * as React from "react";

interface Props {
	message: string;
	type: string;
	onConfirm?: () => void;
}

interface State {};

export class InfoPanel extends React.Component<Props, State> {

	public render() {
		let type = this.props.type;
		let message = this.props.message;
		let onConfirm = this.props.onConfirm;
		return (
			<div className={"info-panel panel " + type}>
				<div className={"info " + type}>
					<div className="message">{message}</div>
					{onConfirm ? (
						<button onClick={() => onConfirm()}>Ok</button>
					) : (null)}
				</div>
			</div>
		);
	}
}