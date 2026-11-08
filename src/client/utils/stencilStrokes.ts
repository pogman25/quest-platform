import { currentStencilStrokeVersion } from '../../shared/quests';
import type { StencilQuest } from '../../shared/quests';

type Point = readonly [x: number, y: number];
export type Stroke = readonly [start: Point, end: Point];

const minMovableLength = 1.5;

// Each path contains points on a 4 × 6 grid. The two digits are x and y.
const latin: Record<string, string> = {
    A: '06 20 46 / 14 34',
    B: '06 00 30 41 42 33 03 / 33 44 45 36 06',
    C: '40 10 01 05 16 46',
    D: '06 00 30 41 45 36 06',
    E: '40 00 06 46 / 03 33',
    F: '40 00 06 / 03 33',
    G: '40 10 01 05 16 36 45 43 23',
    H: '00 06 / 40 46 / 03 43',
    I: '00 40 / 20 26 / 06 46',
    J: '40 45 36 16 05',
    K: '00 06 / 40 03 46',
    L: '00 06 46',
    M: '06 00 23 40 46',
    N: '06 00 46 40',
    O: '10 30 41 45 36 16 05 01 10',
    P: '06 00 30 41 42 33 03',
    Q: '10 30 41 45 36 16 05 01 10 / 24 46',
    R: '06 00 30 41 42 33 03 / 23 46',
    S: '40 10 01 02 13 33 44 45 36 06',
    T: '00 40 / 20 26',
    U: '00 05 16 36 45 40',
    V: '00 26 40',
    W: '00 16 23 36 40',
    X: '00 46 / 40 06',
    Y: '00 23 40 / 23 26',
    Z: '00 40 06 46',
};

const glyphs: Record<string, string> = {
    ...latin,
    А: latin.A,
    Б: '40 00 06 36 45 44 33 03',
    В: latin.B,
    Г: '06 00 40',
    Д: '06 05 45 46 / 15 20 40 45',
    Е: latin.E,
    Ё: '42 02 06 46 / 04 34 / 10 11 / 30 31',
    Ж: '20 26 / 00 23 06 / 40 23 46',
    З: '00 30 41 42 33 13 / 33 44 45 36 06',
    И: '00 06 40 46',
    Й: '01 06 41 46 / 10 21 30',
    К: latin.K,
    Л: '06 10 40 46',
    М: latin.M,
    Н: latin.H,
    О: latin.O,
    П: '06 00 40 46',
    Р: latin.P,
    С: latin.C,
    Т: latin.T,
    У: '00 24 / 40 15 06',
    Ф: '20 26 / 12 32 43 44 35 15 04 03 12',
    Х: latin.X,
    Ц: '00 05 45 40 / 45 46',
    Ч: '00 02 13 43 / 40 46',
    Ш: '00 06 46 40 / 20 26',
    Щ: '00 05 45 40 / 20 25 / 45 46',
    Ъ: '00 10 16 36 45 44 33 13',
    Ы: '00 06 26 35 34 23 03 / 40 46',
    Ь: '00 06 36 45 44 33 03',
    Э: '00 30 41 45 36 06 / 13 43',
    Ю: '00 06 / 03 23 / 20 40 46 26 20',
    Я: '46 40 10 01 02 13 43 / 23 06',
};

function point(value: string): Point {
    return [Number(value[0]), Number(value[1])];
}

function makeRandom(seed: number) {
    let state = seed;
    return () => {
        state = (state + 0x6d2b79f5) >>> 0;
        let value = Math.imul(state ^ (state >>> 15), state | 1);
        value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
        return ((value ^ (value >>> 14)) >>> 0) / 0x100000000;
    };
}

function shuffle<T>(items: T[], random: () => number) {
    for (let index = items.length - 1; index > 0; index--) {
        const other = Math.floor(random() * (index + 1));
        [items[index], items[other]] = [items[other], items[index]];
    }
    return items;
}

function pathEdges(paths: string): Stroke[] {
    return paths.split(' / ').flatMap((path) => {
        const points = path.split(' ').map(point);
        return points.slice(1).map((end, index) => [points[index], end] as const);
    });
}

