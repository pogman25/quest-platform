import { z } from 'zod';
import { iconNames, questTypes } from '../../shared/quests';
import { gameId } from '../http';

export const questType = z.enum(questTypes);
const id = z.string().min(1).max(100);
const token = z.string().min(1).max(512);
const picture = z.object({
    name: z.string().max(255),
    src: z
        .string()
        .max(8_000_000)
        .regex(/^data:image\/(?:png|jpeg|webp|svg\+xml)[;,]/),
});
const base = z.object({ instruction: z.string().max(500) });
const iconQuest = base.extend({
    type: z.enum(['objects', 'tree']),
    slots: z
        .array(
            z.object({
                id,
                icon: z.enum(iconNames),
                label: z.string().max(80),
                picture: picture.optional(),
            }),
        )
        .length(9),
    answers: z.array(id).max(4),
});

export const questSchema = z.discriminatedUnion('type', [
    base.extend({
        type: z.literal('nfc'),
        hintPicture: picture.optional(),
        tags: z
            .array(z.object({ id, label: z.string().max(255), token }))
            .min(1)
            .max(100),
        correctId: id,
        limit: z.number().int().min(1).max(20),
    }),
    base.extend({
        type: z.literal('qr'),
        picture,
        token,
        size: z.number().int().min(90).max(200),
        fit: z.enum(['cover', 'contain']),
    }),
    base.extend({
        type: z.literal('photo'),
        picture,
        answers: z.string().max(2000),
    }),
    iconQuest,
    base.extend({
        type: z.literal('stencil'),
        word: z.string().max(12),
        font: z.enum(['rounded', 'serif', 'mono', 'custom']),
        width: z.number().int().min(100).max(190),
        correct: z.number().int().min(1).max(8),
        customFont: z
            .object({
                name: z.string().max(255),
                family: z.string().max(255),
                src: z.string().max(2_200_000).startsWith('data:'),
            })
            .optional(),
        strokeSeed: z.number().int().min(0).max(0xffffffff).optional(),
        strokeVersion: z.literal(2).optional(),
    }),
]);

export const gameSchema = z
    .object({
        id: gameId,
        title: z.string().trim().min(1).max(80),
        order: z.array(questType).max(questTypes.length),
        quests: z.partialRecord(questType, questSchema),
    })
    .refine(
        ({ order, quests }) =>
            new Set(order).size === order.length &&
            order.every((type) => quests[type]) &&
            Object.entries(quests).every(([type, config]) => type === config.type),
        { message: 'Порядок и конфигурации квестов должны совпадать.' },
    );
