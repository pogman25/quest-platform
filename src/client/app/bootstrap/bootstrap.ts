import { messages } from '../messages/ru';
import type { AdminBootstrap } from '../../../shared/catalog';
import { gamesToImport, markGamesImported } from '../../utils/gameStorage';
import { api, ApiError } from '../api/api';

async function requestBootstrap() {
    const query = new URLSearchParams({ path: window.location.pathname });
    try {
        return await api<AdminBootstrap>(`/admin/bootstrap?${query}`);
    } catch (reason) {
        if (reason instanceof ApiError && reason.status === 401) {
            return null;
        }
        throw reason;
    }
}

export async function loadBootstrap() {
    const element = document.getElementById('quest-bootstrap');
    const bootstrap: AdminBootstrap | null = element
        ? JSON.parse(element.textContent ?? 'null')
        : await requestBootstrap();

    if (bootstrap) {
        const games = gamesToImport(bootstrap.catalog);
        if (games.length) {
            try {
                await api('/admin/games/import', { method: 'POST', body: { games } });
                markGamesImported();
                return { bootstrap: await requestBootstrap() };
            } catch (reason) {
                const error = reason instanceof Error ? reason.message : messages['games.importFailed'];
                return { bootstrap, importError: error };
            }
        }
    }
    return { bootstrap };
}
