import { demoGameId } from '../../shared/legacy';

export function gamePath(gameId: string) {
    return `/${gameId}`;
}

export function legacyRoutePath({
    protocol,
    pathname,
    hash,
}: Pick<Location, 'protocol' | 'pathname' | 'hash'>) {
    if (protocol !== 'file:' && pathname !== '/' && pathname !== '/index.html') {
        return null;
    }

    const route = hash.replace(/^#\/?/, '');
    if (route === 'games') {
        return '/';
    }
    if (route === 'login') {
        return '/login';
    }
    if (route === 'progress') {
        return `${gamePath(demoGameId)}/progress`;
    }

    const match = /^game\/([a-z0-9-]+)(\/[a-z0-9-]+)?$/.exec(route);
    return match ? `${gamePath(match[1])}${match[2] ?? ''}` : null;
}
