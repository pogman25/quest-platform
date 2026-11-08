import { useIntl } from 'react-intl';
import { useState } from 'react';
import type { ChangeEvent } from 'react';
import { Check, Save } from 'lucide-react';
import { useCatalog } from '../../../hooks/useCatalog/useCatalog';
import type { Quest } from '../../../../shared/quests';
import { EditorSection } from '../../../components/EditorSection/EditorSection';
import { PhotoEditor } from '../PhotoEditor/PhotoEditor';
import { IconsEditor } from '../IconsEditor/IconsEditor';
import { NfcEditor } from '../NfcEditor/NfcEditor';
import { QrEditor } from '../QrEditor/QrEditor';
import { StencilEditor } from '../StencilEditor/StencilEditor';
import { validateQuest } from '../../../utils/validateQuest';
import './QuestEditor.css';

interface QuestEditorProps {
    initial: Quest;
    position: number;
    onSave: (quest: Quest) => Promise<boolean>;
}

export function QuestEditor({ initial, position, onSave }: QuestEditorProps) {
    const { formatMessage } = useIntl();
    const [quest, setQuest] = useState(initial);
    const [message, setMessage] = useState('');
    const [savedQuest, setSavedQuest] = useState<Quest>();
    const [saving, setSaving] = useState(false);
    const saved = savedQuest === quest;
    const info = useCatalog().quests[quest.type];

    const handleChange = (updated: Quest) => {
        setQuest(updated);
        setMessage('');
    };
    const handleInstruction = (event: ChangeEvent<HTMLTextAreaElement>) => {
        handleChange({ ...quest, instruction: event.currentTarget.value });
    };
    const saveDraft = async () => {
        if (saving) {
            return;
        }
        setSaving(true);
        try {
            if (await onSave(quest)) {
                setSavedQuest(quest);
            }
        } finally {
            setSaving(false);
        }
    };
    const handleSave = () => {
        const problem = validateQuest(quest);
        if (problem) {
            setMessage(formatMessage({ id: problem }));
            return;
        }
        void saveDraft();
    };

    return (
        <>
            <header className="page-title">
                <div>
                    <div className="eyebrow">
                        {formatMessage(
                            { id: 'quest.position' },
                            { number: String(position).padStart(2, '0') },
                        )}{' '}
                        <span className="badge">{info.badge}</span>
                    </div>
                    <h1>{info.title}</h1>
                    <p>{info.description}</p>
                </div>
                <button className="button primary" type="button" onClick={handleSave} disabled={saving}>
                    {saved ? <Check size={18} /> : <Save size={18} />}
                    {saved ? formatMessage({ id: 'quest.saved' }) : formatMessage({ id: 'quest.save' })}
                </button>
            </header>
            {message && (
                <p className="notice error" role="alert">
                    {message}
                </p>
            )}
            {quest.type === 'nfc' && (
                <NfcEditor quest={quest} onChange={handleChange} onPrepare={saveDraft} />
            )}
            {quest.type === 'qr' && <QrEditor quest={quest} onChange={handleChange} onPrepare={saveDraft} />}
            {quest.type === 'photo' && <PhotoEditor quest={quest} onChange={handleChange} />}
            {(quest.type === 'objects' || quest.type === 'tree') && (
                <IconsEditor quest={quest} onChange={handleChange} />
            )}
            {quest.type === 'stencil' && (
                <StencilEditor quest={quest} onChange={handleChange} onPrepare={saveDraft} />
            )}
            <EditorSection title={formatMessage({ id: 'quest.playerView' })}>
                <label className="field">
                    <span>{formatMessage({ id: 'quest.androidInstruction' })}</span>
                    <textarea
                        rows={3}
                        value={quest.instruction}
                        onChange={handleInstruction}
                        maxLength={500}
                    />
                </label>
                <p className="hint">{formatMessage({ id: 'quest.reward' }, { position })}</p>
            </EditorSection>
        </>
    );
}
