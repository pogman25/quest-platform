import { readFile } from 'node:fs/promises';
import { resolve } from 'node:path';
import express from 'express';
import type { Request, Response } from 'express';
import type { Pool } from 'pg';
import { findOrganizerId, organizerAuth } from './auth/organizers';
import { browserToken } from './auth/webSession';
import { adminRouter } from './games/adminRouter';
import { handleError, HttpError } from './http';
import { injectBootstrap, readAdminBootstrap } from './pages/adminBootstrap';
import { playerRouter } from './runs/playerRouter';

export function createApp(pool: Pool, dist = resolve('dist')) {
    const app = express();
    app.disable('x-powered-by');
    app.use('/api', express.json({ limit: '32mb' }));
    app.use('/api', (_request, response, next) => {
        response.set('Cache-Control', 'no-store');
        next();
    });

    app.get('/api/v1/health', async (_request, response) => {
        await pool.query('SELECT 1');
        response.json({ status: 'ok' });
    });
    app.use('/api/v1/auth', organizerAuth(pool));
    app.use('/api/v1/admin', adminRouter(pool));
    app.use('/api/v1/player/runs', playerRouter(pool));
    app.use('/api', () => {
        throw new HttpError(404, 'NOT_FOUND', 'Маршрут API не найден.');
    });

    const servePage = async (request: Request, response: Response) => {
        const token = request.path === '/play' ? undefined : browserToken(request);
        const organizer = token ? await findOrganizerId(pool, token) : undefined;
        const [html, bootstrap] = await Promise.all([
            readFile(resolve(dist, 'index.html'), 'utf8'),
            organizer ? readAdminBootstrap(pool, organizer, request.path) : null,
        ]);
        response
            .set('Cache-Control', 'private, no-store')
            .type('html')
            .send(injectBootstrap(html, bootstrap));
    };

    app.get('/index.html', servePage);
    app.use(express.static(dist, { index: false }));
    app.get('/{*path}', servePage);
    app.use(handleError);
    return app;
}
