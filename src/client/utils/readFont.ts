import { messages } from '../app/messages/ru';
import { makeToken } from '../../shared/tokens';
import type { UploadedFont } from '../../shared/quests';

export async function readFont(file: File): Promise<UploadedFont> {
    if (!/\.(ttf|otf)$/i.test(file.name) || file.size > 1_500_000) {
        throw new Error(messages['stencil.invalidFont']);
    }
    const src = await new Promise<string>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = () => resolve(String(reader.result));
        reader.onerror = () => reject(new Error(messages['stencil.readFontFailed']));
        reader.readAsDataURL(file);
    });
    return { name: file.name, family: `QuestStencil_${makeToken('id')}`, src };
}
