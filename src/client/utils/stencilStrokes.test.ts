import { expect, it } from 'vitest';
import { currentStencilStrokeVersion } from '../../shared/quests';
import { makeStrokeStencil } from './stencilStrokes';

it('recreates the same printable kit from the saved seed', () => {
    const letters = Array.from('ВОДОПАДЫ');
    const kit = makeStrokeStencil(letters, 42, 2);
    expect(makeStrokeStencil(letters, 42, 2)).toEqual(kit);
    expect(kit[1].base).not.toEqual(kit[3].base);
    expect(kit[2].base).not.toEqual(kit[6].base);
});

it('changes exactly one letter in each half for every decoy', () => {
    const letters = Array.from('СНЕГОПАД');
    const kit = makeStrokeStencil(letters, 42, 2);
    for (let variant = 0; variant < 7; variant++) {
        const changed = kit.map((glyph) => glyph.decoys[variant] !== glyph.complement);
        expect(changed.slice(0, 4).filter(Boolean)).toHaveLength(1);
        expect(changed.slice(4).filter(Boolean)).toHaveLength(1);
    }
});

it.each(['СНЕГОПАД', 'ОООО', 'AAAA', 'АААААААААААА', 'AАOO'])(
    'replaces every position in all seven decoys for %s while preserving the matching layers',
    (word) => {
        const letters = Array.from(word);
        const kit = makeStrokeStencil(letters, 42, currentStencilStrokeVersion);
        expect(makeStrokeStencil(letters, 42, currentStencilStrokeVersion)).toEqual(kit);
        expect(makeStrokeStencil(letters, 43, currentStencilStrokeVersion)).not.toEqual(kit);

        const matchingLayers = kit.map(({ reference, base, complement }) => ({
            reference,
            base,
            complement,
        }));
        const versionTwo = makeStrokeStencil(letters, 42, 2);
        expect(matchingLayers).toEqual(
            versionTwo.map(({ reference, base, complement }) => ({ reference, base, complement })),
        );
        const decoys = Array.from({ length: 7 }, (_, variant) =>
            kit.map((glyph) => {
                expect(glyph.decoys[variant]).not.toEqual(glyph.complement);
                return glyph.decoys[variant];
            }),
        );
        expect(new Set(decoys.map((overlay) => JSON.stringify(overlay))).size).toBe(7);
    },
);
