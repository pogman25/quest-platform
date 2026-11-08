import { useIntl } from 'react-intl';
import { SearchCheck } from 'lucide-react';
import { Link } from 'react-router';
import { gamePath } from '../../../utils/paths';
import type { Game } from '../../../../shared/quests';
import { ProfileMenu } from '../ProfileMenu/ProfileMenu';
import type { ProfileMenuProps } from '../ProfileMenu/ProfileMenu';
import './AdminHeader.css';

interface AdminHeaderProps extends ProfileMenuProps {
    game?: Pick<Game, 'id' | 'title'>;
    pageTitle?: string;
}

export function AdminHeader({ game, pageTitle, signedIn, onSignIn, onSignOut }: AdminHeaderProps) {
    const { formatMessage } = useIntl();
    return (
        <header className="admin-header">
            <div className="admin-header-inner">
                <div className="admin-header-navigation">
                    <span className="admin-brand">
                        <span className="admin-brand-icon">
                            <SearchCheck size={21} aria-hidden="true" />
                        </span>
                        <span>{formatMessage({ id: 'common.brand' })}</span>
                    </span>
                    {signedIn && (
                        <nav
                            className="admin-breadcrumbs"
                            aria-label={formatMessage({ id: 'app.navigation' })}
                        >
                            <Link to="/" aria-current={game ? undefined : 'page'}>
                                {formatMessage({ id: 'games.title' })}
                            </Link>
                            {game && (
                                <span className="admin-breadcrumb-item">
                                    <span aria-hidden="true">/</span>{' '}
                                    {pageTitle ? (
                                        <Link to={gamePath(game.id)}>{game.title}</Link>
                                    ) : (
                                        <span aria-current="page">{game.title}</span>
                                    )}
                                </span>
                            )}
                            {pageTitle && (
                                <span className="admin-breadcrumb-item">
                                    <span aria-hidden="true">/</span>{' '}
                                    <span aria-current="page">{pageTitle}</span>
                                </span>
                            )}
                        </nav>
                    )}
                </div>
                <ProfileMenu signedIn={signedIn} onSignIn={onSignIn} onSignOut={onSignOut} />
            </div>
        </header>
    );
}
