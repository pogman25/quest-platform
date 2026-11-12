import { useEffect, useState } from 'react';
import { formatDuration } from '../../utils/formatDuration';

interface ElapsedTimeProps {
    startedAt: string | null;
    endedAt?: string | null;
}

export function ElapsedTime({ startedAt, endedAt }: ElapsedTimeProps) {
    const [now, setNow] = useState(Date.now);
    useEffect(() => {
        if (!startedAt || endedAt) {
            return;
        }
        const interval = window.setInterval(() => setNow(Date.now()), 1000);
        return () => window.clearInterval(interval);
    }, [startedAt, endedAt]);
    const seconds = startedAt
        ? Math.max(0, Math.floor(((endedAt ? Date.parse(endedAt) : now) - Date.parse(startedAt)) / 1000))
        : 0;
    return <span>{formatDuration(seconds)}</span>;
}
