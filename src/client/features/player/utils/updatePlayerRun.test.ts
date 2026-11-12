import { describe, expect, it } from 'vitest';
import type { PlayerRun } from '../../../../shared/runs';
import { updatePlayerRun } from './updatePlayerRun';

const state: PlayerRun = {
    id: 'run',
    teamName: 'Team',
    gameTitle: 'Game',
    story: '',
    rules: '',
    startedAt: '2026-11-12T10:00:00Z',
    finishedAt: '2026-11-12T10:10:00Z',
    durationSeconds: 600,
    finished: true,
    quests: [
        {
            type: 'nfc',
            title: 'Tag',
            instruction: '',
            position: 1,
            status: 'solved',
            startedAt: '2026-11-12T10:01:00Z',
            solvedAt: '2026-11-12T10:02:00Z',
            durationSeconds: 60,
            remainingAttempts: 1,
            reward: { position: 1, digit: '0' },
        },
    ],
};

describe('player response updates', () => {
    it('keeps the final time when repeating a previously solved quest', () => {
        const next = updatePlayerRun(state, 'nfc', {
            status: 'correct',
            finished: true,
            solvedAt: state.quests[0].solvedAt,
            finishedAt: state.finishedAt,
        });
        expect(next!.finishedAt).toBe(state.finishedAt);
        expect(next!.quests[0].solvedAt).toBe(state.quests[0].solvedAt);
    });
    it('does not undo a solution or restore attempts when an old response is retried', () => {
        const next = updatePlayerRun(state, 'nfc', {
            status: 'incorrect',
            finished: false,
            remainingAttempts: 2,
        });
        expect(next!.quests[0]).toMatchObject({
            status: 'solved',
            remainingAttempts: 1,
            reward: { digit: '0' },
        });
        expect(next!.finished).toBe(true);
    });
});
