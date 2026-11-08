import { createContext } from 'react';
import type { RefObject } from 'react';

export interface Credentials {
    email: string;
    password: string;
}

interface SessionContextValue {
    emailInput: RefObject<HTMLInputElement | null>;
    signIn: (credentials: Credentials) => void;
    signInError: Error | null;
    signingIn: boolean;
}

export const SessionContext = createContext<SessionContextValue | null>(null);
