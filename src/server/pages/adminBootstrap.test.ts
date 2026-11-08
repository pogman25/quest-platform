import { describe, expect, it } from 'vitest';
import type { AdminBootstrap } from '../../shared/catalog';
import { injectBootstrap } from './adminBootstrap';

describe('bootstrap serialization', () => {
    it('keeps user text inside the JSON element', () => {
        const data: AdminBootstrap = {
            games: [{ id: 'test', title: '</script><img src=x> $&', questCount: 0 }],
            catalog: { quests: {} as AdminBootstrap['catalog']['quests'], icons: [] },
        };
        const page = injectBootstrap('<html><head><!-- quest-bootstrap --></head><body></body></html>', data);
        const match = /<script id="quest-bootstrap" type="application\/json">(.*?)<\/script>/.exec(page);
        expect(page).not.toContain('<img');
        expect(JSON.parse(match![1])).toEqual(data);
    });

    it('embeds an anonymous state without game or catalog data', () => {
        expect(injectBootstrap('<head><!-- quest-bootstrap --></head>', null)).toContain(
            'type="application/json">null</script>',
        );
    });

    it('leaves head-like text inside an inline bundle intact', () => {
        const inlineBundle = '<script>const sample = "</head>";</script>';
        const html = `<head><!-- quest-bootstrap -->${inlineBundle}</head>`;
        const page = injectBootstrap(html, null);
        expect(page).toContain(`${inlineBundle}</head>`);
    });
});
