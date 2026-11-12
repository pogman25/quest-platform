import { Router } from 'express';
import type { Pool } from 'pg';
import { z } from 'zod';
import { newToken, tokenHash } from '../auth/credentials';
import { questType } from '../games/gameSchema';
import { bearer, HttpError, uuid } from '../http';
import { markOpened, playerState, startRun, submitAnswer } from './runStore';

const attemptBody = z.object({
    attemptId: uuid,
    answer: z.union([
        z.string().min(1).max(512),
        z.array(z.string().min(1).max(100)).min(1).max(9),
        z.object({ word: z.string().min(1).max(32) }),
    ]),
});

export function playerRouter(pool: Pool) {
    const router = Router();

    router.post('/join', async (request, response) => {
        const { code } = z
            .object({
                code: z
                    .string()
                    .trim()
                    .regex(/^[0-9a-f]{16}$/i),
            })
            .parse(request.body);
        const token = newToken();
        const result = await pool.query<{ run_id: string; expires_at: Date }>(
            `
      INSERT INTO player_sessions (token_hash, run_id, expires_at)
      SELECT $1, id, now() + interval '7 days' FROM runs WHERE join_code = $2
      RETURNING run_id, expires_at
    `,
            [tokenHash(token), code.toUpperCase()],
        );
        const session = result.rows[0];
        if (!session) {
            throw new HttpError(404, 'RUN_NOT_FOUND', 'Код запуска не найден.');
        }
        response.status(201).json({
            token,
            expiresAt: session.expires_at,
            run: await playerState(pool, session.run_id),
        });
    });

    router.use('/:runId', async (request, response, next) => {
        const runId = uuid.parse(request.params.runId);
        const session = await pool.query(
            'SELECT 1 FROM player_sessions WHERE token_hash = $1 AND run_id = $2 AND expires_at > now()',
            [tokenHash(bearer(request)), runId],
        );
        if (!session.rowCount) {
            throw new HttpError(401, 'UNAUTHORIZED', 'Требуется токен игрока для этого запуска.');
        }
        response.locals.runId = runId;
        next();
    });

    router.get('/:runId', async (_request, response) => {
        response.json(await playerState(pool, response.locals.runId));
    });

    router.post('/:runId/start', async (_request, response) => {
        response.json(await startRun(pool, response.locals.runId));
    });

    router.post('/:runId/quests/:type/open', async (request, response) => {
        response.json(await markOpened(pool, response.locals.runId, questType.parse(request.params.type)));
    });

    router.post('/:runId/quests/:type/attempts', async (request, response) => {
        const { attemptId, answer } = attemptBody.parse(request.body);
        response.json(
            await submitAnswer(
                pool,
                response.locals.runId,
                questType.parse(request.params.type),
                attemptId,
                answer,
            ),
        );
    });

    return router;
}
