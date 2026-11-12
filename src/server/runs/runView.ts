import type { Quest, QuestType } from '../../shared/quests';
import type { AttemptResult, PlayerQuest, RunResult } from '../../shared/runs';

export type RunRow = {
    id: string;
    game_id: string;
    team_name: string;
    join_code: string;
    game_title: string;
    story: string;
    rules: string;
    created_at: Date;
    started_at: Date | null;
    finished_at: Date | null;
};

export type RunQuest = {
    type: QuestType;
    title: string;
    position: number;
    config: Quest;
    reward_digit: string;
    started_at: Date | null;
    solved_at: Date | null;
    consumed: number;
    attempts: { at: string; status: AttemptResult['status'] }[];
};

export function summarize(run: RunRow) {
    return {
        id: run.id,
        teamName: run.team_name,
        joinCode: run.join_code,
        createdAt: run.created_at.toISOString(),
        startedAt: run.started_at?.toISOString() ?? null,
        finishedAt: run.finished_at?.toISOString() ?? null,
    };
}

export function timing(start: Date | null, end: Date | null, now = Date.now()) {
    return {
        startedAt: start?.toISOString() ?? null,
        solvedAt: end?.toISOString() ?? null,
        durationSeconds: start
            ? Math.max(0, Math.floor(((end?.getTime() ?? now) - start.getTime()) / 1000))
            : null,
    };
}

export function reward(quest: RunQuest) {
    return { position: quest.position, digit: quest.reward_digit };
}

export function remainingAttempts(quest: Quest, consumed: number) {
    return quest.type === 'nfc' ? Math.max(0, quest.limit - consumed) : undefined;
}

export function runResult(run: RunRow, quests: RunQuest[]): RunResult | undefined {
    if (
        !run.started_at ||
        !run.finished_at ||
        !quests.length ||
        quests.some((quest) => !quest.solved_at || !quest.started_at)
    ) {
        return undefined;
    }
    const ordered = [...quests].sort((a, b) => a.position - b.position);
    const duration = (quest: RunQuest) => quest.solved_at!.getTime() - quest.started_at!.getTime();
    const slowest = ordered.reduce((longest, quest) =>
        duration(quest) > duration(longest) ? quest : longest,
    );
    return {
        finalCode: ordered.map(({ reward_digit }) => reward_digit).join(''),
        totalSeconds: timing(run.started_at, run.finished_at).durationSeconds!,
        slowestQuest: {
            type: slowest.type,
            title: slowest.title,
            position: slowest.position,
            durationSeconds: timing(slowest.started_at, slowest.solved_at).durationSeconds!,
        },
    };
}

export function playerQuest(row: RunQuest): PlayerQuest {
    const config = row.config;
    const remaining = remainingAttempts(config, row.consumed);
    return {
        type: row.type,
        title: row.title,
        instruction: config.instruction,
        position: row.position,
        ...timing(row.started_at, row.solved_at),
        status: row.solved_at
            ? 'solved'
            : remaining === 0
              ? 'locked'
              : row.started_at
                ? 'in_progress'
                : 'ready',
        reward: row.solved_at ? reward(row) : undefined,
        picture:
            config.type === 'photo' ? config.picture : config.type === 'nfc' ? config.hintPicture : undefined,
        slots: config.type === 'objects' || config.type === 'tree' ? config.slots : undefined,
        remainingAttempts: remaining,
    };
}
