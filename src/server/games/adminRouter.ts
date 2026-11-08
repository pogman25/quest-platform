import { Router } from 'express';
import type { Pool } from 'pg';
import { z } from 'zod';
import { organizerId } from '../auth/organizers';
import { organizerToken } from '../auth/webSession';
import { transaction } from '../db/database';
import { gameId, HttpError, uuid } from '../http';
import { readAdminBootstrap } from '../pages/adminBootstrap';
import { isPlayableQuest } from '../runs/playableQuest';
import { insertRun, listRuns, readProgress } from '../runs/runStore';
import { instantiateQuest } from './catalogStore';
import { gameSchema, questType } from './gameSchema';
import { insertGame, listGames, listGameSummaries, updateGame } from './gameStore';

const newRun = z.object({
    teamName: z.string().trim().min(1).max(80),
    code: z.string().regex(/^\d{1,6}$/),
});

export function adminRouter(pool: Pool) {
    const router = Router();

    router.use(async (request, response, next) => {
        response.locals.owner = await organizerId(pool, organizerToken(request));
        next();
    });

    router.get('/bootstrap', async (request, response) => {
        const path = z
            .string()
            .max(256)
            .parse(request.query.path ?? '/');
        response.json(await readAdminBootstrap(pool, response.locals.owner, path));
    });

    router.post('/catalog/:type/instantiate', async (request, response) => {
        response.json(await instantiateQuest(pool, questType.parse(request.params.type)));
    });

    router.get('/games', async (request, response) => {
        const { owner } = response.locals;
        const games =
            request.query.view === 'summary'
                ? await listGameSummaries(pool, owner)
                : await listGames(pool, owner);
        response.json({ games });
    });

    router.get('/games/:gameId', async (request, response) => {
        const id = gameId.parse(request.params.gameId);
        const [game] = await listGames(pool, response.locals.owner, id);
        if (!game) {
            throw new HttpError(404, 'GAME_NOT_FOUND', 'Игра не найдена.');
        }
        response.json(game);
    });

    router.post('/games', async (request, response) => {
        const game = gameSchema.parse(request.body);
        const created = await transaction(pool, (db) => insertGame(db, response.locals.owner, game));
        if (!created) {
            throw new HttpError(409, 'GAME_EXISTS', 'Игра с таким идентификатором уже существует.');
        }
        response.status(201).json(game);
    });

    router.post('/games/import', async (request, response) => {
        const { games } = z.object({ games: z.array(gameSchema).max(100) }).parse(request.body);
        const owner = response.locals.owner as string;
        const imported = await transaction(pool, async (db) => {
            let count = 0;
            for (const game of games) {
                if (await insertGame(db, owner, game)) {
                    count++;
                }
            }
            return count;
        });
        response.json({ imported });
    });

    router.put('/games/:gameId', async (request, response) => {
        const game = gameSchema.parse(request.body);
        if (game.id !== gameId.parse(request.params.gameId)) {
            throw new HttpError(400, 'GAME_ID_MISMATCH', 'Идентификатор в адресе отличается от данных игры.');
        }
        await updateGame(pool, response.locals.owner, game);
        response.json(game);
    });

    router.patch('/games/:gameId', async (request, response) => {
        const id = gameId.parse(request.params.gameId);
        const { title } = z.object({ title: z.string().trim().min(1).max(80) }).parse(request.body);
        const updated = await pool.query('UPDATE games SET title = $3 WHERE id = $1 AND organizer_id = $2', [
            id,
            response.locals.owner,
            title,
        ]);
        if (!updated.rowCount) {
            throw new HttpError(404, 'GAME_NOT_FOUND', 'Игра не найдена.');
        }
        response.json({ title });
    });

    router.delete('/games/:gameId', async (request, response) => {
        const deleted = await pool.query('DELETE FROM games WHERE id = $1 AND organizer_id = $2', [
            gameId.parse(request.params.gameId),
            response.locals.owner,
        ]);
        if (!deleted.rowCount) {
            throw new HttpError(404, 'GAME_NOT_FOUND', 'Игра не найдена.');
        }
        response.sendStatus(204);
    });

    router.get('/games/:gameId/runs', async (request, response) => {
        response.json({
            runs: await listRuns(pool, response.locals.owner, gameId.parse(request.params.gameId)),
        });
    });

    router.post('/games/:gameId/runs', async (request, response) => {
        const id = gameId.parse(request.params.gameId);
        const { teamName, code } = newRun.parse(request.body);
        const owner = response.locals.owner as string;
        const run = await transaction(pool, async (db) => {
            await db.query('SELECT id FROM games WHERE id = $1 AND organizer_id = $2 FOR SHARE', [id, owner]);
            const [game] = await listGames(db, owner, id);
            if (!game) {
                throw new HttpError(404, 'GAME_NOT_FOUND', 'Игра не найдена.');
            }
            if (code.length !== game.order.length) {
                throw new HttpError(400, 'CODE_LENGTH', 'Для каждого квеста нужна одна цифра кода.');
            }
            for (const type of game.order) {
                const quest = game.quests[type];
                if (!quest || !isPlayableQuest(quest)) {
                    throw new HttpError(400, 'QUEST_NOT_READY', `Проверьте настройки квеста ${type}.`);
                }
            }
            return insertRun(db, game, teamName, code);
        });
        response.status(201).json(run);
    });

    router.get('/games/:gameId/runs/:runId', async (request, response) => {
        response.json(
            await readProgress(
                pool,
                response.locals.owner,
                gameId.parse(request.params.gameId),
                uuid.parse(request.params.runId),
            ),
        );
    });

    return router;
}
