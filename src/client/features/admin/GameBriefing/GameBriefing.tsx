import { useState } from 'react';
import type { SubmitEvent } from 'react';
import { useIntl } from 'react-intl';
import { EditorSection } from '../../../components/EditorSection/EditorSection';

interface GameBriefingProps {
    story: string;
    rules: string;
    saving: boolean;
    onSave: (story: string, rules: string) => Promise<boolean>;
}

export function GameBriefing({ story, rules, saving, onSave }: GameBriefingProps) {
    const { formatMessage } = useIntl();
    const [saved, setSaved] = useState(false);

    const handleChange = () => {
        setSaved(false);
    };
    const handleSubmit = async (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (saving) {
            return;
        }
        const data = new FormData(event.currentTarget);
        setSaved(await onSave(String(data.get('story')), String(data.get('rules'))));
    };

    return (
        <EditorSection
            title={formatMessage({ id: 'game.briefing' })}
            detail={formatMessage({ id: 'game.briefingDetail' })}
        >
            <form className="stack" onSubmit={handleSubmit} onChange={handleChange}>
                <label className="field">
                    <span>{formatMessage({ id: 'game.story' })}</span>
                    <textarea
                        name="story"
                        defaultValue={story}
                        maxLength={10000}
                        rows={5}
                        disabled={saving}
                    />
                </label>
                <label className="field">
                    <span>{formatMessage({ id: 'game.rules' })}</span>
                    <textarea
                        name="rules"
                        defaultValue={rules}
                        maxLength={10000}
                        rows={5}
                        disabled={saving}
                    />
                </label>
                <div className="actions">
                    <button className="button primary" type="submit" disabled={saving}>
                        {formatMessage({ id: 'common.save' })}
                    </button>
                    {saved && <span role="status">{formatMessage({ id: 'quest.saved' })}</span>}
                </div>
            </form>
        </EditorSection>
    );
}
