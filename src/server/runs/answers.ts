import type { Quest } from '../../shared/quests';

export type Answer = string | string[] | { word: string; overlay: number };

function normalize(text: string) {
    return text.normalize('NFC').trim().toLocaleLowerCase('ru').replace(/ё/g, 'е').replace(/\s+/g, ' ');
}

export function evaluateAnswer(quest: Quest, answer: Answer) {
    switch (quest.type) {
        case 'nfc': {
            const tag = quest.tags.find(({ token }) => token === answer);
            const correct = tag?.id === quest.correctId;
            return { correct, consumesAttempt: Boolean(tag && !correct) };
        }
        case 'qr':
            return { correct: answer === quest.token };
        case 'photo': {
            const word = typeof answer === 'string' ? normalize(answer) : '';
            return {
                correct:
                    word.length > 0 &&
                    quest.answers.split(',').some((candidate) => normalize(candidate) === word),
            };
        }
        case 'objects':
            return {
                correct:
                    Array.isArray(answer) &&
                    answer.length === quest.answers.length &&
                    new Set(answer).size === answer.length &&
                    quest.answers.every((id) => answer.includes(id)),
            };
        case 'tree':
            return {
                correct:
                    Array.isArray(answer) &&
                    answer.length === quest.answers.length &&
                    quest.answers.every((id, index) => answer[index] === id),
            };
        case 'stencil':
            return {
                correct:
                    typeof answer === 'object' &&
                    !Array.isArray(answer) &&
                    normalize(answer.word) === normalize(quest.word) &&
                    answer.overlay === quest.correct,
            };
    }
}
