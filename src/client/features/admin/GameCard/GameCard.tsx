import { useIntl } from 'react-intl';
import { useState } from 'react';
import { ArrowRight, Ellipsis, Pencil, Trash2 } from 'lucide-react';
import { Link } from 'react-router';
import { gamePath } from '../../../utils/paths';
import { ActionMenu } from '../../../components/ActionMenu/ActionMenu';
import type { GameSummary } from '../../../../shared/catalog';
import { GameDialog } from '../GameDialog/GameDialog';
import type { GameAction } from '../GameDialog/GameDialog';
import './GameCard.css';

interface GameCardProps {
    game: GameSummary;
    onRename: (id: string, title: string) => void;
    onDelete: (id: string) => void;
}

export function GameCard({ game, onRename, onDelete }: GameCardProps) {
    const { formatMessage } = useIntl();
    const [action, setAction] = useState<GameAction>();

    const handleRename = (title: string) => {
        onRename(game.id, title);
    };
    const handleDelete = () => {
        onDelete(game.id);
    };
    const handleClose = () => {
        setAction(undefined);
    };

    const renderActions = (closeMenu: () => void) => {
        const handleRenameAction = () => {
            closeMenu();
            setAction('rename');
        };
        const handleDeleteAction = () => {
            closeMenu();
            setAction('delete');
        };

        return (
            <>
                <button className="action-menu-item" type="button" onClick={handleRenameAction}>
                    <Pencil size={16} aria-hidden="true" /> {formatMessage({ id: 'common.rename' })}
                </button>
                <button className="action-menu-item destructive" type="button" onClick={handleDeleteAction}>
                    <Trash2 size={16} aria-hidden="true" /> {formatMessage({ id: 'common.delete' })}
                </button>
            </>
        );
    };

    return (
        <article className="panel game-card">
            <Link className="game-card-link" to={gamePath(game.id)}>
                <span className="badge">
                    {formatMessage({ id: 'game.questCount' }, { count: game.questCount })}
                </span>
                <h2>{game.title}</h2>
                <span className="card-link">
                    {formatMessage({ id: 'game.configure' })} <ArrowRight size={18} aria-hidden="true" />
                </span>
            </Link>
            <div className="game-card-menu">
                <ActionMenu
                    label={formatMessage({ id: 'game.actions' }, { title: game.title })}
                    trigger={<Ellipsis size={20} aria-hidden="true" />}
                >
                    {renderActions}
                </ActionMenu>
            </div>
            {action && (
                <GameDialog
                    action={action}
                    gameTitle={game.title}
                    onRename={handleRename}
                    onDelete={handleDelete}
                    onClose={handleClose}
                />
            )}
        </article>
    );
}
