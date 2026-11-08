import { useMutation, useQueryClient } from '@tanstack/react-query';
import type { RunSummary } from '../../../shared/runs';
import { api } from '../../app/api/api';
import { runsQuery } from '../../features/admin/runQueries/runQueries';

interface StartRunInput {
    gameId: string;
    teamName: string;
    code: string;
}

export function useStartRun() {
    const queryClient = useQueryClient();

    return useMutation({
        mutationFn: ({ gameId, teamName, code }: StartRunInput) =>
            api<RunSummary>(`/admin/games/${gameId}/runs`, { method: 'POST', body: { teamName, code } }),
        onSuccess: async (run, { gameId }) => {
            const { queryKey } = runsQuery(gameId);
            await queryClient.cancelQueries({ queryKey, exact: true });
            queryClient.setQueryData(queryKey, (runs) => [
                run,
                ...(runs ?? []).filter(({ id }) => id !== run.id),
            ]);
            await queryClient.invalidateQueries({ queryKey, exact: true });
        },
    });
}
