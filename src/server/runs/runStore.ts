import { randomBytes, randomUUID } from 'node:crypto';
import type { Pool, PoolClient } from 'pg';
import type { Game, QuestType } from '../../shared/quests';
import type { AttemptResult, PlayerRun } from '../../shared/runs';
import { transaction } from '../db/database';
import { HttpError } from '../http';
import { evaluateAnswer } from './answers';
import type { Answer } from './answers';
import { playerQuest, remainingAttempts, reward, runResult, summarize, timing } from './runView';
import type { RunQuest, RunRow } from './runView';

type Queryable = Pool | PoolClient;
type StoredResult = AttemptResult & { consumed: boolean };

async function readRun(db: Queryable, id: string, lock = false) {
    const result = await db.query<RunRow>(`SELECT * FROM runs WHERE id = $1 ${lock ? 'FOR UPDATE' : ''}`, [
        id,
    ]);
    if (!result.rows[0]) {
        throw new HttpError(404, 'RUN_NOT_FOUND', 'Запуск не найден.');
    }
    return result.rows[0];
}

async function readQuests(db: Queryable, runId: string) {
    const { rows } = await db.query<RunQuest>(
        `
        SELECT q.*, c.title,
          count(a.request_id) FILTER (WHERE a.result->>'consumed' = 'true')::int AS consumed,
          COALESCE(jsonb_agg(jsonb_build_object('at', a.submitted_at, 'status', a.result->>'status')
            ORDER BY a.submitted_at, a.request_id) FILTER (WHERE a.request_id IS NOT NULL), '[]') AS attempts
        FROM run_quests q JOIN quest_catalog c ON c.type = q.type
        LEFT JOIN attempts a ON a.run_id = q.run_id AND a.quest_type = q.type
        WHERE q.run_id = $1 GROUP BY q.run_id, q.type, c.type ORDER BY q.position
    `,
        [runId],
    );
    return rows;
}

