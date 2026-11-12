import type { PlayerSession } from '../../../../shared/runs';
import { api } from '../../../app/api/api';

type StoredSession = Pick<PlayerSession, 'token' | 'expiresAt'> & { runId: string };

export function clearPlayerSession(code: string) {
    try {
        sessionStorage.removeItem(`quest-player:${code}`);
    } catch {
        // Joining still works when browser storage is unavailable.
    }
}

export async function joinPlayer(code: string, signal: AbortSignal): Promise<StoredSession> {
    try {
        const stored = JSON.parse(
            sessionStorage.getItem(`quest-player:${code}`) ?? 'null',
        ) as StoredSession | null;
        if (stored?.token && stored.runId && Date.parse(stored.expiresAt) > Date.now()) {
            return stored;
        }
    } catch {
        // A new session can replace missing or outdated browser data.
    }
    const { token, expiresAt, run } = await api<PlayerSession>('/player/runs/join', {
        method: 'POST',
        body: { code },
        signal,
    });
    const session = { token, expiresAt, runId: run.id };
    try {
        sessionStorage.setItem(`quest-player:${code}`, JSON.stringify(session));
    } catch {
        // TanStack Query retains the session for the current page.
    }
    return session;
}
