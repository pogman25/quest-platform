import { useState } from 'react';
import type { SubmitEvent } from 'react';
import { useIntl } from 'react-intl';
import { useSearchParams } from 'react-router';
import type { QuestType } from '../../../shared/quests';
import { EditorSection } from '../../components/EditorSection/EditorSection';
import { PlayerRunView } from '../../features/player/PlayerRunView/PlayerRunView';
import { usePlayerRun } from '../../hooks/usePlayerRun/usePlayerRun';
import './PlayPage.css';

export function PlayPage() {
    const { formatMessage } = useIntl();
    const [params, setParams] = useSearchParams();
    const code = (params.get('code') ?? '').trim().toUpperCase();
    const { run, session, start, open, submit, reconnect } = usePlayerRun(code);
    const [selected, setSelected] = useState<QuestType>();
    const state = run.data;
    const error = session.error ?? run.error ?? start.error ?? open.error ?? submit.error;

    const handleJoin = (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();
        const data = new FormData(event.currentTarget);
        setSelected(undefined);
        setParams({ code: String(data.get('code')).trim() });
    };
    const handleStart = () => {
        if (!start.isPending) {
            start.mutate();
        }
    };
    const handleOpen = (type: QuestType) => {
        submit.reset();
        open.mutate(type, { onSuccess: () => setSelected(type) });
    };
    const handleBack = () => setSelected(undefined);
    const handleAnswer = (answer: string | string[]) => {
        if (selected && !submit.isPending) {
            submit.mutate({ type: selected, answer });
        }
    };

    return (
        <main className="player-shell stack">
            <header className="stack">
                <div className="eyebrow">{formatMessage({ id: 'common.brand' })}</div>
                <h1>{state?.gameTitle ?? formatMessage({ id: 'play.title' })}</h1>
                {state && <p>{state.teamName}</p>}
            </header>
            {error && (
                <div className="notice error stack" role="alert">
                    <p>{error.message}</p>
                    <button className="button secondary" type="button" onClick={reconnect}>
                        {formatMessage({ id: 'play.reconnect' })}
                    </button>
                </div>
            )}
            {!/^[0-9a-f]{16}$/i.test(code) ? (
                <EditorSection title={formatMessage({ id: 'play.join' })}>
                    <form className="stack" onSubmit={handleJoin}>
                        <label className="field">
                            <span>{formatMessage({ id: 'progress.joinCode' })}</span>
                            <input
                                name="code"
                                defaultValue={code}
                                pattern="[0-9a-fA-F]{16}"
                                maxLength={16}
                                required
                            />
                        </label>
                        <button className="button primary" type="submit">
                            {formatMessage({ id: 'play.join' })}
                        </button>
                    </form>
                </EditorSection>
            ) : !state ? (
                <p role="status">{formatMessage({ id: 'play.loading' })}</p>
            ) : (
                <PlayerRunView
                    run={state}
                    selected={selected}
                    starting={start.isPending}
                    opening={open.isPending}
                    submitting={submit.isPending}
                    result={submit.variables?.type === selected ? submit.data : undefined}
                    onStart={handleStart}
                    onOpen={handleOpen}
                    onBack={handleBack}
                    onAnswer={handleAnswer}
                />
            )}
        </main>
    );
}
