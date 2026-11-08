import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { Game, Quest, QuestType } from '../../../shared/quests';
import { api } from '../../app/api/api';
import { gameQuery, gamesQuery } from '../../features/admin/gameQueries/gameQueries';

interface SaveGameInput {
    draft: Game;
    newQuest?: QuestType;
}

async function saveGame({ draft, newQuest }: SaveGameInput) {
    let game = draft;
    if (newQuest) {
        const quest = await api<Quest>(`/admin/catalog/${newQuest}/instantiate`, { method: 'POST' });
        game = { ...draft, quests: { ...draft.quests, [newQuest]: quest } };
    }

    return api<Game>(`/admin/games/${game.id}`, { method: 'PUT', body: game });
}

export function useSaveGame() {
    const queryClient = useQueryClient();
    const mutation = useMutation({
        mutationFn: saveGame,
        onSuccess: async (saved) => {
            const { queryKey } = gameQuery(saved.id);
            await queryClient.cancelQueries({ queryKey, exact: true });
            queryClient.setQueryData(queryKey, saved);
            await queryClient.invalidateQueries({ queryKey: gamesQuery.queryKey, exact: true });
        },
    });

    const save = async (draft: Game, newQuest?: QuestType) => {
        try {
            await mutation.mutateAsync({ draft, newQuest });
            return true;
        } catch {
            return false;
        }
    };

    return { save, saving: mutation.isPending, error: mutation.error };
}
