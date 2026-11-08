import { queryOptions, skipToken } from '@tanstack/react-query';
import type { RunProgress, RunSummary } from '../../../../shared/runs';
import { api } from '../../../app/api/api';
import { gameQuery } from '../gameQueries/gameQueries';

export function runsQuery(gameId?: string) {
    return queryOptions({
        queryKey: [...gameQuery(gameId).queryKey, 'runs'],
        queryFn: gameId
            ? async ({ signal }) => {
                  const { runs } = await api<{ runs: RunSummary[] }>(`/admin/games/${gameId}/runs`, {
                      signal,
                  });
                  return runs;
              }
            : skipToken,
    });
}

export function runProgressQuery(gameId?: string, runId?: string) {
    return queryOptions({
        queryKey: [...runsQuery(gameId).queryKey, runId],
        queryFn:
            gameId && runId
                ? ({ signal }) => api<RunProgress>(`/admin/games/${gameId}/runs/${runId}`, { signal })
                : skipToken,
        staleTime: 0,
        refetchInterval: gameId && runId ? 10_000 : false,
        refetchIntervalInBackground: true,
    });
}
