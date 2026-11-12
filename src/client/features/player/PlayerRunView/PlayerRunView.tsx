import { useIntl } from 'react-intl';
import type { QuestType } from '../../../../shared/quests';
import type { AttemptResult, PlayerRun } from '../../../../shared/runs';
import { EditorSection } from '../../../components/EditorSection/EditorSection';
import { RunOverview } from '../../../components/RunOverview/RunOverview';
import { PlayerQuestCard } from '../PlayerQuestCard/PlayerQuestCard';
import { PlayerQuestForm } from '../PlayerQuestForm/PlayerQuestForm';

interface PlayerRunViewProps {
    run: PlayerRun;
    selected?: QuestType;
    starting: boolean;
    opening: boolean;
    submitting: boolean;
    result?: AttemptResult;
    onStart: () => void;
    onOpen: (type: QuestType) => void;
    onBack: () => void;
    onAnswer: (answer: string | string[]) => void;
}

export function PlayerRunView({
    run,
    selected,
    starting,
    opening,
    submitting,
    result,
    onStart,
    onOpen,
    onBack,
    onAnswer,
}: PlayerRunViewProps) {
    const { formatMessage } = useIntl();
    const quest = run.quests.find(({ type }) => type === selected);
    if (!run.startedAt) {
        return (
            <>
                <EditorSection title={formatMessage({ id: 'game.story' })}>
                    <p className="player-text">{run.story || formatMessage({ id: 'play.noStory' })}</p>
                </EditorSection>
                <EditorSection title={formatMessage({ id: 'game.rules' })}>
                    <p className="player-text">{run.rules || formatMessage({ id: 'play.noRules' })}</p>
                </EditorSection>
                <button className="button primary" type="button" onClick={onStart} disabled={starting}>
                    {formatMessage({ id: 'play.start' })}
                </button>
            </>
        );
    }
    return (
        <>
            <RunOverview {...run} />
            {quest && !run.finished ? (
                <PlayerQuestForm
                    key={`${run.id}:${quest.type}`}
                    quest={quest}
                    pending={submitting}
                    result={result}
                    onAnswer={onAnswer}
                    onBack={onBack}
                />
            ) : (
                <div className="stack">
                    {run.quests.map((item) => (
                        <PlayerQuestCard
                            key={item.type}
                            quest={item}
                            onOpen={onOpen}
                            disabled={opening || submitting || run.finished}
                        />
                    ))}
                </div>
            )}
            <details>
                <summary>{formatMessage({ id: 'game.briefing' })}</summary>
                <div className="stack">
                    <p className="player-text">{run.story}</p>
                    <p className="player-text">{run.rules}</p>
                </div>
            </details>
        </>
    );
}