function splitGlyph(letter: string, random: () => number): Stroke[] {
    const strokes: Stroke[] = [];
    for (const [[x, y], [endX, endY]] of pathEdges(glyphs[letter])) {
        const dx = endX - x;
        const dy = endY - y;
        const parts = Math.max(1, Math.ceil(Math.hypot(dx, dy) * 0.8));
        let start: Point = [x, y];
        for (let part = 1; part <= parts; part++) {
            const fraction = part === parts ? 1 : (part + (random() - 0.5) * 0.5) / parts;
            const end: Point = [x + dx * fraction, y + dy * fraction];
            strokes.push([start, end]);
            start = end;
        }
    }
    return strokes;
}

function chooseBase(count: number, used: Set<string>, random: () => number) {
    const start = Math.floor(random() * count);
    const base = Array.from({ length: Math.floor(count / 2) }, (_, index) => (start + index) % count);
    const rest = shuffle(
        Array.from({ length: count - base.length }, (_, index) => (start + base.length + index) % count),
        random,
    );
    // Keep large breaks in the outline, with one stroke exchanged between layers.
    const swap = Math.floor(random() * base.length);
    [base[swap], rest[0]] = [rest[0], base[swap]];

    const reserve = () => {
        const signature = [...base].sort((a, b) => a - b).join(',');
        if (used.has(signature)) {
            return false;
        }
        used.add(signature);
        return true;
    };

    if (reserve()) {
        return new Set(base);
    }

    // A single swap offers more combinations than the 12 possible positions.
    for (let index = 0; index < base.length; index++) {
        const original = base[index];
        for (const alternate of rest) {
            base[index] = alternate;
            if (reserve()) {
                return new Set(base);
            }
        }
        base[index] = original;
    }
    throw new Error('Could not split the repeated stencil letter');
}

function shiftStroke([start, end]: Stroke, random: () => number): Stroke {
    const dx = end[0] - start[0];
    const dy = end[1] - start[1];
    const offset = ((random() < 0.5 ? -1 : 1) * (0.55 + random() * 0.3)) / Math.hypot(dx, dy);
    return [
        [start[0] - dy * offset, start[1] + dx * offset],
        [end[0] - dy * offset, end[1] + dx * offset],
    ];
}

function makeLegacyStencil(letters: string[], seed: number) {
    const random = makeRandom(seed);
    const usedByLetter = new Map<string, Set<string>>();
    return letters.map((letter) => {
        const reference = splitGlyph(letter, random);
        const used = usedByLetter.get(letter) ?? new Set<string>();
        usedByLetter.set(letter, used);
        const baseMask = chooseBase(reference.length, used, random);
        const base = reference.filter((_, index) => baseMask.has(index));
        const complement = reference.filter((_, index) => !baseMask.has(index));
        const decoys = Array.from({ length: 7 }, () => {
            const shuffled = shuffle([...complement], random);
            return shuffled.map((stroke, index) =>
                index < Math.ceil(shuffled.length / 2) ? shiftStroke(stroke, random) : stroke,
            );
        });
        return { reference, base, complement, decoys };
    });
}

function along([[x, y], [endX, endY]]: Stroke, fraction: number): Point {
    return [x + (endX - x) * fraction, y + (endY - y) * fraction];
}

function strokeLength([[x, y], [endX, endY]]: Stroke) {
    return Math.hypot(endX - x, endY - y);
}

function distanceToStroke([x, y]: Point, stroke: Stroke) {
    const [[startX, startY], [endX, endY]] = stroke;
    const dx = endX - startX;
    const dy = endY - startY;
    const projection = ((x - startX) * dx + (y - startY) * dy) / (dx * dx + dy * dy);
    const [nearX, nearY] = along(stroke, Math.max(0, Math.min(1, projection)));
    return Math.hypot(x - nearX, y - nearY);
}

function maskingStroke(ink: Stroke[], angle: number, random: () => number): Stroke | undefined {
    for (let attempt = 0; attempt < 64; attempt++) {
        const x = random() * 4.5 - 0.25;
        const y = random() * 6;
        const radius = 0.34 + random() * 0.22;
        const dx = Math.cos(angle) * radius;
        const dy = Math.sin(angle) * radius;
        const start: Point = [x - dx, y - dy];
        const end: Point = [x + dx, y + dy];
        const samples: Point[] = [start, [x, y], end];
        if (ink.every((stroke) => samples.every((sample) => distanceToStroke(sample, stroke) >= 0.68))) {
            return [start, end];
        }
    }
}

