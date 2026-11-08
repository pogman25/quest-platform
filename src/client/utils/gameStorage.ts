import type { Catalog } from '../../shared/catalog';
import { demoGameId } from '../../shared/legacy';
import type { Game } from '../../shared/quests';

const storageKey = 'quest-bureau.games.v1';
const importKey = 'quest-platform-games-imported';

function replaceSnowIcon(game: Game, catalog: Catalog): Game {
    const quest = game.quests.objects;
    if (quest?.type !== 'objects' || !quest.slots.some(({ icon }) => icon === 'cloud')) {
        return game;
    }
    const cloud = catalog.icons.find(({ id }) => id === 'cloud');
    const lamp = catalog.icons.find(({ id }) => id === 'lamp');
    if (!lamp) {
        return game;
    }
    const slots = quest.slots.map((slot) =>
        slot.icon === 'cloud'
            ? { ...slot, icon: lamp.id, label: slot.label === cloud?.label ? lamp.label : slot.label }
            : slot,
    );
    return { ...game, quests: { ...game.quests, objects: { ...quest, slots } } };
}

export function gamesToImport(catalog: Catalog) {
    try {
        const stored = window.localStorage.getItem(storageKey);
        if (stored && !window.localStorage.getItem(importKey)) {
            const games: Game[] = JSON.parse(stored);
            if (Array.isArray(games)) {
                return games
                    .filter(({ id }) => id !== demoGameId)
                    .map((game) => replaceSnowIcon(game, catalog));
            }
        }
    } catch {
        // The server can provide games if browser storage is disabled.
    }
    return [];
}

export function markGamesImported() {
    try {
        window.localStorage.setItem(importKey, 'true');
    } catch {
        // The import endpoint skips games that already exist.
    }
}
