import { useMutation, useQueryClient } from '@tanstack/react-query';
import { useLocation } from 'react-router';
import { api, ApiError } from '../../app/api/api';
import type { Credentials } from '../../app/SessionContext/SessionContext';

export function useSessionActions() {
    const { state } = useLocation();
    const queryClient = useQueryClient();
    const login = useMutation({
        mutationFn: (credentials: Credentials) =>
            api('/auth/login', { method: 'POST', body: { ...credentials, web: true } }),
        onSuccess: () => {
            const returnTo =
                typeof state?.returnTo === 'string' && /^\/(?![/\\])/.test(state.returnTo)
                    ? state.returnTo
                    : '/';
            window.location.replace(returnTo);
        },
    });
    const logout = useMutation({
        mutationFn: async () => {
            try {
                await api('/auth/logout', { method: 'POST' });
            } catch (reason) {
                if (!(reason instanceof ApiError && reason.status === 401)) {
                    throw reason;
                }
            }
        },
        onSuccess: () => {
            queryClient.clear();
            window.location.replace('/login');
        },
    });

    return { login, logout };
}
