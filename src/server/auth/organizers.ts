import { Router } from 'express';
import type { Pool } from 'pg';
import { z } from 'zod';
import { HttpError } from '../http';
import { newToken, passwordMatches, tokenHash } from './credentials';
import { cookieOptions, organizerCookie, organizerToken, requireWebRequest } from './webSession';

const loginBody = z.object({
    email: z.email().max(254),
    password: z.string().min(1).max(256),
    web: z.boolean().optional(),
});

export async function findOrganizerId(pool: Pool, token: string) {
    const result = await pool.query<{ organizer_id: string }>(
        'SELECT organizer_id FROM organizer_sessions WHERE token_hash = $1 AND expires_at > now()',
        [tokenHash(token)],
    );
    return result.rows[0]?.organizer_id;
}

export async function organizerId(pool: Pool, token: string) {
    const id = await findOrganizerId(pool, token);
    if (!id) {
        throw new HttpError(401, 'UNAUTHORIZED', 'Сессия организатора истекла.');
    }
    return id;
}

export function organizerAuth(pool: Pool) {
    const router = Router();

    router.post('/login', async (request, response) => {
        const { email, password, web } = loginBody.parse(request.body);
        if (web) {
            requireWebRequest(request);
        }
        const result = await pool.query<{ id: string; password_hash: string }>(
            'SELECT id, password_hash FROM organizers WHERE email = $1',
            [email.toLowerCase()],
        );
        const organizer = result.rows[0];
        if (!organizer || !(await passwordMatches(password, organizer.password_hash))) {
            throw new HttpError(401, 'INVALID_CREDENTIALS', 'Неверная почта или пароль.');
        }
        const token = newToken();
        const session = await pool.query<{ expires_at: Date }>(
            "INSERT INTO organizer_sessions VALUES ($1, $2, now() + interval '12 hours') RETURNING expires_at",
            [tokenHash(token), organizer.id],
        );
        const expiresAt = session.rows[0].expires_at;
        if (web) {
            response.cookie(organizerCookie, token, { ...cookieOptions(request), expires: expiresAt });
            response.sendStatus(204);
            return;
        }
        response.json({ token, expiresAt });
    });

    router.post('/logout', async (request, response) => {
        await pool.query('DELETE FROM organizer_sessions WHERE token_hash = $1', [
            tokenHash(organizerToken(request)),
        ]);
        response.clearCookie(organizerCookie, cookieOptions(request));
        response.sendStatus(204);
    });

    return router;
}
