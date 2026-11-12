import { createBrowserRouter, Navigate } from 'react-router';
import { GamesPage } from '../../pages/GamesPage/GamesPage';
import { GamePage } from '../../pages/GamePage/GamePage';
import { LoginPage } from '../../pages/LoginPage/LoginPage';
import { ProgressPage } from '../../pages/ProgressPage/ProgressPage';
import { QuestPage } from '../../pages/QuestPage/QuestPage';
import { PlayPage } from '../../pages/PlayPage/PlayPage';
import { AppLayout } from '../AppLayout/AppLayout';

export function createRouter(authenticated: boolean, importError?: string) {
    return createBrowserRouter([
        { path: 'play', Component: PlayPage },
        {
            element: <AppLayout authenticated={authenticated} importError={importError} />,
            children: [
                { index: true, Component: GamesPage },
                { path: 'login', id: 'login', Component: LoginPage },
                {
                    path: ':gameId',
                    children: [
                        { index: true, Component: GamePage },
                        { path: 'progress', id: 'progress', Component: ProgressPage },
                        { path: ':questType', Component: QuestPage },
                    ],
                },
                { path: '*', element: <Navigate to="/" replace /> },
            ],
        },
    ]);
}
