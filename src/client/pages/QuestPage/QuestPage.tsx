import { Navigate } from 'react-router';
import { gamePath } from '../../utils/paths';
import { useGameRoute } from '../../hooks/useGameRoute/useGameRoute';
import { useSaveGame } from '../../hooks/useSaveGame/useSaveGame';
import type { Quest } from '../../../shared/quests';
import { QuestEditor } from '../../features/quests/QuestEditor/QuestEditor';

export function QuestPage() {
    const { game, quest } = useGameRoute();
    const { save, error } = useSaveGame();

    if (!game) {
        return <Navigate to="/" replace />;
    }
    if (!quest) {
        return <Navigate to={gamePath(game.id)} replace />;
    }

    const currentGame = game;

    const handleSave = (edited: Quest) => {
        return save({ ...currentGame, quests: { ...currentGame.quests, [edited.type]: edited } });
    };

    return (
        <>
            {error && (
                <p className="notice error" role="alert">
                    {error.message}
                </p>
            )}
            <QuestEditor
                key={`${game.id}-${quest.type}`}
                initial={quest}
                position={game.order.indexOf(quest.type) + 1}
                onSave={handleSave}
            />
        </>
    );
}
