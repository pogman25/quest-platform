import type { Pool, PoolClient } from 'pg';
import type { GameSummary } from '../../shared/catalog';
import type { Game, Quest } from '../../shared/quests';
import { transaction } from '../db/database';
import { HttpError } from '../http';

type GameRow = { id: string; title: string };
type QuestRow = { game_id: string; position: number | null; config: Quest };

export async function listGameSummaries(pool: Pool, organizerId: string) {
    const result = await pool.query<GameSummary>(
        `
    SELECT g.id, g.title, count(q.position)::int AS "questCount"
    FROM games g LEFT JOIN game_quests q ON q.game_id = g.id
    WHERE g.organizer_id = $1
    GROUP BY g.id ORDER BY g.created_at, g.id
  `,
        [organizerId],
    );
    return result.rows;
}

export async function listGames(db: Pool | PoolClient, organizerId: string, gameId?: string) {
    const result = await db.query<
        GameRow & {
            quests: QuestRow[];
        }
    >(
        `
    SELECT g.id, g.title, COALESCE(
      jsonb_agg(to_jsonb(q) ORDER BY q.position) FILTER (WHERE q.type IS NOT NULL), '[]'
    ) AS quests
    FROM games g LEFT JOIN game_quests q ON q.game_id = g.id
    WHERE g.organizer_id = $1 AND ($2::text IS NULL OR g.id = $2)
    GROUP BY g.id ORDER BY g.created_at, g.id
  `,
        [organizerId, gameId ?? null],
    );

    return result.rows.map(({ id, title, quests }): Game => ({
        id,
        title,
        order: quests.filter(({ position }) => position !== null).map(({ config }) => config.type),
        quests: Object.fromEntries(quests.map(({ config }) => [config.type, config])),
    }));
}

async function saveQuests(client: PoolClient, game: Game) {
    await client.query('DELETE FROM game_quests WHERE game_id = $1', [game.id]);
    for (const config of Object.values(game.quests)) {
        const index = game.order.indexOf(config.type);
        await client.query('INSERT INTO game_quests VALUES ($1, $2, $3, $4)', [
            game.id,
            config.type,
            index < 0 ? null : index + 1,
            JSON.stringify(config),
        ]);
    }
}

export async function insertGame(client: PoolClient, organizerId: string, game: Game) {
    const result = await client.query(
        'INSERT INTO games (id, organizer_id, title) VALUES ($1, $2, $3) ON CONFLICT (id) DO NOTHING',
        [game.id, organizerId, game.title],
    );
    if (result.rowCount) {
        await saveQuests(client, game);
    }
    return Boolean(result.rowCount);
}

export async function updateGame(pool: Pool, organizerId: string, game: Game) {
    await transaction(pool, async (client) => {
        const result = await client.query('UPDATE games SET title = $3 WHERE id = $1 AND organizer_id = $2', [
            game.id,
            organizerId,
            game.title,
        ]);
        if (!result.rowCount) {
            throw new HttpError(404, 'GAME_NOT_FOUND', 'Игра не найдена.');
        }
        await saveQuests(client, game);
    });
}
