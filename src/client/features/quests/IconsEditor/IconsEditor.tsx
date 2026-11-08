import { useIntl } from 'react-intl';
import { useState } from 'react';
import type { ChangeEvent, MouseEvent } from 'react';
import { ArrowDown, ArrowUp, Check, X } from 'lucide-react';
import { useCatalog } from '../../../hooks/useCatalog/useCatalog';
import type { IconQuest, IconSlot, Picture } from '../../../../shared/quests';
import { EditorSection } from '../../../components/EditorSection/EditorSection';
import { PictureField } from '../../../components/PictureField/PictureField';

import { SlotIcon } from '../SlotIcon/SlotIcon';

interface IconsEditorProps {
    quest: IconQuest;
    onChange: (quest: IconQuest) => void;
}

export function IconsEditor({ quest, onChange }: IconsEditorProps) {
    const { formatMessage } = useIntl();
    const { icons } = useCatalog();
    const [selectedId, setSelectedId] = useState(quest.slots[0].id);
    const slotIndex = quest.slots.findIndex(({ id }) => id === selectedId);
    const slot = quest.slots[slotIndex];
    const ordered = quest.type === 'tree';
    const limit = ordered ? 4 : 3;

    const updateSlot = (changes: Partial<IconSlot>) => {
        const slots = quest.slots.map((item) => (item.id === selectedId ? { ...item, ...changes } : item));
        onChange({ ...quest, slots });
    };

    const handleSelect = (event: MouseEvent<HTMLButtonElement>) => {
        setSelectedId(event.currentTarget.value);
    };
    const handleLabel = (event: ChangeEvent<HTMLInputElement>) => {
        updateSlot({ label: event.currentTarget.value });
    };
    const handleIcon = (event: ChangeEvent<HTMLSelectElement>) => {
        const item = icons.find(({ id }) => id === event.currentTarget.value);
        if (item) {
            updateSlot({ icon: item.id, label: item.label, picture: undefined });
        }
    };
    const handlePicture = (picture: Picture) => {
        updateSlot({ picture });
    };
    const handleClearPicture = () => {
        updateSlot({ picture: undefined });
    };

    const handlePosition = (event: MouseEvent<HTMLButtonElement>) => {
        const target = slotIndex + Number(event.currentTarget.value);
        if (target < 0 || target >= quest.slots.length) {
            return;
        }
        const slots = [...quest.slots];
        [slots[slotIndex], slots[target]] = [slots[target], slots[slotIndex]];
        onChange({ ...quest, slots });
    };

    const handleAnswer = (event: MouseEvent<HTMLButtonElement>) => {
        const id = event.currentTarget.value;
        if (quest.answers.includes(id)) {
            onChange({ ...quest, answers: quest.answers.filter((answer) => answer !== id) });
        } else if (quest.answers.length < limit) {
            onChange({ ...quest, answers: [...quest.answers, id] });
        }
    };

    const handleAnswerOrder = (event: MouseEvent<HTMLButtonElement>) => {
        const index = Number(event.currentTarget.value);
        const target = index + Number(event.currentTarget.dataset.direction);
        if (target < 0 || target >= quest.answers.length) {
            return;
        }
        const answers = [...quest.answers];
        [answers[index], answers[target]] = [answers[target], answers[index]];
        onChange({ ...quest, answers });
    };

    return (
        <div className="editor-grid">
            <EditorSection
                title={formatMessage({ id: 'icons.keyboard' })}
                detail={formatMessage({ id: 'icons.keyboardDetail' })}
            >
                <div className="icon-grid">
                    {quest.slots.map((item, index) => (
                        <button
                            type="button"
                            key={item.id}
                            value={item.id}
                            onClick={handleSelect}
                            aria-pressed={item.id === selectedId}
                            className="icon-tile"
                        >
                            <span className="slot-index">{index + 1}</span>
                            <SlotIcon slot={item} />
                            <span>{item.label || formatMessage({ id: 'icons.unnamed' })}</span>
                        </button>
                    ))}
                </div>
                <div className="slot-editor" key={slot.id}>
                    <div className="section-line">
                        <h3>{formatMessage({ id: 'icons.slot' }, { number: slotIndex + 1 })}</h3>
                        <div className="actions">
                            <button
                                className="icon-button"
                                type="button"
                                value="-1"
                                aria-label={formatMessage({ id: 'icons.moveEarlier' })}
                                onClick={handlePosition}
                                disabled={slotIndex === 0}
                            >
                                <ArrowUp size={17} />
                            </button>
                            <button
                                className="icon-button"
                                type="button"
                                value="1"
                                aria-label={formatMessage({ id: 'icons.moveLater' })}
                                onClick={handlePosition}
                                disabled={slotIndex === quest.slots.length - 1}
                            >
                                <ArrowDown size={17} />
                            </button>
                        </div>
                    </div>
                    <label className="field">
                        <span>{formatMessage({ id: 'icons.name' })}</span>
                        <input value={slot.label} onChange={handleLabel} maxLength={32} />
                    </label>
                    <label className="field">
                        <span>{formatMessage({ id: 'icons.library' })}</span>
                        <select value={slot.icon} onChange={handleIcon}>
                            {icons
                                .filter(({ id }) => ordered || id !== 'cloud')
                                .map(({ id, label }) => (
                                    <option value={id} key={id}>
                                        {label}
                                    </option>
                                ))}
                        </select>
                    </label>
                    <PictureField
                        label={formatMessage({ id: 'icons.upload' })}
                        picture={slot.picture}
                        onChange={handlePicture}
                        compact
                    />
                    {slot.picture && (
                        <button type="button" className="button quiet" onClick={handleClearPicture}>
                            <X size={16} /> {formatMessage({ id: 'icons.restore' })}
                        </button>
                    )}
                </div>
            </EditorSection>
            <EditorSection
                title={
                    ordered ? formatMessage({ id: 'icons.order' }) : formatMessage({ id: 'icons.correct' })
                }
                detail={
                    ordered
                        ? formatMessage({ id: 'icons.orderDetail' })
                        : formatMessage({ id: 'icons.correctDetail' })
                }
            >
                <p className="badge count">
                    {formatMessage({ id: 'icons.selection' }, { count: quest.answers.length, limit })}
                </p>
                <div className="choice-list">
                    {quest.slots.map((item) => {
                        const index = quest.answers.indexOf(item.id);
                        return (
                            <button
                                type="button"
                                className="choice"
                                key={item.id}
                                value={item.id}
                                onClick={handleAnswer}
                                aria-pressed={index >= 0}
                                disabled={index < 0 && quest.answers.length >= limit}
                            >
                                <SlotIcon slot={item} size={18} />
                                <span>{item.label || formatMessage({ id: 'icons.unnamed' })}</span>
                                {index >= 0 && (ordered ? <strong>{index + 1}</strong> : <Check size={16} />)}
                            </button>
                        );
                    })}
                </div>
                {ordered && (
                    <ol className="answer-sequence">
                        {quest.answers.map((id, index) => (
                            <li key={id}>
                                <span>
                                    <strong>{index + 1}</strong>{' '}
                                    {quest.slots.find((item) => item.id === id)?.label ||
                                        formatMessage({ id: 'icons.unnamed' })}
                                </span>
                                <div className="actions">
                                    <button
                                        className="icon-button"
                                        type="button"
                                        aria-label={formatMessage(
                                            { id: 'icons.raise' },
                                            { number: index + 1 },
                                        )}
                                        value={index}
                                        data-direction="-1"
                                        onClick={handleAnswerOrder}
                                        disabled={index === 0}
                                    >
                                        <ArrowUp size={16} />
                                    </button>
                                    <button
                                        className="icon-button"
                                        type="button"
                                        aria-label={formatMessage(
                                            { id: 'icons.lower' },
                                            { number: index + 1 },
                                        )}
                                        value={index}
                                        data-direction="1"
                                        onClick={handleAnswerOrder}
                                        disabled={index === quest.answers.length - 1}
                                    >
                                        <ArrowDown size={16} />
                                    </button>
                                </div>
                            </li>
                        ))}
                    </ol>
                )}
            </EditorSection>
        </div>
    );
}
