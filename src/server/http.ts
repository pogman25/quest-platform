import type { ErrorRequestHandler, Request } from 'express';
import { ZodError, z } from 'zod';

export class HttpError extends Error {
    constructor(
        readonly status: number,
        readonly code: string,
        message: string,
    ) {
        super(message);
    }
}

export const uuid = z.uuid();
export const gameId = z
    .string()
    .regex(/^[a-z0-9][a-z0-9-]{0,79}$/)
    .refine((id) => !['login', 'api', 'assets', 'play'].includes(id));

export function bearer(request: Request) {
    const value = /^Bearer ([A-Za-z0-9_-]+)$/.exec(request.get('authorization') ?? '');
    if (!value) {
        throw new HttpError(401, 'UNAUTHORIZED', 'Требуется токен доступа.');
    }
    return value[1];
}

export const handleError: ErrorRequestHandler = (error: unknown, _request, response, next) => {
    if (response.headersSent) {
        next(error);
        return;
    }
    if (error instanceof HttpError) {
        response.status(error.status).json({ error: { code: error.code, message: error.message } });
        return;
    }
    if (error instanceof ZodError) {
        const issues = error.issues.map(({ path, message }) => ({ path, message }));
        response
            .status(400)
            .json({ error: { code: 'INVALID_INPUT', message: 'Проверьте параметры запроса.', issues } });
        return;
    }
    if (error instanceof SyntaxError && 'status' in error && error.status === 400) {
        response.status(400).json({ error: { code: 'INVALID_JSON', message: 'Некорректный JSON.' } });
        return;
    }
    if (error instanceof Error && 'status' in error && error.status === 413) {
        response
            .status(413)
            .json({ error: { code: 'PAYLOAD_TOO_LARGE', message: 'Слишком большой запрос.' } });
        return;
    }
    console.error(error);
    response.status(500).json({ error: { code: 'INTERNAL_ERROR', message: 'Ошибка сервера.' } });
};
