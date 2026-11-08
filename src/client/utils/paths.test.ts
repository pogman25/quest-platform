import { describe, expect, it } from 'vitest';
import { gamePath, legacyRoutePath } from './paths';

describe('game URLs', () => {
    it('uses the game identifier as the first segment', () => {
        expect(gamePath('my-game')).toBe('/my-game');
    });

    it.each([
        ['http://localhost/#game/my-game', '/my-game'],
        ['http://localhost/#game/my-game/stencil', '/my-game/stencil'],
        ['http://localhost/#games', '/'],
        ['http://localhost/#progress', '/detective-demo/progress'],
        ['file:///project/dist/index.html#game/my-game', '/my-game'],
        ['http://localhost/my-game/stencil', null],
        ['http://localhost/#game//other-host', null],
        ['http://localhost/#game/\\other-host', null],
    ])('maps old address %s', (url, expected) => {
        expect(legacyRoutePath(new URL(url))).toBe(expected);
    });
});
