import type { Request } from 'express';
import { bearer, HttpError } from '../http';

export const organizerCookie = 'quest_organizer';

export function browserToken(request: Request) {
    return /(?:^|;\s*)quest_organizer=([A-Za-z0-9_-]+)(?:;|$)/.exec(request.get('cookie') ?? '')?.[1];
}

export function requireWebRequest(request: Request) {
    if (request.get('X-Quest-Request') !== '1') {
        throw new HttpError(403, 'WEB_REQUEST_REQUIRED', 'Запрос должен быть отправлен из админки.');
    }
}

export function organizerToken(request: Request) {
    if (request.get('authorization')) {
        return bearer(request);
    }
    const token = browserToken(request);
    if (!token) {
        throw new HttpError(401, 'UNAUTHORIZED', 'Требуется сессия организатора.');
    }
    if (!['GET', 'HEAD', 'OPTIONS'].includes(request.method)) {
        requireWebRequest(request);
    }
    return token;
}

export function cookieOptions(request: Request) {
    return {
        httpOnly: true,
        sameSite: 'lax' as const,
        secure: request.secure || process.env.COOKIE_SECURE === 'true',
        path: '/',
    };
}
