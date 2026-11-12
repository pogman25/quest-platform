import type { IconSlot, Picture, QuestType } from './quests';

export type QuestTiming = {
    startedAt: string | null;
    solvedAt: string | null;
    durationSeconds: number | null;
};

export type RunResult = {
    finalCode: string;
    totalSeconds: number;
    slowestQuest: { type: QuestType; title: string; position: number; durationSeconds: number };
};

export type AttemptResult = Partial<QuestTiming> & {
    status: 'correct' | 'incorrect' | 'locked';
    remainingAttempts?: number;
    reward?: { position: number; digit: string };
    finished: boolean;
    finishedAt?: string | null;
    result?: RunResult;
};

export type RunSummary = {
    id: string;
    teamName: string;
    joinCode: string;
    createdAt: string;
    startedAt: string | null;
    finishedAt: string | null;
};

export type QuestProgress = QuestTiming & {
    type: QuestType;
    position: number;
    status: PlayerQuest['status'];
    reward?: { position: number; digit: string };
    attempts: { at: string; status: AttemptResult['status'] }[];
};

export type RunProgress = RunSummary & {
    durationSeconds: number | null;
    result?: RunResult;
    quests: QuestProgress[];
};

export type PlayerQuest = QuestTiming & {
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

export type PlayerRun = {
    id: string;
    teamName: string;
    gameTitle: string;
    story: string;
    rules: string;
    startedAt: string | null;
    finishedAt: string | null;
    durationSeconds: number | null;
    finished: boolean;
    result?: RunResult;
    quests: PlayerQuest[];
};

export type PlayerSession = { token: string; expiresAt: string; run: PlayerRun };
