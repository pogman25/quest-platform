import { expect, it } from 'vitest';
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
