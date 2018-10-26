import {GameWord} from "./";

export class GameManager {
	private _words: GameWord[];
	private _answers: string[];

	constructor() {
		this._words = [];
		this._answers = [];
	}

	public get words(): GameWord[] {
		return this._words;
	}

	public get answers(): string[] {
		return this._answers;
	}

	public setWords(words: GameWord[]) {
		this._words = words;
		this._answers = this._words.map((w, wi) => this._answers[wi] || "");
	}

	public setAnswer(index: number, value: string) {
		this._answers[index] = value.toLowerCase().slice(0, this._words[index].value.length);
	}

	public resetAnswers() {
		this._answers = this._words.map(() =>  "");
	}

	public static importWords(words: {answerLength: number, question?: string}[]): GameWord[] {
		return words.map(w => ({
			value: "*".repeat(w.answerLength),
			question: w.question,
		}));
	}
}