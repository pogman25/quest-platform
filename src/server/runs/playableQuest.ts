import type { Quest } from '../../shared/quests';

export function isPlayableQuest(quest: Quest) {
    if (!quest.instruction.trim() && (quest.type !== 'nfc' || !quest.hintPicture)) {
        return false;
    }
    switch (quest.type) {
        case 'nfc':
            return (
                quest.tags.some(({ id }) => id === quest.correctId) &&
                new Set(quest.tags.map(({ token }) => token)).size === quest.tags.length
            );
        case 'photo':
            return quest.answers.split(',').some((word) => word.trim());
        case 'objects':
        case 'tree':
            return (
                quest.answers.length === (quest.type === 'objects' ? 3 : 4) &&
                new Set(quest.answers).size === quest.answers.length &&
                quest.answers.every((id) => quest.slots.some((slot) => slot.id === id))
            );
        case 'stencil': {
            const word = quest.word.trim().normalize('NFC');
            return /^[а-яёa-z]{4,12}$/i.test(word) && word.length % 2 === 0;
        }
        case 'qr':
            return true;
    }
}
