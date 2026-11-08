import type { Game, IconName, QuestType } from './quests';

export type QuestCatalogEntry = {
    type: QuestType;
    title: string;
    badge: string;
    description: string;
};

export type IconCatalogEntry = { id: IconName; label: string };

export type Catalog = {
    quests: Record<QuestType, QuestCatalogEntry>;
    icons: IconCatalogEntry[];
};

export type GameSummary = { id: string; title: string; questCount: number };

export type AdminBootstrap = {
    catalog: Catalog;
    games: GameSummary[];
    game?: Game;
};
