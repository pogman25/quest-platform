import { useState } from 'react';
import type { ChangeEvent, MouseEvent, SubmitEvent } from 'react';
import { useIntl } from 'react-intl';
import type { AttemptResult, PlayerQuest } from '../../../../shared/runs';
import { ElapsedTime } from '../../../components/ElapsedTime/ElapsedTime';
import { SlotIcon } from '../../quests/SlotIcon/SlotIcon';
import './PlayerQuestForm.css';

interface PlayerQuestFormProps {
    quest: PlayerQuest;
    pending: boolean;
    result?: AttemptResult;
    onAnswer: (answer: string | string[]) => void;
    onBack: () => void;
}

export function PlayerQuestForm({ quest, pending, result, onAnswer, onBack }: PlayerQuestFormProps) {
    const { formatMessage } = useIntl();
    const [word, setWord] = useState('');
    const [selection, setSelection] = useState<string[]>([]);
    const limit = quest.type === 'tree' ? 4 : 3;
    const handleWord = (event: ChangeEvent<HTMLInputElement>) => setWord(event.currentTarget.value);
    const handleSelect = (event: MouseEvent<HTMLButtonElement>) => {
        const id = event.currentTarget.value;
        setSelection((items) =>
            items.includes(id)
                ? items.filter((item) => item !== id)
                : items.length < limit
                  ? [...items, id]
                  : items,
        );
    };
    const handleSubmit = (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (!pending) {
            onAnswer(quest.slots ? selection : word);
        }
    };
    return (
        <section className="panel editor-section">
            <button className="button secondary" type="button" onClick={onBack}>
                {formatMessage({ id: 'play.back' })}
            </button>
            <h2>{quest.title}</h2>
            <ElapsedTime key={quest.startedAt} startedAt={quest.startedAt} endedAt={quest.solvedAt} />
            <p className="player-text">{quest.instruction}</p>
            {quest.picture && (
                <img className="player-picture" src={quest.picture.src} alt={quest.picture.name} />
            )}
            {quest.reward ? (
                <p className="notice">
                    {formatMessage(
                        { id: 'play.reward' },
                        { digit: quest.reward.digit, position: quest.reward.position },
                    )}
                </p>
            ) : quest.status === 'locked' ? (
                <p className="notice">{formatMessage({ id: 'progress.result.locked' })}</p>
            ) : (
                <form className="stack" onSubmit={handleSubmit}>
                    {quest.slots ? (
                        <>
                            <p>
                                {formatMessage(
                                    { id: quest.type === 'tree' ? 'play.selectOrder' : 'play.selectItems' },
                                    { count: limit },
                                )}
                            </p>
                            <div className="player-slots">
                                {quest.slots.map((slot) => (
                                    <button
                                        className="button secondary player-slot"
                                        type="button"
                                        key={slot.id}
                                        value={slot.id}
                                        onClick={handleSelect}
                                        aria-pressed={selection.includes(slot.id)}
                                        disabled={pending}
                                    >
                                        <SlotIcon slot={slot} />
                                        <span>{slot.label}</span>
                                        {selection.includes(slot.id) && (
                                            <strong>{selection.indexOf(slot.id) + 1}</strong>
                                        )}
                                    </button>
                                ))}
                            </div>
                        </>
                    ) : (
                        <label className="field">
                            <span>
                                {formatMessage({
                                    id:
                                        quest.type === 'qr' || quest.type === 'nfc'
                                            ? 'play.token'
                                            : 'play.word',
                                })}
                            </span>
                            <input
                                value={word}
                                onChange={handleWord}
                                maxLength={512}
                                required
                                disabled={pending}
                                autoComplete="off"
                            />
                        </label>
                    )}
                    {quest.remainingAttempts !== undefined && (
                        <p>{formatMessage({ id: 'play.remaining' }, { count: quest.remainingAttempts })}</p>
                    )}
                    <button
                        className="button primary"
                        type="submit"
                        disabled={pending || (quest.slots ? selection.length !== limit : !word.trim())}
                    >
                        {formatMessage({ id: 'common.check' })}
                    </button>
                    {result && result.status !== 'correct' && (
                        <p className="notice" role="status">
                            {formatMessage({ id: `progress.result.${result.status}` })}
                        </p>
                    )}
                </form>
            )}
        </section>
    );
}
