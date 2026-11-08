import { QueryClient } from '@tanstack/react-query';
import type { AdminBootstrap } from '../../../shared/catalog';
import { gameQuery, gamesQuery } from '../../features/admin/gameQueries/gameQueries';
import { ApiError } from '../api/api';

export function createAdminQueryClient(bootstrap: AdminBootstrap | null) {
    const client = new QueryClient({
        defaultOptions: {
            queries: {
                staleTime: 30_000,
                retry: (failureCount, error) =>
                    failureCount < 1 && !(error instanceof ApiError && error.status < 500),
            },
        },
    });

    if (bootstrap) {
        client.setQueryData(gamesQuery.queryKey, bootstrap.games);
        if (bootstrap.game) {
            client.setQueryData(gameQuery(bootstrap.game.id).queryKey, bootstrap.game);
        }
    }

    return client;
}
