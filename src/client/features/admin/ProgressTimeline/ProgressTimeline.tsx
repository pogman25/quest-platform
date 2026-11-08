import { useIntl } from 'react-intl';
import { CheckCircle2, Clock3 } from 'lucide-react';
import type { RunProgress } from '../../../../shared/runs';
import { EditorSection } from '../../../components/EditorSection/EditorSection';
import { useCatalog } from '../../../hooks/useCatalog/useCatalog';
import { formatDuration } from '../../../utils/formatDuration';
import './ProgressTimeline.css';

interface ProgressTimelineProps {
    progress: RunProgress;
}

export function ProgressTimeline({ progress }: ProgressTimelineProps) {
    const { formatTime, formatMessage } = useIntl();
    const { quests: catalog } = useCatalog();
    const solved = progress.quests.filter(({ solvedAt }) => solvedAt).length;
    return (
        <EditorSection
            title={formatMessage({ id: 'progress.timeline' })}
            detail={formatMessage({ id: 'progress.solvedCount' }, { solved, total: progress.quests.length })}
        >
            {progress.quests.map(({ type, startedAt, solvedAt, durationSeconds, attempts }) => (
                <div key={type} className="progress-row">
                    <div className="section-line">
                        <strong>{catalog[type].title}</strong>
                        {durationSeconds === null ? (
                            <span className="badge neutral">
                                <Clock3 size={15} /> {formatMessage({ id: 'progress.notOpened' })}
                            </span>
                        ) : (
                            <span className="badge">
                                <CheckCircle2 size={15} className={solvedAt ? undefined : 'dim'} />
                                {solvedAt
                                    ? formatMessage({ id: 'progress.passed' })
                                    : formatMessage({ id: 'progress.inProgress' })}{' '}
                                · {formatDuration(durationSeconds)}
                            </span>
                        )}
                    </div>
                    {startedAt && (
                        <details>
                            <summary>
                                {formatMessage({ id: 'progress.attemptCount' }, { count: attempts.length })}
                            </summary>
                            <ul>
                                <li>
                                    {formatTime(startedAt, {
                                        hour: '2-digit',
                                        minute: '2-digit',
                                        second: '2-digit',
                                    })}{' '}
                                    · {formatMessage({ id: 'progress.opened' })}
                                </li>
                                {attempts.map(({ at, status }, index) => (
                                    <li key={`${at}-${index}`}>
                                        {formatTime(at, {
                                            hour: '2-digit',
                                            minute: '2-digit',
                                            second: '2-digit',
                                        })}{' '}
                                        · {formatMessage({ id: `progress.result.${status}` })}
                                    </li>
                                ))}
                            </ul>
                        </details>
                    )}
                </div>
            ))}
        </EditorSection>
    );
}
