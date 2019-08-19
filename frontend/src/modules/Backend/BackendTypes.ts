export interface Backend<I, O> {
	connect(nick: string): void;
	send<T extends keyof I>(message: {type: T, payload: I[T]}): void;
	listen<T extends keyof O>(type: T, callback: (message: {type: T, payload: O[T]}) => void): void;
}

export type InputEvents = {
	guess: string;
	leave: {};
	exchangePinRequest: {};
	exchangeCommit: string;
}

export type OutputEvents = {
	error: {
		message: string;
	};
	denied: {
		message: string;
	};
	exchangePin: string;
	roaster: string[];
	challenge: {
		index?: number;
		answerLength: number;
		question?: string;
	}[];
	winner: {
		nick: string;
		keyword: string;
	};
	badGuess: {
		nick: string;
		guess: string;
	};
};

export type EventBackend = Backend<InputEvents, OutputEvents>;