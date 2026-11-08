import { useContext } from 'react';
import { SessionContext } from '../../app/SessionContext/SessionContext';

export function useSession() {
    const session = useContext(SessionContext);
    if (!session) {
        throw new Error('Session requested outside the admin layout.');
    }
    return session;
}
