import { useIntl } from 'react-intl';
import { useState } from 'react';
import { Download } from 'lucide-react';
import './PdfButton.css';

interface PdfButtonProps {
    onDownload: () => Promise<void>;
    disabled?: boolean;
}

export function PdfButton({ onDownload, disabled = false }: PdfButtonProps) {
    const { formatMessage } = useIntl();
    const [working, setWorking] = useState(false);
    const [error, setError] = useState('');

    const handleDownload = async () => {
        setWorking(true);
        setError('');
        try {
            await onDownload();
        } catch (reason) {
            setError(reason instanceof Error ? reason.message : formatMessage({ id: 'pdf.failed' }));
        } finally {
            setWorking(false);
        }
    };

    return (
        <div className="download-action">
            <button
                type="button"
                className="button primary"
                onClick={handleDownload}
                disabled={working || disabled}
            >
                <Download size={17} aria-hidden="true" />
                {working ? formatMessage({ id: 'pdf.building' }) : formatMessage({ id: 'pdf.download' })}
            </button>
            {error && (
                <small className="error" role="alert">
                    {error}
                </small>
            )}
        </div>
    );
}