function fragmentLayout(tiles: Stroke[], angle: number, used: Set<string>, random: () => number) {
    const dx = Math.cos(angle);
    const dy = Math.sin(angle);
    const sorted = tiles
        .map((tile, index) => {
            const [x, y] = along(tile, 0.5);
            return { tile, index, projection: x * dx + y * dy };
        })
        .sort((first, second) => first.projection - second.projection);
    // Keep two long shared tiles near the cut so even a small print has a visible gap.
    const center = (sorted.length - 1) / 2;
    const shared = sorted
        .map((entry, rank) => ({ entry, distance: Math.abs(rank - center) }))
        .filter(({ entry }) => strokeLength(entry.tile) >= minMovableLength)
        .sort((first, second) => first.distance - second.distance)
        .slice(0, 2)
        .map(({ entry }) => entry);
    const sides = sorted.filter((entry) => !shared.includes(entry));
    const hidden = Math.floor((tiles.length - 3) / 2);
    const firstSide = sides.slice(0, hidden);
    const secondSide = sides.slice(-hidden);
    const middle = [...shared, ...sides.slice(hidden, -hidden)].sort(
        (first, second) => first.index - second.index,
    );
    const maskCount = 2 ** middle.length;
    const start = Math.floor(random() * maskCount);
    const directions = random() < 0.5 ? [firstSide, secondSide] : [secondSide, firstSide];

    for (const [baseOnly, overlayOnly] of [directions, [...directions].reverse()]) {
        const side = baseOnly
            .map(({ index }) => index)
            .sort((first, second) => first - second)
            .join(',');
        const otherSide = overlayOnly
            .map(({ index }) => index)
            .sort((first, second) => first - second)
            .join(',');
        for (let offset = 0; offset < maskCount; offset++) {
            const mask = (start + offset) % maskCount;
            const signature = `${side}/${otherSide}/${mask}`;
            if (used.has(signature)) {
                continue;
            }
            used.add(signature);
            return { baseOnly, overlayOnly, middle, mask };
        }
    }
    throw new Error('Could not split the repeated stencil letter');
}

function makeFragmentedGlyph(letter: string, masks: Set<string>, random: () => number) {
    const isA = letter === 'A' || letter === 'А';
    const reference = pathEdges(isA ? '06 20 46 / 13 33' : glyphs[letter]);
    const tiles = reference.flatMap((edge) => {
        const count = Math.ceil(strokeLength(edge) / 2.45);
        return Array.from({ length: count }, (_, index): Stroke => [
            along(edge, index / count),
            along(edge, (index + 1) / count),
        ]);
    });
    // For А and Д the gaps run through opposite sides of the glyph. The apex,
    // crossbar and feet are particularly easy to spot with a horizontal cut.
    const angle = (random() - 0.5) * (isA || letter === 'Д' ? Math.PI / 4 : Math.PI * 2);
    const { baseOnly, overlayOnly, middle, mask } = fragmentLayout(tiles, angle, masks, random);
    const fragments = middle.map(({ tile }, index) => {
        const matchingShare = 0.44 + random() * 0.05;
        const matchingStarts = Math.floor(mask / 2 ** index) % 2 === 1;
        const [start, end] = tile;
        const cut = along(tile, matchingStarts ? matchingShare : 1 - matchingShare);
        const base: Stroke = matchingStarts ? [cut, end] : [start, cut];
        const matching: Stroke = matchingStarts ? [start, cut] : [cut, end];
        // A moved fragment keeps its length and angle. It lands over the card's ink,
        // leaving a gap at its original position when the two sheets are aligned.
        const moved: Stroke = matchingStarts
            ? [along(tile, 1 - matchingShare), end]
            : [start, along(tile, matchingShare)];
        return { base, matching, moved };
    });

    const base = [...fragments.map((fragment) => fragment.base), ...baseOnly.map(({ tile }) => tile)];
    const complement = [
        ...fragments.map((fragment) => fragment.matching),
        ...overlayOnly.map(({ tile }) => tile),
    ];
    const ink = [...reference];
    for (const layer of [base, complement]) {
        for (const angle of [0, ((1 + Math.floor(random() * 3)) * Math.PI) / 4]) {
            const mark = maskingStroke(ink, angle, random);
            if (!mark) {
                continue;
            }
            layer.push(mark);
            ink.push(mark);
        }
    }

    const indices = fragments.map((_, index) => index);
    const changes = indices
        .flatMap((index) => indices.slice(index + 1).map((other) => [index, other]))
        .filter((pair) => pair.some((index) => strokeLength(middle[index].tile) >= minMovableLength));
    if (indices.length === 3) {
        changes.push(indices);
    }

    return { reference, base, complement, fragments, changes: shuffle(changes, random) };
}

