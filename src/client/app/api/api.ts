import { messages } from '../messages/ru';
type Options = {
    method?: string;
    body?: unknown;
    signal?: AbortSignal;
    token?: string;
};

export class ApiError extends Error {
    constructor(
        readonly status: number,
        message: string,
    ) {
        super(message);
    }
}

export async function api<T>(path: string, { method, body, signal, token }: Options = {}): Promise<T> {
    const headers = new Headers({ Accept: 'application/json', 'X-Quest-Request': '1' });
    if (body !== undefined) {
        headers.set('Content-Type', 'application/json');
    }
    if (token) {
        headers.set('Authorization', `Bearer ${token}`);
    }

    const response = await fetch(`/api/v1${path}`, { method, headers, body: JSON.stringify(body), signal });
    if (response.status === 204) {
        return undefined as T;
    }
    if (!response.headers.get('content-type')?.includes('application/json')) {
        throw new ApiError(response.status, messages['app.apiUnavailable']);
    }
    const data = await response.json();
    if (!response.ok) {
        throw new ApiError(response.status, data.error?.message ?? messages['app.requestRejected']);
    }
    return data as T;
}
