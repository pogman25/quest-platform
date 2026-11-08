import { useQuery } from '@tanstack/react-query';
import { useContext, useEffect, useRef } from 'react';
import { useIntl } from 'react-intl';
import { Navigate, Outlet, useLocation, useMatches } from 'react-router';
import { AdminHeader } from '../../features/admin/AdminHeader/AdminHeader';
import { gameQuery } from '../../features/admin/gameQueries/gameQueries';
import type { GameRouteData } from '../../hooks/useGameRoute/useGameRoute';
import { useSessionActions } from '../../hooks/useSessionActions/useSessionActions';
import { ApiError } from '../api/api';
import { CatalogContext } from '../CatalogContext/CatalogContext';
import { SessionContext } from '../SessionContext/SessionContext';
import './AppLayout.css';

interface AppLayoutProps {
    authenticated: boolean;
    importError?: string;
}

export function AppLayout({ authenticated, importError }: AppLayoutProps) {
    const { formatMessage } = useIntl();
    const { login, logout } = useSessionActions();
    const catalog = useContext(CatalogContext);
    const emailInput = useRef<HTMLInputElement>(null);
    const match = useMatches().at(-1);
    const { pathname, search } = useLocation();
    const gameId = authenticated ? match?.params.gameId : undefined;
    const { data: game, isPending, error } = useQuery(gameQuery(gameId));
    const questType = game?.order.find((type) => type === match?.params.questType);
    const quest = questType ? game?.quests[questType] : undefined;
    const showLogin = match?.id === 'login';
    const pageTitle = quest
        ? catalog?.quests[quest.type].title
        : game && match?.id === 'progress'
          ? formatMessage({ id: 'game.progress' })
          : undefined;
    const route: GameRouteData = { game, quest };
    const returnTo = pathname + search;

    useEffect(() => {
        window.scrollTo(0, 0);
    }, [pathname]);

    const focusLogin = () => {
        emailInput.current?.focus();
    };
    const signOut = () => {
        if (!logout.isPending) {
            logout.mutate();
        }
    };

    if ((!authenticated && !showLogin) || (error instanceof ApiError && error.status === 401)) {
        return <Navigate to="/login" state={{ returnTo }} replace />;
    }
    if (!game && error instanceof ApiError && (error.status === 400 || error.status === 404)) {
        return <Navigate to="/" replace />;
    }

    const notice =
        logout.error?.message ??
        (importError && formatMessage({ id: 'games.importError' }, { detail: importError }));
    const session = {
        emailInput,
        signIn: login.mutate,
        signInError: login.error,
        signingIn: login.isPending,
    };

    return (
        <SessionContext value={session}>
            <div className="app-shell">
                <AdminHeader
                    game={game}
                    pageTitle={pageTitle}
                    signedIn={!showLogin}
                    onSignIn={focusLogin}
                    onSignOut={signOut}
                />
                <main className={showLogin ? 'login-shell' : 'main-content'}>
                    {notice && !showLogin && (
                        <p className="notice error" role="alert">
                            {notice}
                        </p>
                    )}
                    {error && (
                        <p className="notice error" role="alert">
                            {error.message}
                        </p>
                    )}
                    {gameId && isPending && (
                        <p className="notice" role="status">
                            {formatMessage({ id: 'game.loading' })}
                        </p>
                    )}
                    {(!gameId || game) && <Outlet context={route} />}
                </main>
            </div>
        </SessionContext>
    );
}
