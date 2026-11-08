import { useIntl } from 'react-intl';
import { Link } from 'react-router';
import { gamePath } from '../../../utils/paths';
import type { Game } from '../../../../shared/quests';
import './GameNavigation.css';

interface GameNavigationProps {
    game: Game;
    showProgress: boolean;
}

export function GameNavigation({ game, showProgress }: GameNavigationProps) {
    const { formatMessage } = useIntl();
    return (
        <nav className="game-navigation" aria-label={formatMessage({ id: 'game.navigation' })}>
            <Link
                className="nav-link"
                to={gamePath(game.id)}
                aria-current={!showProgress ? 'page' : undefined}
            >
                {formatMessage({ id: 'game.quests' })}
            </Link>
            <Link
                className="nav-link"
                to={`${gamePath(game.id)}/progress`}
                aria-current={showProgress ? 'page' : undefined}
            >
                {formatMessage({ id: 'game.progress' })}
            </Link>
        </nav>
    );
}
