import { useIntl } from 'react-intl';
import { useState } from 'react';
import type { ChangeEvent, SubmitEvent } from 'react';
import { Navigate } from 'react-router';
import { useGameRoute } from '../../hooks/useGameRoute/useGameRoute';
import { useRuns } from '../../hooks/useRuns/useRuns';
import { useStartRun } from '../../hooks/useStartRun/useStartRun';
import { EditorSection } from '../../components/EditorSection/EditorSection';
import { GameNavigation } from '../../features/admin/GameNavigation/GameNavigation';
import { ProgressTimeline } from '../../features/admin/ProgressTimeline/ProgressTimeline';
import { RunInvitation } from '../../features/admin/RunInvitation/RunInvitation';
import { RunOverview } from '../../components/RunOverview/RunOverview';

export function ProgressPage() {
    const { formatDate, formatMessage } = useIntl();
    const { game } = useGameRoute();
    const [selected, setSelected] = useState('');
    const { runs, run, progress } = useRuns(game?.id, selected);
    const startRun = useStartRun();

    if (!game) {
        return <Navigate to="/" replace />;
    }
    const gameId = game.id;
    const error = startRun.error ?? runs.error ?? progress.error;

    const handleSelect = (event: ChangeEvent<HTMLSelectElement>) => {
        setSelected(event.currentTarget.value);
        if (startRun.isError) {
            startRun.reset();
        }
    };

    const handleStart = (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (startRun.isPending) {
            return;
        }
        const form = event.currentTarget;
        const data = new FormData(form);
        startRun.mutate(
            { gameId, teamName: String(data.get('teamName')), code: String(data.get('code')) },
            {
                onSuccess: (created) => {
                    setSelected(created.id);
                    form.reset();
                },
            },
        );
    };

    return (
        <>
            <GameNavigation game={game} showProgress />
            <header className="page-title">
                <div>
                    <div className="eyebrow">{formatMessage({ id: 'progress.runs' })}</div>
                    <h1>{formatMessage({ id: 'progress.title' })}</h1>
                    <p>{game.title}</p>
                </div>
            </header>
            {error && (
                <p className="notice error" role="alert">
                    {error.message}
                </p>
            )}
            <div className="editor-grid">
                <EditorSection title={formatMessage({ id: 'progress.new' })}>
                    <form className="stack" onSubmit={handleStart}>
                        <label className="field">
                            <span>{formatMessage({ id: 'progress.teamName' })}</span>
                            <input name="teamName" maxLength={80} required />
                        </label>
                        <label className="field">
                            <span>
                                {formatMessage({ id: 'progress.lockCode' }, { count: game.order.length })}
                            </span>
                            <input
                                name="code"
                                inputMode="numeric"
                                pattern={`[0-9]{${game.order.length}}`}
                                maxLength={game.order.length}
                                required
                            />
                        </label>
                        <button
                            type="submit"
                            className="button primary"
                            disabled={startRun.isPending || game.order.length === 0}
                        >
                            {formatMessage({ id: 'progress.start' })}
                        </button>
                    </form>
                </EditorSection>
                <EditorSection title={formatMessage({ id: 'progress.gameRuns' })}>
                    {run ? (
                        <>
                            <label className="field">
                                <span>{formatMessage({ id: 'progress.team' })}</span>
                                <select value={run.id} onChange={handleSelect}>
                                    {runs.data?.map(({ id, teamName, createdAt }) => (
                                        <option key={id} value={id}>
                                            {teamName} ·{' '}
                                            {formatDate(createdAt, {
                                                dateStyle: 'short',
                                                timeStyle: 'short',
                                            })}
                                        </option>
                                    ))}
                                </select>
                            </label>
                            <p>
                                {formatMessage({ id: 'progress.joinCode' })} <strong>{run.joinCode}</strong>
                            </p>
                            <RunInvitation key={run.id} run={run} />
                        </>
                    ) : runs.isPending ? (
                        <p className="hint" role="status">
                            {formatMessage({ id: 'progress.loading' })}
                        </p>
                    ) : runs.isSuccess ? (
                        <p className="hint">{formatMessage({ id: 'progress.empty' })}</p>
                    ) : null}
                </EditorSection>
            </div>
            {run && progress.isPending && (
                <p className="hint" role="status">
                    {formatMessage({ id: 'progress.loadingTimeline' })}
                </p>
            )}
            {progress.data && (
                <>
                    <RunOverview {...progress.data} />
                    <ProgressTimeline progress={progress.data} />
                </>
            )}
        </>
    );
}
