import { readdir, readFile } from 'node:fs/promises';
import { connectDatabase, transaction } from './database';

const pool = connectDatabase();
const folder = new URL('./migrations/', import.meta.url);

try {
    const files = (await readdir(folder)).filter((file) => /^\d+_.+\.sql$/.test(file)).sort();
    await transaction(pool, async (client) => {
        await client.query('SELECT pg_advisory_xact_lock(4102026)');
        await client.query(
            'CREATE TABLE IF NOT EXISTS schema_migrations (name text PRIMARY KEY, applied_at timestamptz NOT NULL DEFAULT now())',
        );
        const applied = await client.query<{ name: string }>('SELECT name FROM schema_migrations');
        const known = new Set(applied.rows.map(({ name }) => name));
        for (const file of files) {
            if (known.has(file)) {
                continue;
            }
            await client.query(await readFile(new URL(file, folder), 'utf8'));
            await client.query('INSERT INTO schema_migrations (name) VALUES ($1)', [file]);
            console.log(`Applied ${file}`);
        }
    });
} finally {
    await pool.end();
}
