import { useQuery } from '@tanstack/react-query';
import { useIntl } from 'react-intl';
import { useState } from 'react';
import type { ChangeEvent, SubmitEvent } from 'react';
import { Plus } from 'lucide-react';
import { Navigate, useNavigate } from 'react-router';
import { ApiError } from '../../app/api/api';
import { gamesQuery } from '../../features/admin/gameQueries/gameQueries';
import { useGameListMutation } from '../../hooks/useGameListMutation/useGameListMutation';
import { gamePath } from '../../utils/paths';
import { makeToken } from '../../../shared/tokens';
import { GameCard } from '../../features/admin/GameCard/GameCard';
import './GamesPage.css';

export function GamesPage() {
    const { formatMessage } = useIntl();
    const navigate = useNavigate();
    const { data: games, error: loadError, isPending } = useQuery(gamesQuery);
    const { mutate, mutateAsync, error: changeError, isPending: pending } = useGameListMutation();
    const [creating, setCreating] = useState(false);
    const [title, setTitle] = useState('');

    const handleRename = (id: string, title: string) => {
        mutate({ type: 'rename', id, title });
    };

    const handleDelete = (id: string) => {
        mutate({ type: 'delete', id });
    };

    const handleNew = () => {
        setCreating(true);
    };
    const handleTitle = (event: ChangeEvent<HTMLInputElement>) => {
        setTitle(event.currentTarget.value);
    };
    const handleSubmit = async (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();
        const name = title.trim();
        if (name && !pending) {
            const id = makeToken('id');
            try {
                await mutateAsync({
                    type: 'create',
                    draft: { id, title: name, story: '', rules: '', order: [], quests: {} },
                });
            } catch {
                return;
            }
            void navigate(gamePath(id));
        }
    };

    if (loadError instanceof ApiError && loadError.status === 401) {
        return <Navigate to="/login" state={{ returnTo: '/' }} replace />;
    }

    const error = changeError ?? loadError;
    return (
        <>
            <header className="page-title">
                <div>
                    <div className="eyebrow">{formatMessage({ id: 'common.organizer' })}</div>
                    <h1>{formatMessage({ id: 'games.title' })}</h1>
                    <p>{formatMessage({ id: 'games.detail' })}</p>
                </div>
                <button className="button primary" type="button" onClick={handleNew}>
                    <Plus size={18} /> {formatMessage({ id: 'games.new' })}
                </button>
            </header>
            {error && (
                <p className="notice error" role="alert">
                    {error.message}
                </p>
            )}
            {isPending && (
                <p className="notice" role="status">
                    {formatMessage({ id: 'games.loading' })}
                </p>
            )}
            {creating && (
                <form className="panel editor-section" onSubmit={handleSubmit}>
                    <label className="field">
                        <span>{formatMessage({ id: 'game.name' })}</span>
                        <input autoFocus value={title} onChange={handleTitle} maxLength={80} />
                    </label>
                    <button className="button primary" type="submit" disabled={!title.trim() || pending}>
                        {formatMessage({ id: 'games.create' })}
                    </button>
                </form>
            )}
            {games?.length === 0 && !creating && (
                <p className="notice">{formatMessage({ id: 'games.empty' })}</p>
            )}
            <div className="game-cards">
                {games?.map((game) => (
                    <GameCard key={game.id} game={game} onRename={handleRename} onDelete={handleDelete} />
                ))}
            </div>
        </>
    );
}
