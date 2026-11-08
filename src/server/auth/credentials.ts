import { createHash, randomBytes, scrypt, timingSafeEqual } from 'node:crypto';
import { promisify } from 'node:util';

const deriveKey = promisify(scrypt);

export async function hashPassword(password: string) {
    const salt = randomBytes(16).toString('hex');
    const key = (await deriveKey(password, salt, 64)) as Buffer;
    return `${salt}:${key.toString('hex')}`;
}

export async function passwordMatches(password: string, encoded: string) {
    const [salt, hex] = encoded.split(':');
    const expected = Buffer.from(hex, 'hex');
    const actual = (await deriveKey(password, salt, expected.length)) as Buffer;
    return timingSafeEqual(expected, actual);
}

export function newToken() {
    return randomBytes(32).toString('base64url');
}

export function tokenHash(token: string) {
    return createHash('sha256').update(token).digest('hex');
}
