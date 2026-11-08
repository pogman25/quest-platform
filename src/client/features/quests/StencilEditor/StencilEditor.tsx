import { useIntl } from 'react-intl';
import { useEffect, useMemo, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { FileType2, Layers3, Printer, RefreshCw } from 'lucide-react';
import type { StencilFont, StencilQuest } from '../../../../shared/quests';
import { EditorSection } from '../../../components/EditorSection/EditorSection';
import { PdfButton } from '../../../components/PdfButton/PdfButton';
import {
    makeStencilKit,
    makeStencilPdf,
    previewStencil,
    stencilFonts,
    stencilHeight,
    stencilLetters,
} from '../../../utils/stencil';
import { useUploadedFont } from '../../../hooks/useUploadedFont/useUploadedFont';
import { readFont } from '../../../utils/readFont';

const overlayNumbers = [1, 2, 3, 4, 5, 6, 7, 8];

interface StencilEditorProps {
    quest: StencilQuest;
    onChange: (quest: StencilQuest) => void;
    onPrepare: () => void;
}

export function StencilEditor({ quest, onChange, onPrepare }: StencilEditorProps) {
    const { formatMessage } = useIntl();
    const [selected, setSelected] = useState(quest.correct);
    const [combined, setCombined] = useState(true);
    const [fontError, setFontError] = useState('');
    const latestQuest = useRef(quest);
    const upload = useRef(0);
    const { word, font, width, correct, customFont, strokeSeed, strokeVersion } = quest;
    const usesStrokes = strokeSeed !== undefined;
    const customReady = useUploadedFont(usesStrokes ? undefined : customFont);
    const family = font === 'custom' ? customReady && customFont?.family : stencilFonts[font].family;
    const letters = useMemo(() => stencilLetters(word), [word]);
    const kit = useMemo(() => {
        const drawing = strokeSeed !== undefined ? { strokeSeed, strokeVersion } : family ? { family } : null;
        if (!letters || !drawing) {
            return null;
        }
        const result = makeStencilKit({ ...drawing, width, correct }, letters);
        return {
            ...result,
            images: {
                left: result.left.toDataURL(),
                right: result.right.toDataURL(),
            },
        };
    }, [letters, family, width, correct, strokeSeed, strokeVersion]);
    const preview = useMemo(() => kit && previewStencil(kit, selected, combined), [kit, selected, combined]);

    useEffect(() => {
        latestQuest.current = quest;
    }, [quest]);
    useEffect(
        () => () => {
            upload.current++;
        },
        [],
    );

    const handleWord = (event: ChangeEvent<HTMLInputElement>) => {
        onChange({ ...quest, word: event.currentTarget.value.toLocaleUpperCase('ru') });
    };
    const handleFont = (event: ChangeEvent<HTMLSelectElement>) => {
        onChange({ ...quest, font: event.currentTarget.value as StencilFont });
    };
    const handleFontFile = async (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.currentTarget.files?.[0];
        event.currentTarget.value = '';
        if (!file) {
            return;
        }
        const request = ++upload.current;
        setFontError('');
        try {
            const customFont = await readFont(file);
            if (request === upload.current) {
                onChange({ ...latestQuest.current, font: 'custom', customFont });
            }
        } catch (error) {
            if (request === upload.current) {
                setFontError(
                    error instanceof Error ? error.message : formatMessage({ id: 'stencil.fontFailed' }),
                );
            }
        }
    };
    const handleWidth = (event: ChangeEvent<HTMLSelectElement>) => {
        onChange({ ...quest, width: Number(event.currentTarget.value) });
    };
    const handleCorrect = (event: ChangeEvent<HTMLSelectElement>) => {
        const correct = Number(event.currentTarget.value);
        onChange({ ...quest, correct });
        setSelected(correct);
    };
    const handleNewKit = () => {
        upload.current++;
        const [seed, number] = crypto.getRandomValues(new Uint32Array(2));
        const nextSeed = seed === strokeSeed ? (seed + 1) >>> 0 : seed;
        const otherNumbers = overlayNumbers.filter((value) => value !== correct);
        const nextCorrect = otherNumbers[number % otherNumbers.length];
        onChange({ ...quest, strokeSeed: nextSeed, strokeVersion: 2, correct: nextCorrect });
        setSelected(nextCorrect);
    };
    const handleOverlay = (event: ChangeEvent<HTMLSelectElement>) => {
        setSelected(Number(event.currentTarget.value));
    };
    const handleCombine = (event: ChangeEvent<HTMLInputElement>) => {
        setCombined(event.currentTarget.checked);
    };
    const handleDownload = async () => {
        if (!kit) {
            return;
        }
        onPrepare();
        makeStencilPdf(quest, kit).save('snow-stencils-a4.pdf');
    };

    return (
        <div className="editor-grid print-editor">
            <EditorSection
                title={formatMessage({ id: 'stencil.designer' })}
                detail={formatMessage({ id: 'stencil.designerDetail' })}
            >
                {kit && preview && (
                    <>
                        <div className="stencil-cards">
                            <figure>
                                <figcaption>
                                    {formatMessage(
                                        { id: 'stencil.cardA' },
                                        { width: width / 2, height: stencilHeight },
                                    )}
                                </figcaption>
                                <img
                                    src={kit.images.left}
                                    alt={formatMessage({ id: 'stencil.leftStrokes' })}
                                />
                            </figure>
                            <figure>
                                <figcaption>
                                    {formatMessage(
                                        { id: 'stencil.cardB' },
                                        { width: width / 2, height: stencilHeight },
                                    )}
                                </figcaption>
                                <img
                                    src={kit.images.right}
                                    alt={formatMessage({ id: 'stencil.rightStrokes' })}
                                />
                            </figure>
                        </div>
                        <div className="section-line overlay-controls">
                            <label className="field">
                                <span>{formatMessage({ id: 'stencil.check' })}</span>
                                <select value={selected} onChange={handleOverlay}>
                                    {overlayNumbers.map((number) => (
                                        <option key={number} value={number}>
                                            № {number}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <label className="radio-label">
                                <input type="checkbox" checked={combined} onChange={handleCombine} />
                                <Layers3 size={17} /> {formatMessage({ id: 'stencil.combine' })}
                            </label>
                        </div>
                        <div className="stencil-overlay">
                            <img
                                src={preview}
                                alt={
                                    combined
                                        ? formatMessage({ id: 'stencil.combinedAlt' }, { number: selected })
                                        : formatMessage({ id: 'stencil.overlayAlt' }, { number: selected })
                                }
                            />
                        </div>
                        <p className="hint">
                            {combined && selected === correct
                                ? formatMessage({ id: 'stencil.correctHint' })
                                : formatMessage({ id: 'stencil.compareHint' })}
                        </p>
                    </>
                )}
                {!letters && (
                    <p className="notice" role="status">
                        {formatMessage({ id: 'stencil.wordRule' })}
                    </p>
                )}
                {letters && !usesStrokes && !family && (
                    <p className="notice" role="status">
                        {customReady === false
                            ? formatMessage({ id: 'stencil.fontError' })
                            : formatMessage({ id: 'stencil.fontLoading' })}
                    </p>
                )}
            </EditorSection>
            <div className="stack">
                <EditorSection title={formatMessage({ id: 'stencil.format' })}>
                    <label className="field">
                        <span>{formatMessage({ id: 'stencil.word' })}</span>
                        <input value={word} onChange={handleWord} maxLength={12} aria-invalid={!letters} />
                    </label>
                    {!usesStrokes && (
                        <>
                            <label className="field">
                                <span>{formatMessage({ id: 'stencil.font' })}</span>
                                <select value={font} onChange={handleFont}>
                                    {Object.entries(stencilFonts).map(([key, { labelId }]) => (
                                        <option key={key} value={key}>
                                            {formatMessage({ id: labelId })}
                                        </option>
                                    ))}
                                    {customFont && <option value="custom">{customFont.name}</option>}
                                </select>
                            </label>
                            <label className="upload">
                                <FileType2 size={18} />
                                <span>
                                    <strong>{formatMessage({ id: 'stencil.uploadFont' })}</strong>
                                    <small>{formatMessage({ id: 'stencil.fontFormats' })}</small>
                                </span>
                                <input type="file" accept=".ttf,.otf" onChange={handleFontFile} />
                            </label>
                            {fontError && (
                                <small className="error" role="alert">
                                    {fontError}
                                </small>
                            )}
                        </>
                    )}
                    <label className="field">
                        <span>{formatMessage({ id: 'stencil.width' })}</span>
                        <select value={width} onChange={handleWidth}>
                            <option value="144">
                                {formatMessage({ id: 'stencil.widthOption' }, { size: 144 })}
                            </option>
                            <option value="168">
                                {formatMessage({ id: 'stencil.widthOption' }, { size: 168 })}
                            </option>
                            <option value="184">
                                {formatMessage({ id: 'stencil.widthOption' }, { size: 184 })}
                            </option>
                        </select>
                    </label>
                    <label className="field">
                        <span>{formatMessage({ id: 'stencil.correctNumber' })}</span>
                        <select value={correct} onChange={handleCorrect}>
                            {overlayNumbers.map((number) => (
                                <option key={number} value={number}>
                                    {number}
                                </option>
                            ))}
                        </select>
                    </label>
                    <button
                        type="button"
                        className="button secondary"
                        onClick={handleNewKit}
                        disabled={!letters}
                    >
                        <RefreshCw size={16} aria-hidden="true" /> {formatMessage({ id: 'stencil.newKit' })}
                    </button>
                    {strokeVersion !== 2 && (
                        <p className="hint">{formatMessage({ id: 'stencil.newKitDetail' })}</p>
                    )}
                </EditorSection>
                <EditorSection title={formatMessage({ id: 'stencil.print' })}>
                    <div className="print-hint">
                        <Printer size={20} />
                        <p>
                            <strong>{formatMessage({ id: 'stencil.printFormat' })}</strong>
                            <br />
                            {formatMessage({ id: 'stencil.printDetail' })}
                        </p>
                    </div>
                    <PdfButton onDownload={handleDownload} disabled={!kit} />
                </EditorSection>
            </div>
        </div>
    );
}
