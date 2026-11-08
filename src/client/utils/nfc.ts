import { messages } from '../app/messages/ru';
type NdefRecord = { recordType: string; data: DataView | null; encoding?: string };
type NdefReading = Event & { message: { records: NdefRecord[] } };
type NdefReader = EventTarget & {
    write(message: string, options: { signal: AbortSignal }): Promise<void>;
    scan(options: { signal: AbortSignal }): Promise<void>;
};

const Reader = (window as Window & { NDEFReader?: new () => NdefReader }).NDEFReader;
export const canUseNfc = window.isSecureContext && Boolean(Reader);

export async function writeNfc(token: string, signal: AbortSignal) {
    if (!Reader) {
        throw new Error(messages['nfc.writeUnsupported']);
    }
    await new Reader().write(token, { signal });
}

export async function readNfc(signal: AbortSignal): Promise<string> {
    if (!Reader) {
        throw new Error(messages['nfc.readUnsupported']);
    }
    const reader = new Reader();
    return new Promise((resolve, reject) => {
        const fail = () => {
            reject(new Error(messages['nfc.readFailed']));
        };
        const abort = () => {
            reject(signal.reason);
        };
        const handleReading = (event: Event) => {
            const { message } = event as NdefReading;
            const record = message.records.find(({ recordType, data }) => recordType === 'text' && data);
            if (!record?.data) {
                fail();
                return;
            }
            try {
                resolve(new TextDecoder(record.encoding ?? 'utf-8').decode(record.data));
            } catch {
                fail();
            }
        };
        signal.addEventListener('abort', abort, { once: true });
        reader.addEventListener('readingerror', fail, { once: true });
        reader.addEventListener('reading', handleReading, { once: true });
        reader.scan({ signal }).catch(reject);
    });
}
