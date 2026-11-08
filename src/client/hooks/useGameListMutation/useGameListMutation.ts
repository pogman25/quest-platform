import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { Game } from '../../../shared/quests';
import { api } from '../../app/api/api';
import { gameQuery, gamesQuery } from '../../features/admin/gameQueries/gameQueries';

type GameChange =
    | { type: 'create'; draft: Game }
    | { type: 'rename'; id: string; title: string }
    | { type: 'delete'; id: string };

async function changeGame(change: GameChange) {
    switch (change.type) {
        case 'create':
            return api<Game>('/admin/games', { method: 'POST', body: change.draft });
        case 'rename':
            await api(`/admin/games/${change.id}`, { method: 'PATCH', body: { title: change.title } });
            break;
        case 'delete':
            await api(`/admin/games/${change.id}`, { method: 'DELETE' });
            break;
    }
}

export function useGameListMutation() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: changeGame,
        onSuccess: async (created, change) => {
            if (created) {
                queryClient.setQueryData(gameQuery(created.id).queryKey, created);
            }
            if (change.type === 'delete') {
                queryClient.removeQueries({ queryKey: gameQuery(change.id).queryKey });
            }
            await Promise.all([
                queryClient.invalidateQueries({ queryKey: gamesQuery.queryKey, exact: true }),
                change.type === 'rename' &&
                    queryClient.invalidateQueries({ queryKey: gameQuery(change.id).queryKey, exact: true }),
            ]);
        },
    });
}
