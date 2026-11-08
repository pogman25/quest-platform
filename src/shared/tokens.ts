import type { NfcTag } from './quests';

export function makeToken(kind: 'nfc' | 'qr' | 'id') {
    const value = Array.from(crypto.getRandomValues(new Uint8Array(16)), (byte) =>
        byte.toString(16).padStart(2, '0'),
    ).join('');
    return kind === 'id' ? value : `quest.${kind}.${value}`;
}
export function makeTag(label: string): NfcTag {
    return { id: makeToken('id'), label, token: makeToken('nfc') };
}
