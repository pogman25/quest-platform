import type { Quest } from '../../shared/quests';
import { stencilLetters } from './stencil';

export function validateQuest(quest: Quest) {
    if (!quest.instruction.trim() && (quest.type !== 'nfc' || !quest.hintPicture)) {
        return 'quest.requiredInstruction';
    }
    if (quest.type === 'stencil' && !stencilLetters(quest.word)) {
        return 'quest.invalidWord';
    }
    if (quest.type === 'photo' && !quest.answers.split(',').some((answer) => answer.trim())) {
        return 'quest.requiredAnswer';
    }
    if (quest.type === 'nfc' && quest.tags.some(({ label }) => !label.trim())) {
        return 'quest.requiredTagName';
    }
    if (quest.type === 'objects' || quest.type === 'tree') {
        if (quest.slots.some(({ label }) => !label.trim())) {
            return 'quest.requiredItemName';
        }
        if (quest.answers.length !== (quest.type === 'objects' ? 3 : 4)) {
            return 'quest.answerCount';
        }
    }
    return '';
}
