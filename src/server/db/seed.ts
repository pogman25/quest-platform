import { randomUUID } from 'node:crypto';
import { demoGameId } from '../../shared/legacy';
import type { Game, QuestType } from '../../shared/quests';
import { hashPassword } from '../auth/credentials';
import { instantiateQuest, listQuestCatalog } from '../games/catalogStore';
import { insertGame } from '../games/gameStore';
import { insertRun } from '../runs/runStore';
import { connectDatabase, transaction } from './database';

const progress: { type: QuestType; start: number; solved?: number; attempts: [number, boolean, string][] }[] =
    [
        {
            type: 'nfc',
            start: 6,
            solved: 130,
            attempts: [
                [51, false, 'Ложная метка'],
                [130, true, 'Правильная метка'],
            ],
        },
        { type: 'qr', start: 144, solved: 466, attempts: [[466, true, 'QR-код']] },
        { type: 'objects', start: 488, attempts: [[533, false, 'Неверный набор']] },
        { type: 'photo', start: 548, solved: 613, attempts: [[613, true, 'фонарик']] },
    ];

const email = (process.env.ADMIN_EMAIL ?? 'organizer@example.test').toLowerCase();
const password = process.env.ADMIN_PASSWORD ?? (process.env.NODE_ENV === 'production' ? '' : 'demo-password');
if (!password) {
    throw new Error('Set ADMIN_PASSWORD before seeding in production.');
}
const pool = connectDatabase();

try {
    await transaction(pool, async (client) => {
        await client.query('SELECT pg_advisory_xact_lock(4102027)');
        await client.query(
            'INSERT INTO organizers (id, email, password_hash) VALUES ($1, $2, $3) ON CONFLICT (email) DO NOTHING',
            [randomUUID(), email, await hashPassword(password)],
        );
        const account = await client.query<{ id: string }>('SELECT id FROM organizers WHERE email = $1', [
            email,
        ]);
        const catalog = await listQuestCatalog(client);
        const quests: Game['quests'] = {};
        for (const { type } of catalog) {
            quests[type] = await instantiateQuest(client, type);
        }
        const game: Game = {
            id: demoGameId,
            title: 'Дело о пропавшем коде',
            order: catalog.map(({ type }) => type),
            quests,
        };
        if (!(await insertGame(client, account.rows[0].id, game))) {
            return;
        }
        const run = await insertRun(client, game, 'Команда № 1', '625481');
        const start = Date.now() - 650_000;
        const at = (seconds: number) => new Date(start + seconds * 1000);
        await client.query('UPDATE runs SET created_at = $2 WHERE id = $1', [run.id, at(0)]);
        for (const { type, start, solved, attempts } of progress) {
            await client.query(
                'UPDATE run_quests SET started_at = $3, solved_at = $4 WHERE run_id = $1 AND type = $2',
                [run.id, type, at(start), solved === undefined ? null : at(solved)],
            );
            for (const [seconds, correct, answer] of attempts) {
                await client.query(
                    'INSERT INTO attempts (run_id, request_id, quest_type, submitted_at, answer, result) VALUES ($1, $2, $3, $4, $5, $6)',
                    [
                        run.id,
                        randomUUID(),
                        type,
                        at(seconds),
                        JSON.stringify(answer),
                        JSON.stringify({
                            status: correct ? 'correct' : 'incorrect',
                            consumed: type === 'nfc' && !correct,
                            finished: false,
                        }),
                    ],
                );
            }
        }
    });
    console.log('Demo data is ready.');
} finally {
    await pool.end();
}
