import { useId, useRef } from 'react';
import { useIntl } from 'react-intl';
import type { RunSummary } from '../../../../shared/runs';
import { QrCode } from '../../../components/QrCode/QrCode';
import './RunInvitation.css';

interface RunInvitationProps {
    run: RunSummary;
}

export function RunInvitation({ run }: RunInvitationProps) {
    const { formatMessage } = useIntl();
    const dialog = useRef<HTMLDialogElement>(null);
    const headingId = useId();
    const link = new URL('/play', window.location.origin);
    link.searchParams.set('code', run.joinCode);
    const label = formatMessage({ id: 'progress.invitationQr' }, { team: run.teamName });

    const handleEnlarge = () => {
        dialog.current?.showModal();
    };
    const handleClose = () => {
        dialog.current?.close();
    };

    return (
        <div className="run-invitation stack">
            <strong>{run.teamName}</strong>
            <div className="run-invitation-qr">
                <QrCode value={link.href} label={label} />
            </div>
            <a className="run-invitation-link" href={link.href} target="_blank" rel="noreferrer">
                {link.href}
            </a>
            <button className="button secondary" type="button" onClick={handleEnlarge}>
                {formatMessage({ id: 'progress.enlargeQr' })}
            </button>
            <dialog className="panel run-invitation-dialog" ref={dialog} aria-labelledby={headingId}>
                <div className="stack">
                    <div className="section-line">
                        <h2 id={headingId}>{run.teamName}</h2>
                        <button className="button secondary" type="button" onClick={handleClose}>
                            {formatMessage({ id: 'common.close' })}
                        </button>
                    </div>
                    <QrCode value={link.href} label={label} />
                    <p>{formatMessage({ id: 'progress.scanInvitation' })}</p>
                    <strong>{run.joinCode}</strong>
                </div>
            </dialog>
        </div>
    );
}
