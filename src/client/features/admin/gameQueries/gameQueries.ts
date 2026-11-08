import { queryOptions, skipToken } from '@tanstack/react-query';
import type { GameSummary } from '../../../../shared/catalog';
import type { Game } from '../../../../shared/quests';
import { api } from '../../../app/api/api';

export const gamesQuery = queryOptions({
    queryKey: ['admin', 'games', 'summaries'],
    queryFn: async ({ signal }) => {
        const { games } = await api<{ games: GameSummary[] }>('/admin/games?view=summary', { signal });
        return games;
    },
});

export function gameQuery(id?: string) {
    return queryOptions({
        queryKey: ['admin', 'games', 'detail', id],
        queryFn: id
            ? ({ signal }) => api<Game>(`/admin/games/${encodeURIComponent(id)}`, { signal })
            : skipToken,
    });
}
