import { useEffect, useState } from 'react';
import type { UploadedFont } from '../../../shared/quests';

export function useUploadedFont(font?: UploadedFont) {
    const [loaded, setLoaded] = useState<{ family: string; ready: boolean }>();

    useEffect(() => {
        if (!font) {
            return;
        }
        const face = new FontFace(font.family, `url("${font.src}")`, { weight: '900' });
        let current = true;
        void face
            .load()
            .then(() => {
                if (!current) {
                    return;
                }
                document.fonts.add(face);
                setLoaded({ family: font.family, ready: true });
            })
            .catch(() => {
                if (current) {
                    setLoaded({ family: font.family, ready: false });
                }
            });
        return () => {
            current = false;
            document.fonts.delete(face);
        };
    }, [font]);

    return loaded?.family === font?.family ? loaded?.ready : undefined;
}
