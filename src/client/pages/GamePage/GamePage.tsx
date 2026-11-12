import { useIntl } from 'react-intl';
import type { ChangeEvent } from 'react';
import { Navigate } from 'react-router';
import { useGameRoute } from '../../hooks/useGameRoute/useGameRoute';
import { useSaveGame } from '../../hooks/useSaveGame/useSaveGame';
import { useCatalog } from '../../hooks/useCatalog/useCatalog';
import { questIcons } from '../../domain/questIcons/questIcons';
import type { QuestType } from '../../../shared/quests';
import { GameNavigation } from '../../features/admin/GameNavigation/GameNavigation';
import { QuestOrder } from '../../features/admin/QuestOrder/QuestOrder';
import { GameBriefing } from '../../features/admin/GameBriefing/GameBriefing';
import './GamePage.css';

export function GamePage() {
    const { formatMessage } = useIntl();
    const { game } = useGameRoute();
    const { save, saving, error } = useSaveGame();
    const catalog = useCatalog();

    if (!game) {
        return <Navigate to="/" replace />;
    }
    const currentGame = game;

    const handleToggle = (event: ChangeEvent<HTMLInputElement>) => {
        if (saving) {
            return;
        }
        const type = event.currentTarget.value as QuestType;
        if (event.currentTarget.checked) {
            void save(
                { ...currentGame, order: [...currentGame.order, type] },
                currentGame.quests[type] ? undefined : type,
            );
        } else {
            void save({ ...currentGame, order: currentGame.order.filter((item) => item !== type) });
        }
    };

    const handleOrder = (order: QuestType[]) => {
        if (!saving) {
            void save({ ...currentGame, order });
        }
    };

    const handleBriefing = (story: string, rules: string) => save({ ...currentGame, story, rules });

    return (
        <>
            <GameNavigation game={currentGame} showProgress={false} />
            <header className="page-title">
                <div>
                    <div className="eyebrow">{formatMessage({ id: 'game.settings' })}</div>
                    <h1>{currentGame.title}</h1>
                    <p>{formatMessage({ id: 'game.settingsDetail' })}</p>
                </div>
            </header>
            {error && (
                <p className="notice error" role="alert">
                    {error.message}
                </p>
            )}
            <div className="editor-grid">
                <section className="panel editor-section">
                    <div className="section-head">
                        <h2>{formatMessage({ id: 'game.quests' })}</h2>
                        <p>
                            {currentGame.order.length > 1 && formatMessage({ id: 'game.reorder' }) + ' '}
                            {formatMessage({ id: 'game.solveOrder' })}
                        </p>
                    </div>
                    {currentGame.order.length === 0 && (
                        <p className="notice">{formatMessage({ id: 'game.noQuests' })}</p>
                    )}
                    {currentGame.order.length > 0 && (
                        <QuestOrder
                            gameId={currentGame.id}
                            order={currentGame.order}
                            onChange={handleOrder}
                            disabled={saving}
                        />
                    )}
                </section>
                <section className="panel editor-section">
                    <div className="section-head">
                        <h2>{formatMessage({ id: 'game.catalog' })}</h2>
                        <p>{formatMessage({ id: 'game.catalogDetail' })}</p>
                    </div>
                    <div className="catalog-list">
                        {Object.values(catalog.quests).map(({ type, title, badge }) => {
                            const Icon = questIcons[type];
                            return (
                                <label key={type}>
                                    <input
                                        type="checkbox"
                                        value={type}
                                        onChange={handleToggle}
                                        checked={currentGame.order.includes(type)}
                                        disabled={saving}
                                    />
                                    <Icon size={18} />
                                    <span>
                                        <strong>{title}</strong>
                                        <small>{badge}</small>
                                    </span>
                                </label>
                            );
                        })}
                    </div>
                </section>
            </div>
            <GameBriefing
                key={currentGame.id}
                story={currentGame.story}
                rules={currentGame.rules}
                saving={saving}
                onSave={handleBriefing}
            />
        </>
    );
}
