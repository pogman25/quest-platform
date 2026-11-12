import { useIntl } from 'react-intl';
import type { PlayerQuest, RunResult } from '../../../shared/runs';
import { formatDuration } from '../../utils/formatDuration';
import { EditorSection } from '../EditorSection/EditorSection';
import { ElapsedTime } from '../ElapsedTime/ElapsedTime';
import './RunOverview.css';

interface RunOverviewProps {
    startedAt: string | null;
    finishedAt: string | null;
    quests: Pick<PlayerQuest, 'position' | 'reward'>[];
    result?: RunResult;
}

export function RunOverview({ startedAt, finishedAt, quests, result }: RunOverviewProps) {
    const { formatMessage } = useIntl();
    return (
        <EditorSection title={formatMessage({ id: result ? 'play.finished' : 'play.collectedCode' })}>
            <div className="run-code" aria-label={formatMessage({ id: 'play.collectedCode' })}>
                {quests.map(({ position, reward }) => (
                    <span key={position}>{reward?.digit ?? '·'}</span>
                ))}
            </div>
            <p>
                {formatMessage({ id: startedAt ? 'play.totalTime' : 'play.notStarted' })}{' '}
                {startedAt && <ElapsedTime key={startedAt} startedAt={startedAt} endedAt={finishedAt} />}
            </p>
            {result && (
                <p>
                    {formatMessage(
                        { id: 'play.slowest' },
                        {
                            title: result.slowestQuest.title,
                            duration: formatDuration(result.slowestQuest.durationSeconds),
                        },
                    )}
                </p>
            )}
        </EditorSection>
    );
}
