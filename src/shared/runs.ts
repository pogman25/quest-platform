import type { IconSlot, Picture, QuestType } from './quests';

export type AttemptResult = {
    status: 'correct' | 'incorrect' | 'locked';
    remainingAttempts?: number;
    reward?: { position: number; digit: string };
    finished: boolean;
};

export type RunSummary = {
    id: string;
    teamName: string;
    joinCode: string;
    createdAt: string;
    finishedAt: string | null;
};

export type QuestProgress = {
    type: QuestType;
    position: number;
    startedAt: string | null;
    solvedAt: string | null;
    durationSeconds: number | null;
    attempts: { at: string; status: AttemptResult['status'] }[];
};

export type RunProgress = RunSummary & { quests: QuestProgress[] };

export type PlayerQuest = {
    type: QuestType;
    title: string;
    instruction: string;
    position: number;
    status: 'ready' | 'in_progress' | 'solved' | 'locked';
    reward?: { position: number; digit: string };
    picture?: Picture;
    slots?: IconSlot[];
    remainingAttempts?: number;
};