function makeFragmentedGlyphs(letters: string[], random: () => number) {
    const usedByLetter = new Map<string, Set<string>>();
    return letters.map((letter) => {
        const masks = usedByLetter.get(letter) ?? new Set<string>();
        usedByLetter.set(letter, masks);
        return makeFragmentedGlyph(letter, masks, random);
    });
}

function makeFragmentedStencil(letters: string[], seed: number) {
    const random = makeRandom(seed);
    const glyphs = makeFragmentedGlyphs(letters, random);

    const choices = (half: typeof glyphs) => {
        const positions = shuffle(
            half.map((_, position) => position),
            random,
        );
        // Spread the broken letters across the card before reusing a position.
        const rounds = Math.min(...half.map((glyph) => glyph.changes.length));
        return Array.from({ length: rounds }, (_, round) =>
            positions.map((position) => ({
                position,
                strokes: half[position].changes[round],
            })),
        ).flat();
    };

    const middle = glyphs.length / 2;
    const left = choices(glyphs.slice(0, middle));
    const right = choices(glyphs.slice(middle));
    return glyphs.map(({ reference, base, complement, fragments }, index) => {
        const decoys = Array.from({ length: 7 }, (_, variant) => {
            const choice = index < middle ? left[variant] : right[variant];
            if (choice.position !== index % middle) {
                return complement;
            }
            const overlay = [...complement];
            for (const stroke of choice.strokes) {
                overlay[stroke] = fragments[stroke].moved;
            }
            return overlay;
        });
        return { reference, base, complement, decoys };
    });
}

function scrambleLetters(letters: string[], absent: string[], random: () => number) {
    const half = letters.length / 2;
    // Give each card its own share of foreign letters, even in a four-letter word.
    const foreignPositions = new Set(
        [0, half].flatMap((start) =>
            shuffle(
                Array.from({ length: half }, (_, index) => start + index),
                random,
            ).slice(0, Math.ceil(half / 2)),
        ),
    );
    const shuffled = shuffle([...letters], random);
    const foreign = shuffle([...absent], random);
    return letters.map((letter, index) => {
        if (!foreignPositions.has(index)) {
            const donor = shuffled.findIndex((candidate) => glyphs[candidate] !== glyphs[letter]);
            if (donor !== -1) {
                return shuffled.splice(donor, 1)[0];
            }
        }
        // Repeated letters can leave no different donor. Use an absent letter in that cell as well.
        return foreign.pop()!;
    });
}

function makeScrambledStencil(letters: string[], seed: number) {
    const random = makeRandom(seed);
    const originals = makeFragmentedGlyphs(letters, random);
    const usedShapes = new Set(letters.map((letter) => glyphs[letter]));
    const scripts = new Set(letters.map((letter) => letter in latin));
    const absent = Object.keys(glyphs).filter(
        (letter) => scripts.has(letter in latin) && !usedShapes.has(glyphs[letter]),
    );
    const overlays = Array.from({ length: 7 }, () => {
        const decoyLetters = scrambleLetters(letters, absent, random);
        return makeFragmentedGlyphs(decoyLetters, random).map(({ complement }) => complement);
    });
    return originals.map(({ reference, base, complement }, index) => ({
        reference,
        base,
        complement,
        decoys: overlays.map((overlay) => overlay[index]),
    }));
}

export function makeStrokeStencil(letters: string[], seed: number, version?: StencilQuest['strokeVersion']) {
    // Old saved seeds must reproduce the sheets an organizer may have already printed.
    if (version === currentStencilStrokeVersion) {
        return makeScrambledStencil(letters, seed);
    }
    return version === 2 ? makeFragmentedStencil(letters, seed) : makeLegacyStencil(letters, seed);
}
