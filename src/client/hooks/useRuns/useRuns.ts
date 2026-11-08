import { useQuery } from '@tanstack/react-query';
import { runProgressQuery, runsQuery } from '../../features/admin/runQueries/runQueries';

export function useRuns(gameId: string | undefined, selected: string) {
    const runs = useQuery(runsQuery(gameId));
    const run = runs.data?.find(({ id }) => id === selected) ?? runs.data?.[0];
    const progress = useQuery(runProgressQuery(gameId, run?.id));
    return { runs, run, progress };
}
