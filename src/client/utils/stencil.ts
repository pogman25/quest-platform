import { jsPDF } from 'jspdf';
import type { StencilFont, StencilQuest } from '../../shared/quests';
import { makeCanvas } from './images';
import { makeStrokeStencil } from './stencilStrokes';
import type { Stroke } from './stencilStrokes';

export const stencilHeight = 34;
export const stencilFonts = {
    rounded: { labelId: 'stencil.font.rounded', family: 'Arial Black, Arial, sans-serif' },
    serif: { labelId: 'stencil.font.serif', family: 'Georgia, Times New Roman, serif' },
    mono: { labelId: 'stencil.font.mono', family: 'Courier New, monospace' },
} satisfies Record<Exclude<StencilFont, 'custom'>, { labelId: string; family: string }>;

export function stencilLetters(word: string) {
    const letters = Array.from(word.trim().normalize('NFC').toLocaleUpperCase('ru'));
    return letters.length >= 4 &&
        letters.length <= 12 &&
        letters.length % 2 === 0 &&
        letters.every((letter) => /^[А-ЯЁA-Z]$/.test(letter))
        ? letters
        : null;
}

function makeFontLayers(letters: string[], cell: number, height: number, correct: number, family: string) {
    const width = cell * letters.length;
    const { canvas: word, context } = makeCanvas(width, height);
    let fontSize = height * 0.77;
    context.font = `900 ${fontSize}px ${family}`;
    const maxWidth = Math.max(...letters.map((letter) => context.measureText(letter).width));
    fontSize *= Math.min(1, (cell * 0.84) / maxWidth);
    context.font = `900 ${fontSize}px ${family}`;
    context.textAlign = 'center';
    for (const [index, letter] of letters.entries()) {
        const metrics = context.measureText(letter);
        const baseline = (height + metrics.actualBoundingBoxAscent - metrics.actualBoundingBoxDescent) / 2;
        context.fillText(letter, (index + 0.5) * cell, baseline);
    }

    const source = context.getImageData(0, 0, width, height);
    const base = new ImageData(new Uint8ClampedArray(source.data), width, height);
    const complement = new ImageData(new Uint8ClampedArray(source.data), width, height);
    for (let row = 0; row < height; row++) {
        for (let col = 0; col < width; col++) {
            const alpha = (row * width + col) * 4 + 3;
            const glyph = Math.floor(col / cell);
            const band = Math.floor((row * 4) / height);
            const column = Math.floor(((col % cell) * 3) / cell);
            const layer = (band + column + glyph + Math.floor(glyph / 2)) % 2 === 0 ? base : complement;
            layer.data[alpha] = 0;
        }
    }
    const { canvas: baseCanvas, context: baseContext } = makeCanvas(width, height);
    const { canvas: matching, context: matchingContext } = makeCanvas(width, height);
    baseContext.putImageData(base, 0, 0);
    matchingContext.putImageData(complement, 0, 0);

    let decoy = 0;
    const overlays = Array.from({ length: 8 }, (_, index) => {
        if (index + 1 === correct) {
            return matching;
        }
        const variant = decoy++;
        const { canvas, context: fake } = makeCanvas(width, height);
        for (let glyph = 0; glyph < letters.length; glyph++) {
            const keep =
                (variant < 3 && glyph < letters.length / 2) ||
                (variant >= 3 && variant < 6 && glyph >= letters.length / 2) ||
                (variant === 6 && glyph % 3 === 1);
            if (keep) {
                fake.drawImage(matching, glyph * cell, 0, cell, height, glyph * cell, 0, cell, height);
                continue;
            }
            const other = (glyph + variant + 1) % letters.length;
            const offset = height * (variant % 2 === 0 ? -0.13 : 0.15);
            fake.save();
            fake.translate((glyph + 1) * cell, offset);
            fake.scale(-1, 1);
            fake.drawImage(matching, other * cell, 0, cell, height, 0, 0, cell, height);
            fake.restore();
        }
        return canvas;
    });
    return { word, base: baseCanvas, overlays };
}

type GlyphLayers = ReturnType<typeof makeStrokeStencil>[number];

