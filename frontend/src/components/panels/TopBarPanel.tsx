import * as React from "react";

interface Props {
	items: string[];
	onItemClicked(item: string): void;
}

interface State {}

export class TopBarPanel extends React.Component<Props, State> {
	public render() {
		let items = this.props.items;
		let onItemClicked = this.props.onItemClicked;
		return (
			<div className="top-bar-panel panel">
				{items.map((item, index) => (
					<div className={item + " item"} key={index} onClick={() => onItemClicked(item)}/>
				))}
			</div>
		);
	}
}