import { Pool } from 'pg';
import type { PoolClient } from 'pg';

export function connectDatabase() {
    const connectionString = process.env.DATABASE_URL;
    if (!connectionString && !process.env.PGHOST) {
        throw new Error('Set DATABASE_URL or PostgreSQL connection environment variables.');
    }
    return new Pool({ connectionString });
}

export async function transaction<T>(pool: Pool, work: (client: PoolClient) => Promise<T>) {
    const client = await pool.connect();
    try {
        await client.query('BEGIN');
        const result = await work(client);
        await client.query('COMMIT');
        return result;
    } catch (error) {
        await client.query('ROLLBACK');
        throw error;
    } finally {
        client.release();
    }
}
