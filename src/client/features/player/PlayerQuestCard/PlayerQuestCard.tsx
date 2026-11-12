import { useIntl } from 'react-intl';
import type { PlayerQuest } from '../../../../shared/runs';
import type { QuestType } from '../../../../shared/quests';
import { ElapsedTime } from '../../../components/ElapsedTime/ElapsedTime';
import './PlayerQuestCard.css';

interface PlayerQuestCardProps {
    quest: PlayerQuest;
    disabled: boolean;
    onOpen: (type: QuestType) => void;
}

export function PlayerQuestCard({ quest, disabled, onOpen }: PlayerQuestCardProps) {
    const { formatMessage } = useIntl();
    const handleOpen = () => onOpen(quest.type);
    return (
        <button className="panel player-quest-card" type="button" onClick={handleOpen} disabled={disabled}>
            <span className="section-line">
                <strong>{quest.title}</strong>
                {quest.reward && <span className="badge">{quest.reward.digit}</span>}
            </span>
            <span>{formatMessage({ id: `play.status.${quest.status}` })}</span>
            {quest.startedAt && (
                <ElapsedTime key={quest.startedAt} startedAt={quest.startedAt} endedAt={quest.solvedAt} />
            )}
        </button>
    );
}
