import { createApp } from './app';
import { connectDatabase } from './db/database';

const pool = connectDatabase();
const host = process.env.HOST ?? '127.0.0.1';
const port = Number(process.env.PORT ?? 4173);

await pool.query('SELECT 1 FROM schema_migrations LIMIT 1');
const server = createApp(pool).listen(port, host, () => {
    console.log(`Quest Platform: http://${host}:${port}`);
});

function shutdown() {
    server.close(() => {
        void pool.end();
    });
}

process.once('SIGTERM', shutdown);
process.once('SIGINT', shutdown);
