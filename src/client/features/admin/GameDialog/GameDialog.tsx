import { useIntl } from 'react-intl';
import { useEffect, useId, useRef, useState } from 'react';
import type { ChangeEvent, SubmitEvent } from 'react';
import './GameDialog.css';

export type GameAction = 'rename' | 'delete';

interface GameDialogProps {
    action: GameAction;
    gameTitle: string;
    onRename: (title: string) => void;
    onDelete: () => void;
    onClose: () => void;
}

export function GameDialog({ action, gameTitle, onRename, onDelete, onClose }: GameDialogProps) {
    const { formatMessage } = useIntl();
    const [title, setTitle] = useState(gameTitle);
    const dialog = useRef<HTMLDialogElement>(null);
    const headingId = useId();
    const descriptionId = useId();
    const deleting = action === 'delete';

    useEffect(() => {
        dialog.current?.showModal();
    }, []);

    const handleTitle = (event: ChangeEvent<HTMLInputElement>) => {
        setTitle(event.currentTarget.value);
    };
    const handleCancel = () => {
        dialog.current?.close();
    };

    const handleSubmit = (event: SubmitEvent<HTMLFormElement>) => {
        event.preventDefault();
        if (deleting) {
            dialog.current?.close();
            onDelete();
        } else if (title.trim()) {
            onRename(title.trim());
            dialog.current?.close();
        }
    };

    return (
        <dialog
            ref={dialog}
            className="panel game-dialog"
            aria-labelledby={headingId}
            aria-describedby={deleting ? descriptionId : undefined}
            onClose={onClose}
        >
            <form onSubmit={handleSubmit}>
                <h2 id={headingId}>
                    {deleting
                        ? formatMessage({ id: 'game.deletePrompt' })
                        : formatMessage({ id: 'game.renamePrompt' })}
                </h2>
                {deleting ? (
                    <p id={descriptionId}>
                        {formatMessage({ id: 'game.deleteDetail' }, { title: gameTitle })}
                    </p>
                ) : (
                    <label className="field">
                        <span>{formatMessage({ id: 'game.name' })}</span>
                        <input value={title} onChange={handleTitle} maxLength={80} required />
                    </label>
                )}
                <div className="game-dialog-actions">
                    <button className="button secondary" type="button" onClick={handleCancel}>
                        {formatMessage({ id: 'common.cancel' })}
                    </button>
                    <button
                        className={`button ${deleting ? 'delete-game-button' : 'primary'}`}
                        type="submit"
                        disabled={!deleting && !title.trim()}
                    >
                        {deleting
                            ? formatMessage({ id: 'game.delete' })
                            : formatMessage({ id: 'common.save' })}
                    </button>
                </div>
            </form>
        </dialog>
    );
}
