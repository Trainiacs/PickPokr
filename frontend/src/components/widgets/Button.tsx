import * as React from "react";

interface Props {
	caption: string;
}

const Button: React.SFC<Props> = (props: Props) => {
	let caption = this.props.caption;
	let type = this.props.type || "basic";
	let className = [
		"button",
		"type-" + type,
	].join(" ");
	return (
		<div className="button">{caption}</div>
	)
}

export {Button}