export async function insertRun(db: PoolClient, game: Game, teamName: string, code: string) {
    const result = await db.query<RunRow>(
        'INSERT INTO runs (id, game_id, join_code, team_name, game_title, story, rules) VALUES ($1, $2, $3, $4, $5, $6, $7) RETURNING *',
        [
            randomUUID(),
            game.id,
            randomBytes(8).toString('hex').toUpperCase(),
            teamName,
            game.title,
            game.story,
            game.rules,
        ],
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
    const quests = await readQuests(pool, runId);
    return {
        ...summarize(run),
        durationSeconds: timing(run.started_at, run.finished_at).durationSeconds,
        result: runResult(run, quests),
        quests: quests.map((quest) => ({
            type: quest.type,
            position: quest.position,
            ...timing(quest.started_at, quest.solved_at),
            status: playerQuest(quest).status,
            reward: quest.solved_at ? reward(quest) : undefined,
            attempts: quest.attempts,
        })),
    };
}

export async function playerState(db: Queryable, runId: string, run?: RunRow): Promise<PlayerRun> {
    const current = run ?? (await readRun(db, runId));
    const quests = await readQuests(db, runId);
    return {
        id: runId,
        teamName: current.team_name,
        gameTitle: current.game_title,
        story: current.story,
        rules: current.rules,
        startedAt: current.started_at?.toISOString() ?? null,
        finishedAt: current.finished_at?.toISOString() ?? null,
        durationSeconds: timing(current.started_at, current.finished_at).durationSeconds,
        finished: Boolean(current.finished_at),
        result: runResult(current, quests),
        quests: quests.map(playerQuest),
    };
}

export async function startRun(pool: Pool, runId: string) {
    return transaction(pool, async (client) => {
        const result = await client.query<RunRow>(
            'UPDATE runs SET started_at = COALESCE(started_at, clock_timestamp()) WHERE id = $1 RETURNING *',
            [runId],
        );
        if (!result.rows[0]) {
            throw new HttpError(404, 'RUN_NOT_FOUND', 'Запуск не найден.');
        }
        return playerState(client, runId, result.rows[0]);
    });
}

function requireStarted(run: RunRow) {
    if (!run.started_at) {
        throw new HttpError(409, 'RUN_NOT_STARTED', 'Нажмите «Начать расследование» перед открытием улик.');
    }
}

async function openQuest(db: Queryable, runId: string, type: QuestType) {
    const result = await db.query<RunQuest>(
        'UPDATE run_quests SET started_at = COALESCE(started_at, clock_timestamp()) WHERE run_id = $1 AND type = $2 RETURNING *',
        [runId, type],
    );
    if (!result.rows[0]) {
        throw new HttpError(404, 'QUEST_NOT_FOUND', 'Квест не найден.');
    }
    return result.rows[0];
}

export async function markOpened(pool: Pool, runId: string, type: QuestType) {
    return transaction(pool, async (client) => {
        requireStarted(await readRun(client, runId, true));
        const quest = await openQuest(client, runId, type);
        return timing(quest.started_at, quest.solved_at);
    });
}

export async function submitAnswer(
    pool: Pool,
    runId: string,
    type: QuestType,
    requestId: string,
    answer: Answer,
) {
    return transaction(pool, async (client): Promise<AttemptResult> => {
        // One team lock serializes start/open/solve events, retries and concurrent final answers.
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
            const {
                status,
                remainingAttempts,
                reward,
                finished,
                finishedAt,
                startedAt,
                solvedAt,
                durationSeconds,
                result,
            } = prior.rows[0].result;
            return {
                status,
                remainingAttempts,
                reward,
                finished,
                finishedAt,
                startedAt,
                solvedAt,
                durationSeconds,
                result,
            };
        }
        requireStarted(run);
        let quest = await openQuest(client, runId, type);
        const quests = await readQuests(client, runId);
        const current = quests.find((item) => item.type === type)!;
        const remaining = remainingAttempts(quest.config, current.consumed);
        if (quest.solved_at) {
            return {
                status: 'correct',
                reward: reward(quest),
                remainingAttempts: remaining,
                ...timing(quest.started_at, quest.solved_at),
                finished: Boolean(run.finished_at),
                finishedAt: run.finished_at?.toISOString() ?? null,
                result: runResult(run, quests),
            };
        }
        const evaluation = remaining === 0 ? { correct: false } : evaluateAnswer(quest.config, answer);
        const consumed = evaluation.consumesAttempt === true;
        let finished = false;
        let finishedAt: string | null = null;
        let result;
        if (evaluation.correct) {
            const solved = await client.query<RunQuest>(
                'UPDATE run_quests SET solved_at = clock_timestamp() WHERE run_id = $1 AND type = $2 RETURNING *',
                [runId, type],
            );
            quest = solved.rows[0];
            const update = await client.query<RunRow>(
                `
                UPDATE runs SET finished_at = (SELECT max(solved_at) FROM run_quests WHERE run_id = $1)
                WHERE id = $1 AND finished_at IS NULL
                AND NOT EXISTS (SELECT 1 FROM run_quests WHERE run_id = $1 AND solved_at IS NULL)
                RETURNING *
            `,
                [runId],
            );
            finished = Boolean(update.rowCount);
            if (finished) {
                finishedAt = update.rows[0].finished_at!.toISOString();
                result = runResult(
                    update.rows[0],
                    quests.map((item) => (item.type === type ? { ...item, ...quest } : item)),
                );
            }
        }
        const response: AttemptResult = {
            status: evaluation.correct ? 'correct' : remaining === 0 ? 'locked' : 'incorrect',
            remainingAttempts: remaining === undefined ? undefined : remaining - Number(consumed),
            reward: evaluation.correct ? reward(quest) : undefined,
            ...timing(quest.started_at, quest.solved_at),
            finished,
            finishedAt,
            result,
        };
        await client.query(
            'INSERT INTO attempts (run_id, request_id, quest_type, submitted_at, answer, result) VALUES ($1, $2, $3, clock_timestamp(), $4, $5)',
            [runId, requestId, type, JSON.stringify(answer), JSON.stringify({ ...response, consumed })],
        );
        return response;
    });
}
