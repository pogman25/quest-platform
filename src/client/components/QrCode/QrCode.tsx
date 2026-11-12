import { useMemo } from 'react';
import { makeQr } from '../../utils/qr';

interface QrCodeProps {
    value: string;
    label: string;
}

export function QrCode({ value, label }: QrCodeProps) {
    const qr = useMemo(() => makeQr(value), [value]);
    return (
        <svg viewBox={`0 0 ${qr.size} ${qr.size}`} role="img" aria-label={label} shapeRendering="crispEdges">
            <rect width={qr.size} height={qr.size} fill="white" />
            <path d={qr.path} fill="black" />
        </svg>
    );
}
