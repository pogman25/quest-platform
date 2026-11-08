import { useIntl } from 'react-intl';
import { useEffect, useRef, useState } from 'react';
import type { ChangeEvent } from 'react';
import { ImagePlus } from 'lucide-react';
import type { Picture } from '../../../shared/quests';
import { readPicture } from '../../utils/images';

interface PictureFieldProps {
    label: string;
    picture?: Picture;
    onChange: (value: Picture) => void;
    compact?: boolean;
}

export function PictureField({ label, picture, onChange, compact = false }: PictureFieldProps) {
    const { formatMessage } = useIntl();
    const [status, setStatus] = useState('');
    const latestChange = useRef(onChange);
    const upload = useRef(0);

    useEffect(() => {
        latestChange.current = onChange;
    }, [onChange]);
    useEffect(
        () => () => {
            upload.current++;
        },
        [],
    );

    const handleFile = async (event: ChangeEvent<HTMLInputElement>) => {
        const file = event.currentTarget.files?.[0];
        event.currentTarget.value = '';
        if (!file) {
            return;
        }
        const request = ++upload.current;
        setStatus(formatMessage({ id: 'picture.loading' }));
        try {
            const picture = await readPicture(file, compact ? 256 : 1800);
            if (request !== upload.current) {
                return;
            }
            latestChange.current(picture);
            setStatus('');
        } catch (error) {
            if (request === upload.current) {
                setStatus(error instanceof Error ? error.message : formatMessage({ id: 'picture.failed' }));
            }
        }
    };

    return (
        <div className="field">
            <label className="upload">
                <ImagePlus size={18} aria-hidden="true" />
                <span>
                    <strong>{label}</strong>
                    <small>{picture?.name ?? formatMessage({ id: 'picture.formats' })}</small>
                </span>
                <input type="file" accept="image/png,image/jpeg,image/webp" onChange={handleFile} />
            </label>
            {status && <small role="status">{status}</small>}
        </div>
    );
}
