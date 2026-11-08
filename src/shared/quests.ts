// Supported mechanics and icon keys are part of the client/server contract.
export const questTypes = ['nfc', 'qr', 'objects', 'photo', 'tree', 'stencil'] as const;
export type QuestType = (typeof questTypes)[number];

export const iconNames = [
    'clock',
    'cup',
    'candle',
    'book',
    'key',
    'cloud',
    'lamp',
    'umbrella',
    'ball',
    'chair',
    'star',
    'bell',
    'gift',
    'candy',
    'flake',
    'glove',
    'snowman',
    'ribbon',
    'rabbit',
    'tree',
    'camera',
] as const;
export type IconName = (typeof iconNames)[number];
export type Picture = { name: string; src: string };
export type IconSlot = { id: string; icon: IconName; label: string; picture?: Picture };
export type NfcTag = { id: string; label: string; token: string };

type QuestBase<Type extends QuestType> = { type: Type; instruction: string };
export type NfcQuest = QuestBase<'nfc'> & {
    hintPicture?: Picture;
    tags: NfcTag[];
    correctId: string;
    limit: number;
};
export type QrQuest = QuestBase<'qr'> & {
    picture: Picture;
    token: string;
    size: number;
    fit: 'cover' | 'contain';
};
export type PhotoQuest = QuestBase<'photo'> & { picture: Picture; answers: string };
export type IconQuest = QuestBase<'objects' | 'tree'> & { slots: IconSlot[]; answers: string[] };
export const currentStencilStrokeVersion = 3;
export type StencilFont = 'rounded' | 'serif' | 'mono' | 'custom';
export type UploadedFont = { name: string; family: string; src: string };
export type StencilQuest = QuestBase<'stencil'> & {
    word: string;
    font: StencilFont;
    width: number;
    correct: number;
    customFont?: UploadedFont;
    strokeSeed?: number;
    strokeVersion?: 2 | typeof currentStencilStrokeVersion;
};
export type Quest = NfcQuest | QrQuest | PhotoQuest | IconQuest | StencilQuest;
export type Game = {
    id: string;
    title: string;
    order: QuestType[];
    quests: Partial<Record<QuestType, Quest>>;
};
