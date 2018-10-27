export interface GameWord {
	question?: string;
	value: string;
}

export interface GamePlayer {
	name: string;
	ready: boolean;
}