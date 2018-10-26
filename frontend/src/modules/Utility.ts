export class Utility {
	public static generateId(prefix: string, padding: string) {
		let id = Date.now();
		let preHash = prefix + id + padding;
		return prefix + btoa(preHash);
	}
}