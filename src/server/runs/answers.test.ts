import { describe, expect, it } from 'vitest';
import type { IconQuest, PhotoQuest } from '../../shared/quests';
import { evaluateAnswer } from './answers';

describe('quest answers', () => {
    it('charges NFC attempts only for registered decoys', () => {
        const quest = {
            type: 'nfc' as const,
            instruction: 'Сканируйте',
            correctId: 'right',
            limit: 3,
            tags: [
                { id: 'right', label: 'Цель', token: 'valid' },
                { id: 'wrong', label: 'Ловушка', token: 'decoy' },
            ],
        };
        expect(evaluateAnswer(quest, 'valid')).toEqual({ correct: true, consumesAttempt: false });
        expect(evaluateAnswer(quest, 'decoy')).toEqual({ correct: false, consumesAttempt: true });
        expect(evaluateAnswer(quest, 'stranger')).toEqual({ correct: false, consumesAttempt: false });
    });

    it('accepts an unordered selection for objects and rejects duplicates', () => {
        const quest: IconQuest = {
            type: 'objects',
            instruction: '',
            slots: [],
            answers: ['slot-1', 'slot-4', 'slot-9'],
        };
        expect(evaluateAnswer(quest, ['slot-9', 'slot-1', 'slot-4']).correct).toBe(true);
        expect(evaluateAnswer(quest, ['slot-1', 'slot-4', 'slot-4']).correct).toBe(false);
    });

    it('requires the exact order of ornaments', () => {
        const quest: IconQuest = {
            type: 'tree',
            instruction: '',
            slots: [],
            answers: ['slot-1', 'slot-5', 'slot-2', 'slot-6'],
        };
        expect(evaluateAnswer(quest, ['slot-1', 'slot-5', 'slot-2', 'slot-6']).correct).toBe(true);
        expect(evaluateAnswer(quest, ['slot-5', 'slot-1', 'slot-2', 'slot-6']).correct).toBe(false);
    });

    it('normalizes photo answers and requires both fields for stencils', () => {
        const photo: PhotoQuest = {
            type: 'photo',
            instruction: '',
            picture: { name: 'Room', src: '' },
            answers: 'фонарик, фонарь',
        };
        expect(evaluateAnswer(photo, '  ФОНАРЬ ').correct).toBe(true);
        const quest = {
            type: 'stencil' as const,
            word: 'СНЕГОПАД',
            correct: 3,
            instruction: '',
            width: 168,
            font: 'rounded' as const,
        };
        expect(evaluateAnswer(quest, { word: 'снегопад', overlay: 3 }).correct).toBe(true);
        expect(evaluateAnswer(quest, { word: 'снегопад', overlay: 2 }).correct).toBe(false);
        expect(evaluateAnswer(quest, 'снегопад').correct).toBe(false);
    });

    it('rejects whitespace even if the configured answers end with a comma', () => {
        const quest = {
            type: 'photo' as const,
            instruction: '',
            picture: { name: 'Room', src: '' },
            answers: 'фонарь,',
        };
        expect(evaluateAnswer(quest, '  ').correct).toBe(false);
    });
});
