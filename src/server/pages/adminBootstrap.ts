import type { Pool } from 'pg';
import type { AdminBootstrap } from '../../shared/catalog';
import { readCatalog } from '../games/catalogStore';
import { listGames, listGameSummaries } from '../games/gameStore';
import { gameId } from '../http';

export async function readAdminBootstrap(
    pool: Pool,
    organizer: string,
    pathname: string,
): Promise<AdminBootstrap> {
    const active = gameId.safeParse(pathname.split('/')[1]);
    const [catalog, games, [game]] = await Promise.all([
        readCatalog(pool),
        listGameSummaries(pool, organizer),
        active.success ? listGames(pool, organizer, active.data) : [],
    ]);
    return { catalog, games, game };
}

export function injectBootstrap(html: string, data: AdminBootstrap | null) {
    const marker = '<!-- quest-bootstrap -->';
    if (!html.includes(marker)) {
        throw new Error('The client build is missing the bootstrap marker.');
    }
    // A JSON script element still uses HTML parsing for its closing tag.
    const json = JSON.stringify(data).replaceAll('<', '\\u003c');
    const script = `<script id="quest-bootstrap" type="application/json">${json}</script>`;
    return html.replace(marker, () => script);
}
