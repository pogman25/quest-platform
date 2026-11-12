import { skipToken, useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useRef } from 'react';
import type { QuestType } from '../../../shared/quests';
import type { AttemptResult, PlayerRun, QuestTiming } from '../../../shared/runs';
import { api } from '../../app/api/api';
import { clearPlayerSession, joinPlayer } from '../../features/player/playerSession/playerSession';
import { updatePlayerRun } from '../../features/player/utils/updatePlayerRun';

export function usePlayerRun(code: string) {
    const queryClient = useQueryClient();
    const attempt = useRef<{ key: string; attemptId: string } | null>(null);
    const sessionKey = ['playerSession', code];
    const session = useQuery({
        queryKey: sessionKey,
        queryFn: /^[0-9a-f]{16}$/i.test(code) ? ({ signal }) => joinPlayer(code, signal) : skipToken,
        staleTime: Infinity,
        retry: false,
    });
    const credentials = session.data;
    const queryKey = ['playerRun', credentials?.runId, credentials?.token];
    const run = useQuery({
        queryKey,
        queryFn: credentials
            ? ({ signal }) =>
                  api<PlayerRun>(`/player/runs/${credentials.runId}`, { token: credentials.token, signal })
            : skipToken,
        staleTime: 0,
        refetchInterval: credentials ? 5000 : false,
    });

    const request = <T>(path: string, body?: unknown) => {
        if (!credentials) {
            throw new Error('Player session is required');
        }
        return api<T>(`/player/runs/${credentials.runId}${path}`, {
            method: 'POST',
            token: credentials.token,
            body,
        });
    };
    const start = useMutation({
        mutationFn: () => request<PlayerRun>('/start'),
        onSuccess: async (state) => {
            await queryClient.cancelQueries({ queryKey, exact: true });
            queryClient.setQueryData(queryKey, state);
            await queryClient.invalidateQueries({ queryKey, exact: true });
        },
    });
    const open = useMutation({
        mutationFn: (type: QuestType) => request<QuestTiming>(`/quests/${type}/open`),
        onSuccess: async () => {
            await queryClient.invalidateQueries({ queryKey, exact: true });
        },
    });
    const submit = useMutation({
        mutationFn: ({ type, answer }: { type: QuestType; answer: string | string[] }) => {
            const key = JSON.stringify([credentials?.runId, type, answer]);
            if (attempt.current?.key !== key) {
                attempt.current = { key, attemptId: crypto.randomUUID() };
            }
            return request<AttemptResult>(`/quests/${type}/attempts`, {
                attemptId: attempt.current.attemptId,
                answer,
            });
        },
        onSuccess: async (result, { type }) => {
            attempt.current = null;
            await queryClient.cancelQueries({ queryKey, exact: true });
            queryClient.setQueryData<PlayerRun>(queryKey, (state) => updatePlayerRun(state, type, result));
            await queryClient.invalidateQueries({ queryKey, exact: true });
        },
    });
    const reconnect = () => {
        clearPlayerSession(code);
        void queryClient.resetQueries({ queryKey: sessionKey, exact: true });
    };
    return { run, session, start, open, submit, reconnect };
}
