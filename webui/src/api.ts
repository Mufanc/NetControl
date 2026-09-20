export interface AppInfo {
    appid: number;
    label: string;
    packages: string[];
    system: boolean;
}

let disconnectHandler: () => void = () => undefined;

export function onDisconnect(handler: () => void): void {
    disconnectHandler = handler;
}

export async function request(
    path: string,
    options: RequestInit = {},
): Promise<Response> {
    const abort = new AbortController();
    const timeout = window.setTimeout(() => abort.abort(), 7000);

    let response: Response;

    try {
        response = await fetch(path, {
            ...options,
            cache: "no-store",
            referrerPolicy: "no-referrer",
            signal: abort.signal,
        });
    } catch {
        disconnectHandler();
        throw new Error("连接已断开");
    } finally {
        window.clearTimeout(timeout);
    }

    if (!response.ok) {
        let message = "操作失败";

        try {
            const body = (await response.json()) as { error?: string };
            message = body.error ?? message;
        } catch {
            // Responses without a JSON body keep the generic message.
        }

        throw new Error(message);
    }

    return response;
}
