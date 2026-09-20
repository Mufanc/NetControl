import { request } from "./api";

const cache = new Map<number, string>();
const pending = new Map<number, Promise<string>>();
const queue: Array<() => Promise<void>> = [];

let running = 0;

export function loadIcon(appId: number): Promise<string> {
    const cached = cache.get(appId);

    if (cached !== undefined) return Promise.resolve(cached);

    const existing = pending.get(appId);

    if (existing !== undefined) return existing;

    const result = new Promise<string>((resolve, reject) => {
        queue.push(async () => {
            try {
                const response = await request(`api/apps/${appId}/icon`);
                const url = URL.createObjectURL(await response.blob());

                cache.set(appId, url);
                resolve(url);
            } catch (err) {
                reject(err);
            } finally {
                pending.delete(appId);
            }
        });

        pump();
    });

    pending.set(appId, result);
    return result;
}

export function clearIcons(): void {
    cache.forEach(URL.revokeObjectURL);
    cache.clear();
}

function pump(): void {
    while (running < 2 && queue.length > 0) {
        const task = queue.shift()!;

        running++;
        task().finally(() => {
            running--;
            pump();
        });
    }
}
