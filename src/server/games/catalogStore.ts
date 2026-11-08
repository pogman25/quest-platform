import type { Pool, PoolClient } from 'pg';
import type { Catalog, IconCatalogEntry, QuestCatalogEntry } from '../../shared/catalog';
import type { QuestType } from '../../shared/quests';
import { makeTag, makeToken } from '../../shared/tokens';
import { HttpError } from '../http';
import { questSchema } from './gameSchema';

export async function listQuestCatalog(db: Pool | PoolClient) {
    const result = await db.query<QuestCatalogEntry>(
        'SELECT type, title, badge, description FROM quest_catalog ORDER BY position',
    );
    return result.rows;
}

export async function readCatalog(db: Pool): Promise<Catalog> {
    const [quests, icons] = await Promise.all([
        listQuestCatalog(db),
        db.query<IconCatalogEntry>('SELECT id, label FROM icon_catalog ORDER BY position'),
    ]);
    return {
        quests: Object.fromEntries(quests.map((quest) => [quest.type, quest])) as Catalog['quests'],
        icons: icons.rows,
    };
}

export async function instantiateQuest(db: Pool | PoolClient, type: QuestType) {
    const result = await db.query<{ template: unknown }>(
        'SELECT template FROM quest_catalog WHERE type = $1',
        [type],
    );
    const row = result.rows[0];
    if (!row) {
        throw new HttpError(404, 'QUEST_TEMPLATE_NOT_FOUND', 'Шаблон квеста не найден.');
    }
    const template = questSchema.parse(row.template);
    switch (template.type) {
        case 'nfc': {
            const correctIndex = template.tags.findIndex(({ id }) => id === template.correctId);
            if (correctIndex < 0) {
                throw new Error('The NFC template has no matching correct tag.');
            }
            const tags = template.tags.map(({ label }) => makeTag(label));
            return { ...template, tags, correctId: tags[correctIndex].id };
        }
        case 'qr':
            return { ...template, token: makeToken('qr') };
        case 'stencil': {
            const [strokeSeed, number] = crypto.getRandomValues(new Uint32Array(2));
            return { ...template, strokeSeed, correct: (number % 8) + 1 };
        }
        default:
            return template;
    }
}