function makeStrokeLayers(
    letters: string[],
    cell: number,
    height: number,
    correct: number,
    seed: number,
    version: StencilQuest['strokeVersion'],
) {
    const glyphs = makeStrokeStencil(letters, seed, version);
    const scale = Math.min(cell / 6.3, height / 8.3);
    const xOffset = (cell - 4 * scale) / 2;
    const yOffset = (height - 6 * scale) / 2;

    const drawLayer = (selectStrokes: (glyph: GlyphLayers) => Stroke[]) => {
        const { canvas, context } = makeCanvas(cell * letters.length, height);
        context.lineWidth = scale * 0.3;
        context.lineCap = 'round';
        context.beginPath();
        glyphs.forEach((glyph, index) => {
            const originX = index * cell + xOffset;
            for (const [[x, y], [endX, endY]] of selectStrokes(glyph)) {
                context.moveTo(originX + x * scale, yOffset + y * scale);
                context.lineTo(originX + endX * scale, yOffset + endY * scale);
            }
        });
        context.stroke();
        return canvas;
    };

    const overlays = Array.from({ length: 8 }, (_, index) => {
        if (index + 1 === correct) {
            return drawLayer((glyph) => glyph.complement);
        }
        const decoy = index < correct - 1 ? index : index - 1;
        return drawLayer((glyph) => glyph.decoys[decoy]);
    });
    return { word: drawLayer((glyph) => glyph.reference), base: drawLayer((glyph) => glyph.base), overlays };
}

type StencilDrawing =
    { family: string } | { strokeSeed: number; strokeVersion?: StencilQuest['strokeVersion'] };

export function makeStencilKit(
    quest: Pick<StencilQuest, 'width' | 'correct'> & StencilDrawing,
    letters: string[],
) {
    const height = stencilHeight * 12;
    const cell = Math.round((quest.width * 12) / letters.length);
    const layers =
        'strokeSeed' in quest
            ? makeStrokeLayers(letters, cell, height, quest.correct, quest.strokeSeed, quest.strokeVersion)
            : makeFontLayers(letters, cell, height, quest.correct, quest.family);
    const half = layers.base.width / 2;
    const { canvas: left, context: leftContext } = makeCanvas(half, height);
    const { canvas: right, context: rightContext } = makeCanvas(half, height);
    leftContext.drawImage(layers.base, 0, 0, half, height, 0, 0, half, height);
    rightContext.drawImage(layers.base, half, 0, half, height, 0, 0, half, height);
    return { ...layers, left, right };
}

type StencilKit = ReturnType<typeof makeStencilKit>;

export function previewStencil(kit: StencilKit, selected: number, combined: boolean) {
    const overlay = kit.overlays[selected - 1];
    if (!combined) {
        return overlay.toDataURL();
    }
    const { canvas, context } = makeCanvas(overlay.width, overlay.height);
    context.drawImage(overlay, 0, 0);
    context.drawImage(kit.base, 0, 0);
    return canvas.toDataURL();
}

function card(doc: jsPDF, canvas: HTMLCanvasElement, x: number, y: number, width: number, label: string) {
    doc.setTextColor(55, 67, 61)
        .setFontSize(8)
        .text(label, x, y - 3);
    doc.addImage(canvas.toDataURL('image/png'), 'PNG', x, y, width, stencilHeight, undefined, 'FAST');
    doc.setDrawColor(95, 106, 99).setLineWidth(0.2).rect(x, y, width, stencilHeight);
}

export function makeStencilPdf(quest: StencilQuest, kit: StencilKit) {
    const doc = new jsPDF({ unit: 'mm', format: 'a4', compress: true });
    const x = (210 - quest.width) / 2;
    const title = (text: string, detail: string) => {
        doc.setTextColor(25, 40, 32).setFontSize(15).text(text, 15, 20);
        doc.setTextColor(82, 94, 87).setFontSize(9).text(detail, 15, 27);
        doc.setDrawColor(95, 106, 99).setLineWidth(0.25).line(15, 282, 65, 282);
        doc.text('50 mm', 15, 287);
    };
    title('SNOW STENCIL / ORGANIZER COPY', `A4 / ACTUAL SIZE / matching overlay ${quest.correct}`);
    card(doc, kit.word, x, 46, quest.width, 'Complete word / reference');
    const half = quest.width / 2;
    const halfX = (210 - quest.width - 6) / 2;
    card(doc, kit.left, halfX, 104, half, 'Card A');
    card(doc, kit.right, halfX + half + 6, 104, half, 'Card B');
    doc.setFontSize(9).text('Trace the fragments and rectangle. Keep the labels outside the cards.', 15, 152);

    for (let page = 0; page < 2; page++) {
        doc.addPage();
        title('SNOW STENCIL / OVERLAYS', `A4 / ACTUAL SIZE / ${page + 2} OF 3`);
        for (let row = 0; row < 4; row++) {
            const index = page * 4 + row;
            card(doc, kit.overlays[index], x, 44 + row * 55, quest.width, `Overlay ${index + 1}`);
        }
    }
    return doc;
}
