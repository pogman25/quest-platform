import { useIntl } from 'react-intl';
import type { ChangeEvent } from 'react';
import { EditorSection } from '../../../components/EditorSection/EditorSection';
import { PictureField } from '../../../components/PictureField/PictureField';
import type { Picture, PhotoQuest } from '../../../../shared/quests';

interface PhotoEditorProps {
    quest: PhotoQuest;
    onChange: (quest: PhotoQuest) => void;
}

export function PhotoEditor({ quest, onChange }: PhotoEditorProps) {
    const { formatMessage } = useIntl();
    const handlePicture = (picture: Picture) => {
        onChange({ ...quest, picture });
    };
    const handleAnswers = (event: ChangeEvent<HTMLInputElement>) => {
        onChange({ ...quest, answers: event.currentTarget.value });
    };

    return (
        <div className="editor-grid">
            <EditorSection title={formatMessage({ id: 'photo.playerPhoto' })}>
                <img
                    className="picture-preview"
                    src={quest.picture.src}
                    alt={formatMessage({ id: 'photo.room' })}
                />
            </EditorSection>
            <EditorSection title={formatMessage({ id: 'photo.settings' })}>
                <PictureField
                    label={formatMessage({ id: 'photo.chooseRoom' })}
                    picture={quest.picture}
                    onChange={handlePicture}
                />
                <label className="field">
                    <span>{formatMessage({ id: 'photo.answers' })}</span>
                    <input value={quest.answers} onChange={handleAnswers} />
                </label>
                <p className="hint">{formatMessage({ id: 'photo.hint' })}</p>
            </EditorSection>
        </div>
    );
}
