import { messages } from '../app/messages/ru';
import type { Picture } from '../../shared/quests';

export function makeCanvas(width: number, height: number) {
    const canvas = document.createElement('canvas');
    canvas.width = width;
    canvas.height = height;
    const context = canvas.getContext('2d');
    if (!context) {
        throw new Error(messages['picture.canvasFailed']);
    }
    return { canvas, context };
}

export function loadImage(src: string): Promise<HTMLImageElement> {
    return new Promise((resolve, reject) => {
        const image = new Image();
        image.onload = () => resolve(image);
        image.onerror = () => reject(new Error(messages['picture.openFailed']));
        image.src = src;
    });
}

export async function readPicture(file: File, maxSide = 1800): Promise<Picture> {
    if (!['image/png', 'image/jpeg', 'image/webp'].includes(file.type)) {
        throw new Error(messages['picture.invalidFormat']);
    }
    if (file.size > 12 * 1024 * 1024) {
        throw new Error(messages['picture.tooLarge']);
    }
    const url = URL.createObjectURL(file);
    try {
        const image = await loadImage(url);
        const ratio = Math.min(1, maxSide / Math.max(image.naturalWidth, image.naturalHeight));
        const { canvas, context } = makeCanvas(
            Math.max(1, Math.round(image.naturalWidth * ratio)),
            Math.max(1, Math.round(image.naturalHeight * ratio)),
        );
        context.drawImage(image, 0, 0, canvas.width, canvas.height);
        const format = maxSide <= 256 ? 'image/png' : 'image/webp';
        return { name: file.name, src: canvas.toDataURL(format, 0.88) };
    } finally {
        URL.revokeObjectURL(url);
    }
}
