import { useIntl } from 'react-intl';
import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { Copy, Nfc, Plus, ScanLine, Trash2 } from 'lucide-react';
import { makeTag } from '../../../../shared/tokens';
import type { NfcQuest, NfcTag, Picture } from '../../../../shared/quests';
import { EditorSection } from '../../../components/EditorSection/EditorSection';
import { PictureField } from '../../../components/PictureField/PictureField';
import { canUseNfc, readNfc, writeNfc } from '../../../utils/nfc';

interface NfcEditorProps {
    quest: NfcQuest;
    onChange: (quest: NfcQuest) => void;
    onPrepare: () => void;
}

export function NfcEditor({ quest, onChange, onPrepare }: NfcEditorProps) {
    const { formatMessage } = useIntl();
    const [status, setStatus] = useState('');
    const [pending, setPending] = useState(false);
    const operation = useRef<AbortController>(null);

    useEffect(() => () => operation.current?.abort(), []);

    const handleLimit = (event: ChangeEvent<HTMLInputElement>) => {
        const { valueAsNumber } = event.currentTarget;
        if (Number.isFinite(valueAsNumber)) {
            onChange({ ...quest, limit: Math.min(20, Math.max(1, Math.trunc(valueAsNumber))) });
        }
    };

    const handlePicture = (hintPicture: Picture) => {
        onChange({ ...quest, hintPicture });
    };
    const handleAdd = () => {
        onChange({ ...quest, tags: [...quest.tags, makeTag(formatMessage({ id: 'nfc.newClue' }))] });
    };
    const handleCorrect = (event: ChangeEvent<HTMLInputElement>) => {
        onChange({ ...quest, correctId: event.currentTarget.value });
    };

    const handleRename = (event: ChangeEvent<HTMLInputElement>) => {
        const { name, value } = event.currentTarget;
        onChange({
            ...quest,
            tags: quest.tags.map((tag) => (tag.id === name ? { ...tag, label: value } : tag)),
        });
    };

    const handleRemove = (event: React.MouseEvent<HTMLButtonElement>) => {
        const { value } = event.currentTarget;
        if (value !== quest.correctId) {
            onChange({ ...quest, tags: quest.tags.filter((tag) => tag.id !== value) });
        }
    };

    const handleCopy = async (event: React.MouseEvent<HTMLButtonElement>) => {
        try {
            onPrepare();
            await navigator.clipboard.writeText(event.currentTarget.value);
            setStatus(formatMessage({ id: 'nfc.copied' }));
        } catch {
            setStatus(formatMessage({ id: 'nfc.copyManually' }));
        }
    };

    const handleNfc = async (event: React.MouseEvent<HTMLButtonElement>) => {
        if (operation.current) {
            return;
        }
        const { value, dataset } = event.currentTarget;
        const tag = quest.tags.find(({ id }) => id === value);
        if (!tag) {
            return;
        }
        const controller = new AbortController();
        operation.current = controller;
        setPending(true);
        setStatus(formatMessage({ id: 'nfc.bringTag' }, { label: tag.label }));
        const timeout = window.setTimeout(() => controller.abort(), 25_000);
        try {
            if (dataset.operation === 'write') {
                onPrepare();
                await writeNfc(tag.token, controller.signal);
                setStatus(formatMessage({ id: 'nfc.written' }, { label: tag.label }));
            } else {
                const value = await readNfc(controller.signal);
                setStatus(
                    value === tag.token
                        ? formatMessage({ id: 'nfc.read' }, { label: tag.label })
                        : formatMessage({ id: 'nfc.wrongTag' }),
                );
            }
        } catch (error) {
            setStatus(
                controller.signal.aborted
                    ? formatMessage({ id: 'nfc.canceled' })
                    : formatMessage(
                          { id: 'nfc.failed' },
                          {
                              detail:
                                  error instanceof Error
                                      ? error.message
                                      : formatMessage({ id: 'nfc.checkAccess' }),
                          },
                      ),
            );
        } finally {
            window.clearTimeout(timeout);
            controller.abort();
            operation.current = null;
            setPending(false);
        }
    };

    const handleCancel = () => {
        operation.current?.abort();
    };

    const renderTag = (tag: NfcTag) => {
        const correct = tag.id === quest.correctId;
        return (
            <div className="nfc-row" key={tag.id}>
                <div className="nfc-title">
                    <label className="field">
                        <span>{formatMessage({ id: 'nfc.tagName' })}</span>
                        <input
                            aria-label={formatMessage({ id: 'nfc.tagName' })}
                            name={tag.id}
                            value={tag.label}
                            onChange={handleRename}
                        />
                    </label>
                    <label className="radio-label">
                        <input
                            type="radio"
                            name="correct-tag"
                            value={tag.id}
                            checked={correct}
                            onChange={handleCorrect}
                        />
                        {correct ? formatMessage({ id: 'nfc.correct' }) : formatMessage({ id: 'nfc.decoy' })}
                    </label>
                    <button
                        className="icon-button"
                        type="button"
                        aria-label={formatMessage({ id: 'nfc.deleteTag' }, { label: tag.label })}
                        value={tag.id}
                        onClick={handleRemove}
                        disabled={correct || pending}
                    >
                        <Trash2 size={17} />
                    </button>
                </div>
                <div className="token-row">
                    <input
                        aria-label={formatMessage({ id: 'nfc.tagToken' }, { label: tag.label })}
                        className="token"
                        readOnly
                        value={tag.token}
                    />
                    <button
                        className="icon-button"
                        type="button"
                        aria-label={formatMessage({ id: 'nfc.copyToken' }, { label: tag.label })}
                        value={tag.token}
                        onClick={handleCopy}
                    >
                        <Copy size={17} />
                    </button>
                </div>
                <div className="actions">
                    <button
                        className="button secondary"
                        type="button"
                        data-operation="write"
                        value={tag.id}
                        onClick={handleNfc}
                        disabled={!canUseNfc || pending}
                    >
                        <Nfc size={16} /> {formatMessage({ id: 'nfc.write' })}
                    </button>
                    <button
                        className="button quiet"
                        type="button"
                        data-operation="read"
                        value={tag.id}
                        onClick={handleNfc}
                        disabled={!canUseNfc || pending}
                    >
                        <ScanLine size={16} /> {formatMessage({ id: 'common.check' })}
                    </button>
                </div>
            </div>
        );
    };

    return (
        <div className="editor-grid">
            <EditorSection
                title={formatMessage({ id: 'nfc.props' })}
                detail={formatMessage({ id: 'nfc.propsDetail' })}
            >
                <div className="nfc-list">{quest.tags.map(renderTag)}</div>
                <button className="button secondary" type="button" onClick={handleAdd}>
                    <Plus size={17} /> {formatMessage({ id: 'nfc.addTag' })}
                </button>
            </EditorSection>
            <div className="stack">
                <EditorSection title={formatMessage({ id: 'nfc.settings' })}>
                    <label className="field">
                        <span>{formatMessage({ id: 'nfc.attemptLimit' })}</span>
                        <input type="number" min="1" max="20" value={quest.limit} onChange={handleLimit} />
                    </label>
                    <PictureField
                        label={formatMessage({ id: 'nfc.hintPicture' })}
                        picture={quest.hintPicture}
                        onChange={handlePicture}
                    />
                    {quest.hintPicture && (
                        <img
                            className="picture-preview"
                            src={quest.hintPicture.src}
                            alt={formatMessage({ id: 'nfc.hintPicture' })}
                        />
                    )}
                </EditorSection>
                <EditorSection title={formatMessage({ id: 'nfc.howToWrite' })}>
                    <p>{formatMessage({ id: 'nfc.howToWriteDetail' })}</p>
                    {!canUseNfc && <p className="notice">{formatMessage({ id: 'nfc.browserFallback' })}</p>}
                    {status && (
                        <p className="notice" role="status">
                            {status}
                        </p>
                    )}
                    {pending && (
                        <button className="button secondary" type="button" onClick={handleCancel}>
                            {formatMessage({ id: 'nfc.cancelOperation' })}
                        </button>
                    )}
                </EditorSection>
            </div>
        </div>
    );
}
