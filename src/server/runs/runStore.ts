import { randomBytes, randomUUID } from 'node:crypto';
import type { Pool, PoolClient } from 'pg';
import type { Game, Quest, QuestType } from '../../shared/quests';
import type { AttemptResult, PlayerQuest } from '../../shared/runs';
import { transaction } from '../db/database';
import { HttpError } from '../http';
import { evaluateAnswer } from './answers';
import type { Answer } from './answers';

type Queryable = Pool | PoolClient;
type RunRow = {
    id: string;
    game_id: string;
    team_name: string;
    join_code: string;
    created_at: Date;
    finished_at: Date | null;
};
type RunQuest = {
    type: QuestType;
    position: number;
    config: Quest;
    reward_digit: string;
    started_at: Date | null;
    solved_at: Date | null;
};
type StoredResult = AttemptResult & { consumed: boolean };

function summarize(run: RunRow) {
    return {
        id: run.id,
        teamName: run.team_name,
        joinCode: run.join_code,
        createdAt: run.created_at,
        finishedAt: run.finished_at,
    };
}

async function readRun(db: Queryable, id: string, lock = false) {
    const result = await db.query<RunRow>(`SELECT * FROM runs WHERE id = $1 ${lock ? 'FOR UPDATE' : ''}`, [
        id,
    ]);
    if (!result.rows[0]) {
        throw new HttpError(404, 'RUN_NOT_FOUND', 'Запуск не найден.');
    }
    return result.rows[0];
}

export async function insertRun(db: PoolClient, game: Game, teamName: string, code: string) {
    const result = await db.query<RunRow>(
        'INSERT INTO runs (id, game_id, join_code, team_name) VALUES ($1, $2, $3, $4) RETURNING *',
        [randomUUID(), game.id, randomBytes(8).toString('hex').toUpperCase(), teamName],
    );
    const run = result.rows[0];
    for (const [index, type] of game.order.entries()) {
        await db.query(
            'INSERT INTO run_quests (run_id, type, position, config, reward_digit) VALUES ($1, $2, $3, $4, $5)',
            [run.id, type, index + 1, JSON.stringify(game.quests[type]), code[index]],
        );
    }
    return summarize(run);
}

export async function listRuns(pool: Pool, owner: string, gameId: string) {
    const result = await pool.query<RunRow>(
        `
    SELECT r.* FROM runs r JOIN games g ON g.id = r.game_id
    WHERE r.game_id = $1 AND g.organizer_id = $2 ORDER BY r.created_at DESC
  `,
        [gameId, owner],
    );
    return result.rows.map(summarize);
}

export async function readProgress(pool: Pool, owner: string, gameId: string, runId: string) {
    const result = await pool.query<RunRow>(
        `
    SELECT r.* FROM runs r JOIN games g ON g.id = r.game_id
    WHERE r.id = $1 AND r.game_id = $2 AND g.organizer_id = $3
  `,
        [runId, gameId, owner],
    );
    const run = result.rows[0];
    if (!run) {
        throw new HttpError(404, 'RUN_NOT_FOUND', 'Запуск не найден.');
    }
    const quests = await pool.query<
        RunQuest & {
            duration: number | null;
            attempts: { at: Date; status: AttemptResult['status'] }[];
        }
    >(
        `
    SELECT q.type, q.position, q.started_at, q.solved_at,
      floor(extract(epoch FROM (coalesce(q.solved_at, now()) - q.started_at)))::int AS duration,
      COALESCE(jsonb_agg(jsonb_build_object('at', a.submitted_at, 'status', a.result->>'status')
        ORDER BY a.submitted_at, a.request_id) FILTER (WHERE a.request_id IS NOT NULL), '[]') AS attempts
    FROM run_quests q LEFT JOIN attempts a ON a.run_id = q.run_id AND a.quest_type = q.type
    WHERE q.run_id = $1 GROUP BY q.run_id, q.type ORDER BY q.position
  `,
        [runId],
    );
    return {
        ...summarize(run),
        quests: quests.rows.map((quest) => ({
            type: quest.type,
            position: quest.position,
            startedAt: quest.started_at,
            solvedAt: quest.solved_at,
            durationSeconds: quest.duration,
            attempts: quest.attempts,
        })),
    };
}

function reward(quest: RunQuest) {
    return { position: quest.position, digit: quest.reward_digit };
}

function remainingAttempts(quest: Quest, consumed: number) {
    return quest.type === 'nfc' ? Math.max(0, quest.limit - consumed) : undefined;
}

