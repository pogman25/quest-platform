import { randomUUID } from 'node:crypto';
import { once } from 'node:events';
import { readdir, readFile } from 'node:fs/promises';
import type { Server } from 'node:http';
import { Pool } from 'pg';
import { afterAll, beforeAll, describe, expect, it } from 'vitest';
import type { Game } from '../../shared/quests';
import { questTypes } from '../../shared/quests';
import type {
    AttemptResult,
    PlayerRun,
    PlayerSession,
    QuestTiming,
    RunProgress,
    RunSummary,
} from '../../shared/runs';
import { createApp } from '../app';
import { tokenHash } from '../auth/credentials';
import { instantiateQuest } from '../games/catalogStore';

// Use an isolated schema in an explicitly supplied test database; never use DATABASE_URL implicitly.
describe.skipIf(!process.env.TEST_DATABASE_URL)('player API with PostgreSQL', () => {
    const schema = `test_${randomUUID().replaceAll('-', '')}`;
    const pool = new Pool({
        connectionString: process.env.TEST_DATABASE_URL,
        options: `-c search_path=${schema}`,
    });
    const owner = randomUUID();
    const adminToken = randomUUID();
    let server: Server;
    let origin: string;
    let game: Game;

    const call = async <T>(
        path: string,
        {
            method = 'GET',
            token,
            body,
            status = 200,
        }: { method?: string; token?: string; body?: unknown; status?: number } = {},
    ): Promise<T> => {
        const response = await fetch(`${origin}/api/v1${path}`, {
            method,
            headers: {
                'Content-Type': 'application/json',
                ...(token ? { Authorization: `Bearer ${token}` } : {}),
            },
            body: JSON.stringify(body),
        });
        expect(response.status, `${method} ${path}`).toBe(status);
        return response.status === 204 ? (undefined as T) : response.json();
    };
    const newRun = () =>
        call<RunSummary>(`/admin/games/${game.id}/runs`, {
            method: 'POST',
            token: adminToken,
            body: { teamName: 'Detectives', code: '012345' },
            status: 201,
        });
    const join = (code: string) =>
        call<PlayerSession>('/player/runs/join', { method: 'POST', body: { code }, status: 201 });

    beforeAll(async () => {
        await pool.query(`CREATE SCHEMA ${schema}`);
        const folder = new URL('../db/migrations/', import.meta.url);
        for (const file of (await readdir(folder)).filter((file) => file.endsWith('.sql')).sort()) {
            await pool.query(await readFile(new URL(file, folder), 'utf8'));
        }
        await pool.query('INSERT INTO organizers VALUES ($1, $2, $3)', [owner, 'api@example.test', 'unused']);
        await pool.query("INSERT INTO organizer_sessions VALUES ($1, $2, now() + interval '1 hour')", [
            tokenHash(adminToken),
            owner,
        ]);
        game = {
            id: 'api-test',
            title: 'Case',
            story: 'Original story',
            rules: 'Original rules',
            order: [...questTypes],
            quests: {},
        };
        for (const type of questTypes) {
            game.quests[type] = await instantiateQuest(pool, type);
        }
        server = createApp(pool).listen(0, '127.0.0.1');
        await once(server, 'listening');
        const address = server.address();
        if (!address || typeof address === 'string') {
            throw new Error('Missing test server address');
        }
        origin = `http://127.0.0.1:${address.port}`;
        await call('/admin/games', { method: 'POST', token: adminToken, body: game, status: 201 });
    });

    afterAll(async () => {
        if (server) {
            await new Promise<void>((resolve, reject) =>
                server.close((error) => (error ? reject(error) : resolve())),
            );
        }
        await pool.query(`DROP SCHEMA IF EXISTS ${schema} CASCADE`);
        await pool.end();
    });

    it('snapshots the briefing, joins without starting, and makes explicit starts and opens idempotent', async () => {
        const created = await newRun();
        expect(created.startedAt).toBeNull();
        await call(`/admin/games/${game.id}`, {
            method: 'PUT',
            token: adminToken,
            body: { ...game, story: 'New story', rules: 'New rules' },
        });
        const { token, run } = await join(created.joinCode.toLowerCase());
        expect(run).toMatchObject({
            story: 'Original story',
            rules: 'Original rules',
            startedAt: null,
            finished: false,
        });
        expect(run.quests.every((quest) => quest.startedAt === null && !quest.reward)).toBe(true);
        const prefix = `/player/runs/${run.id}`;
        await call(`${prefix}/quests/stencil/open`, { method: 'POST', token, status: 409 });
        await call(`${prefix}/quests/stencil/attempts`, {
            method: 'POST',
            token,
            body: { attemptId: randomUUID(), answer: 'СНЕГОПАД' },
            status: 409,
        });
        const starts = await Promise.all(
            [0, 1, 2].map(() => call<PlayerRun>(`${prefix}/start`, { method: 'POST', token })),
        );
        expect(starts[0].startedAt).not.toBeNull();
        expect(new Set(starts.map(({ startedAt }) => startedAt)).size).toBe(1);
        const opened = await call<QuestTiming>(`${prefix}/quests/stencil/open`, { method: 'POST', token });
        const reopened = await call<QuestTiming>(`${prefix}/quests/stencil/open`, { method: 'POST', token });
        expect(reopened.startedAt).toBe(opened.startedAt);
        const state = await call<PlayerRun>(prefix, { token });
        expect(state.quests.find(({ type }) => type === 'stencil')).toMatchObject({
            startedAt: opened.startedAt,
            solvedAt: null,
            status: 'in_progress',
        });
        expect(state).not.toHaveProperty('result');
        const another = await newRun();
        await call(`/player/runs/${another.id}/start`, { method: 'POST', token, status: 401 });
        await call(`/admin/games/${game.id}`, { method: 'PUT', token: adminToken, body: game });
    });

    it('returns final results on the sixth solve and keeps retries, timers, and concurrent final solves stable', async () => {
        const created = await newRun();
        const { token, run } = await join(created.joinCode);
        const prefix = `/player/runs/${run.id}`;
        await call(`${prefix}/start`, { method: 'POST', token });
        await pool.query(
            "UPDATE runs SET started_at = clock_timestamp() - interval '100 seconds' WHERE id = $1",
            [run.id],
        );
        await pool.query(
            "UPDATE run_quests SET started_at = clock_timestamp() - CASE WHEN type = 'stencil' THEN interval '90 seconds' ELSE interval '5 seconds' END WHERE run_id = $1",
            [run.id],
        );
        const nfc = game.quests.nfc;
        const answers = [
            nfc?.type === 'nfc' ? nfc.tags.find(({ id }) => id === nfc.correctId)!.token : '',
            game.quests.qr?.type === 'qr' ? game.quests.qr.token : '',
            game.quests.objects?.type === 'objects' ? game.quests.objects.answers : [],
            'ФОНАРЬ',
            game.quests.tree?.type === 'tree' ? game.quests.tree.answers : [],
            '  снегопад  ',
        ];
        for (const [index, type] of questTypes.slice(0, 4).entries()) {
            const response = await call<AttemptResult>(`${prefix}/quests/${type}/attempts`, {
                method: 'POST',
                token,
                body: { attemptId: randomUUID(), answer: answers[index] },
            });
            expect(response).toMatchObject({
                status: 'correct',
                finished: false,
                reward: { position: index + 1, digit: String(index) },
            });
            expect(response).not.toHaveProperty('result');
        }
        const finalAttempt = randomUUID();
        const final = await Promise.all([
            call<AttemptResult>(`${prefix}/quests/tree/attempts`, {
                method: 'POST',
                token,
                body: { attemptId: randomUUID(), answer: answers[4] },
            }),
            call<AttemptResult>(`${prefix}/quests/stencil/attempts`, {
                method: 'POST',
                token,
                body: { attemptId: finalAttempt, answer: answers[5] },
            }),
        ]);
        expect(final.filter(({ finished }) => finished)).toHaveLength(1);
        const state = await call<PlayerRun>(prefix, { token });
        expect(state.result).toMatchObject({ finalCode: '012345', slowestQuest: { type: 'stencil' } });
        expect(state.result!.totalSeconds).toBeGreaterThanOrEqual(100);
        expect(state.result!.slowestQuest.durationSeconds).toBeGreaterThanOrEqual(90);
        const retry = await call<AttemptResult>(`${prefix}/quests/stencil/attempts`, {
            method: 'POST',
            token,
            body: { attemptId: finalAttempt, answer: answers[5] },
        });
        expect(retry).toEqual(final[1]);
        const solved = state.quests.find(({ type }) => type === 'stencil')!;
        const reopen = await call<QuestTiming>(`${prefix}/quests/stencil/open`, { method: 'POST', token });
        expect(reopen).toMatchObject({
            startedAt: solved.startedAt,
            solvedAt: solved.solvedAt,
            durationSeconds: solved.durationSeconds,
        });
        const repeated = await call<AttemptResult>(`${prefix}/quests/stencil/attempts`, {
            method: 'POST',
            token,
            body: { attemptId: randomUUID(), answer: { word: 'СНЕГОПАД', overlay: 8 } },
        });
        expect(repeated.result).toEqual(state.result);
        const restarted = await call<PlayerRun>(`${prefix}/start`, { method: 'POST', token });
        expect(restarted.startedAt).toBe(state.startedAt);
        expect(restarted.finishedAt).toBe(state.finishedAt);
        await call(`${prefix}/quests/photo/attempts`, {
            method: 'POST',
            token,
            body: { attemptId: finalAttempt, answer: 'фонарь' },
            status: 409,
        });
        const progress = await call<RunProgress>(`/admin/games/${game.id}/runs/${run.id}`, {
            token: adminToken,
        });
        expect(progress.result).toEqual(state.result);
        expect(progress.quests.every(({ reward }) => reward)).toBe(true);
    });

    it('accepts legacy stencil objects and consumes a concurrent NFC retry only once', async () => {
        const created = await newRun();
        const { token, run } = await join(created.joinCode);
        const prefix = `/player/runs/${run.id}`;
        await call(`${prefix}/start`, { method: 'POST', token });
        const wrong = await call<AttemptResult>(`${prefix}/quests/stencil/attempts`, {
            method: 'POST',
            token,
            body: { attemptId: randomUUID(), answer: 'метель' },
        });
        expect(wrong.status).toBe('incorrect');
        expect(wrong.reward).toBeUndefined();
        const legacy = await call<AttemptResult>(`${prefix}/quests/stencil/attempts`, {
            method: 'POST',
            token,
            body: { attemptId: randomUUID(), answer: { word: 'СНЕГОПАД', overlay: 8 } },
        });
        expect(legacy.status).toBe('correct');
        const nfc = game.quests.nfc;
        if (nfc?.type !== 'nfc') {
            throw new Error('Missing NFC fixture');
        }
        const decoy = nfc.tags.find(({ id }) => id !== nfc.correctId)!.token;
        const body = { attemptId: randomUUID(), answer: decoy };
        const results = await Promise.all(
            [0, 1].map(() =>
                call<AttemptResult>(`${prefix}/quests/nfc/attempts`, { method: 'POST', token, body }),
            ),
        );
        expect(results[0]).toEqual(results[1]);
        expect(results[0].remainingAttempts).toBe(nfc.limit - 1);
        const state = await call<PlayerRun>(prefix, { token });
        expect(state.quests.find(({ type }) => type === 'nfc')!.remainingAttempts).toBe(nfc.limit - 1);
        const progress = await call<RunProgress>(`/admin/games/${game.id}/runs/${run.id}`, {
            token: adminToken,
        });
        expect(progress.quests.find(({ type }) => type === 'nfc')!.attempts).toHaveLength(1);
    });
});
