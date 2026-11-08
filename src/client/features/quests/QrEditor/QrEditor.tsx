import { useIntl } from 'react-intl';
import { useMemo } from 'react';
import type { ChangeEvent } from 'react';
import { Printer, RefreshCw } from 'lucide-react';
import { makeToken } from '../../../../shared/tokens';
import type { Picture, QrQuest } from '../../../../shared/quests';
import { EditorSection } from '../../../components/EditorSection/EditorSection';
import { PdfButton } from '../../../components/PdfButton/PdfButton';
import { PictureField } from '../../../components/PictureField/PictureField';
import { makeQr, makeQrPdf } from '../../../utils/qr';

interface QrEditorProps {
    quest: QrQuest;
    onChange: (quest: QrQuest) => void;
    onPrepare: () => void;
}

export function QrEditor({ quest, onChange, onPrepare }: QrEditorProps) {
    const { formatMessage } = useIntl();
    const qr = useMemo(() => makeQr(quest.token), [quest.token]);

    const handlePicture = (picture: Picture) => {
        onChange({ ...quest, picture });
    };
    const handleFit = (event: ChangeEvent<HTMLSelectElement>) => {
        onChange({ ...quest, fit: event.currentTarget.value === 'contain' ? 'contain' : 'cover' });
    };
    const handleSize = (event: ChangeEvent<HTMLSelectElement>) => {
        onChange({ ...quest, size: Number(event.currentTarget.value) });
    };
    const handleRegenerate = () => {
        onChange({ ...quest, token: makeToken('qr') });
    };
    const handleDownload = async () => {
        onPrepare();
        (await makeQrPdf(quest)).save('qr-puzzle-duplex-a4.pdf');
    };

    return (
        <div className="editor-grid print-editor">
            <EditorSection
                title={formatMessage({ id: 'qr.twoSides' })}
                detail={formatMessage({ id: 'qr.twoSidesDetail' })}
            >
                <div className="paper-spread">
                    <figure>
                        <figcaption>
                            <span>01</span> {formatMessage({ id: 'qr.front' })}
                        </figcaption>
                        <div className="paper">
                            <div className="puzzle-square" style={{ width: `${(quest.size / 210) * 100}%` }}>
                                <img
                                    src={quest.picture.src}
                                    alt={formatMessage({ id: 'qr.frontAlt' })}
                                    style={{ objectFit: quest.fit }}
                                />
                                <div className="puzzle-cut" aria-hidden="true" />
                            </div>
                        </div>
                    </figure>
                    <figure>
                        <figcaption>
                            <span>02</span> {formatMessage({ id: 'qr.back' })}
                        </figcaption>
                        <div className="paper">
                            <div
                                className="puzzle-square qr-square"
                                style={{
                                    width: `${(quest.size / 210) * 100}%`,
                                    padding: `${(8 / 210) * 100}%`,
                                }}
                            >
                                <svg
                                    viewBox={`0 0 ${qr.size} ${qr.size}`}
                                    shapeRendering="crispEdges"
                                    role="img"
                                    aria-label={formatMessage({ id: 'qr.clueCode' })}
                                >
                                    <path d={qr.path} fill="#000" />
                                </svg>
                            </div>
                        </div>
                    </figure>
                </div>
            </EditorSection>
            <div className="stack">
                <EditorSection title={formatMessage({ id: 'qr.pictureAndCode' })}>
                    <PictureField
                        label={formatMessage({ id: 'qr.choosePicture' })}
                        picture={quest.picture}
                        onChange={handlePicture}
                    />
                    <label className="field">
                        <span>{formatMessage({ id: 'qr.pictureFit' })}</span>
                        <select value={quest.fit} onChange={handleFit}>
                            <option value="cover">{formatMessage({ id: 'qr.cover' })}</option>
                            <option value="contain">{formatMessage({ id: 'qr.contain' })}</option>
                        </select>
                    </label>
                    <label className="field">
                        <span>{formatMessage({ id: 'qr.size' })}</span>
                        <select value={quest.size} onChange={handleSize}>
                            <option value="120">
                                {formatMessage({ id: 'qr.sizeOption' }, { size: 120 })}
                            </option>
                            <option value="150">
                                {formatMessage({ id: 'qr.sizeOption' }, { size: 150 })}
                            </option>
                            <option value="180">
                                {formatMessage({ id: 'qr.sizeOption' }, { size: 180 })}
                            </option>
                        </select>
                    </label>
                    <label className="field">
                        <span>{formatMessage({ id: 'qr.token' })}</span>
                        <input className="token" value={quest.token} readOnly />
                    </label>
                    <button className="button secondary" type="button" onClick={handleRegenerate}>
                        <RefreshCw size={16} /> {formatMessage({ id: 'qr.regenerate' })}
                    </button>
                </EditorSection>
                <EditorSection title={formatMessage({ id: 'qr.print' })}>
                    <div className="print-hint">
                        <Printer size={20} />
                        <p>
                            <strong>{formatMessage({ id: 'qr.printFormat' })}</strong>
                            <br />
                            {formatMessage({ id: 'qr.printDetail' })}
                        </p>
                    </div>
                    <PdfButton onDownload={handleDownload} />
                </EditorSection>
            </div>
        </div>
    );
}