export async function playerState(pool: Pool, runId: string) {
    const rows = await pool.query<
        RunQuest & { title: string; consumed: number; team_name: string; finished_at: Date | null }
    >(
        `
    SELECT q.*, c.title, r.team_name, r.finished_at, count(a.request_id)::int AS consumed
    FROM run_quests q JOIN runs r ON r.id = q.run_id
    JOIN quest_catalog c ON c.type = q.type
    LEFT JOIN attempts a ON a.run_id = q.run_id AND a.quest_type = q.type AND a.result->>'consumed' = 'true'
    WHERE q.run_id = $1 GROUP BY q.run_id, q.type, r.id, c.type ORDER BY q.position
  `,
        [runId],
    );
    if (!rows.rows.length) {
        throw new HttpError(404, 'RUN_NOT_FOUND', 'Запуск не найден.');
    }
    const quests = rows.rows.map((row): PlayerQuest => {
        const config = row.config;
        const remaining = remainingAttempts(config, row.consumed);
        return {
            type: row.type,
            title: row.title,
            instruction: config.instruction,
            position: row.position,
            status: row.solved_at
                ? 'solved'
                : remaining === 0
                  ? 'locked'
                  : row.started_at
                    ? 'in_progress'
                    : 'ready',
            reward: row.solved_at ? reward(row) : undefined,
            picture:
                config.type === 'photo'
                    ? config.picture
                    : config.type === 'nfc'
                      ? config.hintPicture
                      : undefined,
            slots: config.type === 'objects' || config.type === 'tree' ? config.slots : undefined,
            remainingAttempts: remaining,
        };
    });
    return {
        id: runId,
        teamName: rows.rows[0].team_name,
        finished: Boolean(rows.rows[0].finished_at),
        quests,
    };
}

export async function markOpened(db: Queryable, runId: string, type: QuestType) {
    const result = await db.query<RunQuest>(
        `
    UPDATE run_quests SET started_at = COALESCE(started_at, clock_timestamp()) WHERE run_id = $1 AND type = $2 RETURNING *
  `,
        [runId, type],
    );
    if (!result.rows[0]) {
        throw new HttpError(404, 'QUEST_NOT_FOUND', 'Квест не найден.');
    }
    return result.rows[0];
}

export async function submitAnswer(
    pool: Pool,
    runId: string,
    type: QuestType,
    requestId: string,
    answer: Answer,
) {
    return transaction(pool, async (client): Promise<AttemptResult> => {
        // Serialize submissions for a team so retries and simultaneous solves cannot consume extra attempts.
        const run = await readRun(client, runId, true);
        const prior = await client.query<{ quest_type: QuestType; result: StoredResult }>(
            'SELECT quest_type, result FROM attempts WHERE run_id = $1 AND request_id = $2',
            [runId, requestId],
        );
        if (prior.rows[0]) {
            if (prior.rows[0].quest_type !== type) {
                throw new HttpError(
                    409,
                    'ATTEMPT_ID_REUSED',
                    'Идентификатор уже использован для другого квеста.',
                );
            }
            const { status, remainingAttempts, reward, finished } = prior.rows[0].result;
            return { status, remainingAttempts, reward, finished };
        }

        const quest = await markOpened(client, runId, type);
        const count = await client.query<{ consumed: number }>(
            "SELECT count(*)::int AS consumed FROM attempts WHERE run_id = $1 AND quest_type = $2 AND result->>'consumed' = 'true'",
            [runId, type],
        );
        const remaining = remainingAttempts(quest.config, count.rows[0].consumed);
        if (quest.solved_at) {
            return {
                status: 'correct',
                reward: reward(quest),
                remainingAttempts: remaining,
                finished: Boolean(run.finished_at),
            };
        }
        const evaluation = remaining === 0 ? { correct: false } : evaluateAnswer(quest.config, answer);
        const consumed = evaluation.consumesAttempt === true;
        let finished = false;
        if (evaluation.correct) {
            await client.query(
                'UPDATE run_quests SET solved_at = clock_timestamp() WHERE run_id = $1 AND type = $2',
                [runId, type],
            );
            const update = await client.query(
                `
        UPDATE runs SET finished_at = clock_timestamp() WHERE id = $1
        AND NOT EXISTS (SELECT 1 FROM run_quests WHERE run_id = $1 AND solved_at IS NULL)
      `,
                [runId],
            );
            finished = Boolean(update.rowCount);
        }
        const result: AttemptResult = {
            status: evaluation.correct ? 'correct' : remaining === 0 ? 'locked' : 'incorrect',
            remainingAttempts: remaining === undefined ? undefined : remaining - Number(consumed),
            reward: evaluation.correct ? reward(quest) : undefined,
            finished,
        };
        await client.query(
            'INSERT INTO attempts (run_id, request_id, quest_type, submitted_at, answer, result) VALUES ($1, $2, $3, clock_timestamp(), $4, $5)',
            [runId, requestId, type, JSON.stringify(answer), JSON.stringify({ ...result, consumed })],
        );
        return result;
    });
}
