import { describe, expect, it } from 'vitest';
import { playerQuest, runResult, timing } from './runView';
import type { RunQuest, RunRow } from './runView';

const at = (seconds: number) => new Date(Date.UTC(2026, 10, 12) + seconds * 1000);
const run: RunRow = {
    id: 'run',
    game_id: 'game',
    team_name: 'Team',
    join_code: 'code',
    game_title: 'Game',
    story: 'Story',
    rules: 'Rules',
    created_at: at(0),
    started_at: at(100),
    finished_at: at(180),
};
const quest: RunQuest = {
    type: 'stencil',
    title: 'Snow',
    position: 2,
    reward_digit: '5',
    consumed: 0,
    attempts: [],
    started_at: at(110),
    solved_at: at(180),
    config: {
        type: 'stencil',
        instruction: 'Find a word',
        word: 'СНЕГОПАД',
        correct: 3,
        font: 'rounded',
        width: 168,
    },
};

describe('run presentation', () => {
    it('keeps time running until the solution and freezes the solved duration', () => {
        expect(timing(at(110), null, at(140).getTime()).durationSeconds).toBe(30);
        expect(timing(at(110), at(130), at(500).getTime()).durationSeconds).toBe(20);
        expect(timing(null, null).durationSeconds).toBeNull();
    });

    it('orders code digits by position and selects the longest quest rather than the last solution', () => {
        const other = {
            ...quest,
            type: 'photo' as const,
            title: 'Photo',
            position: 1,
            reward_digit: '0',
            started_at: at(100),
            solved_at: at(175),
        };
        expect(runResult(run, [quest, other])).toEqual({
            finalCode: '05',
            totalSeconds: 80,
            slowestQuest: { type: 'photo', title: 'Photo', position: 1, durationSeconds: 75 },
        });
        expect(runResult({ ...run, finished_at: null }, [quest])).toBeUndefined();
        expect(runResult(run, [{ ...quest, solved_at: null }])).toBeUndefined();
    });

    it('exposes timings but keeps the stencil word, correct overlay and unearned digit private', () => {
        const publicQuest = playerQuest({ ...quest, solved_at: null });
        expect(publicQuest.startedAt).toBe(at(110).toISOString());
        expect(publicQuest.status).toBe('in_progress');
        expect(publicQuest.reward).toBeUndefined();
        expect(publicQuest).not.toHaveProperty('config');
        expect(publicQuest).not.toHaveProperty('word');
        expect(publicQuest).not.toHaveProperty('correct');
        expect(playerQuest(quest).reward).toEqual({ position: 2, digit: '5' });
    });
});
