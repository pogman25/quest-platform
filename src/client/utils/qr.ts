import QRCode from 'qrcode';
import { jsPDF } from 'jspdf';
import type { QrQuest } from '../../shared/quests';
import { loadImage, makeCanvas } from './images';

export function makeQr(token: string) {
    const { modules } = QRCode.create(token, { errorCorrectionLevel: 'H' });
    const quiet = 4;
    const size = modules.size + quiet * 2;
    const runs: { x: number; y: number; width: number }[] = [];
    for (let row = 0; row < modules.size; row++) {
        let start = -1;
        for (let col = 0; col <= modules.size; col++) {
            const filled = col < modules.size && modules.get(row, col);
            if (filled && start < 0) {
                start = col;
            }
            if (!filled && start >= 0) {
                runs.push({ x: start + quiet, y: row + quiet, width: col - start });
                start = -1;
            }
        }
    }
    return { size, runs, path: runs.map(({ x, y, width }) => `M${x} ${y}h${width}v1h-${width}z`).join('') };
}

function pageLabel(doc: jsPDF, side: string, size: number, x: number, y: number) {
    doc.setTextColor(40, 55, 48).setFontSize(9);
    doc.text(`QUEST BUREAU / ${side}`, x, y - 8);
    doc.setTextColor(95, 108, 99).setFontSize(8);
    doc.text(`${size} x ${size} mm  /  A4  /  100%  /  DUPLEX LONG EDGE`, x, y + size + 8);
    doc.setLineWidth(0.2).setDrawColor(100, 111, 104).rect(x, y, size, size);
}

export async function makeQrPdf(quest: QrQuest) {
    const doc = new jsPDF({ unit: 'mm', format: 'a4', compress: true });
    const size = quest.size;
    const x = (210 - size) / 2;
    const y = (297 - size) / 2;
    const pixels = Math.round(size * 12);
    const image = await loadImage(quest.picture.src);
    const { canvas, context } = makeCanvas(pixels, pixels);
    const scale =
        quest.fit === 'cover'
            ? pixels / Math.min(image.naturalWidth, image.naturalHeight)
            : pixels / Math.max(image.naturalWidth, image.naturalHeight);
    const w = image.naturalWidth * scale;
    const h = image.naturalHeight * scale;
    context.fillStyle = '#fff';
    context.fillRect(0, 0, pixels, pixels);
    context.drawImage(image, (pixels - w) / 2, (pixels - h) / 2, w, h);
    doc.addImage(canvas.toDataURL('image/jpeg', 0.94), 'JPEG', x, y, size, size);
    doc.setDrawColor(25, 33, 31).setLineWidth(0.55);
    for (const fraction of [1 / 3, 2 / 3]) {
        const offset = size * fraction;
        doc.line(x + offset, y, x + offset, y + size);
        doc.line(x, y + offset, x + size, y + offset);
    }
    doc.setDrawColor(255).setLineWidth(0.24).setLineDashPattern([2, 1.2], 0);
    for (const fraction of [1 / 3, 2 / 3]) {
        const offset = size * fraction;
        doc.line(x + offset, y, x + offset, y + size);
        doc.line(x, y + offset, x + size, y + offset);
    }
    doc.setLineDashPattern([], 0);
    pageLabel(doc, '01 FRONT', size, x, y);

    doc.addPage();
    const qr = makeQr(quest.token);
    const margin = 8;
    const moduleSize = (size - margin * 2) / qr.size;
    doc.setFillColor('#000000');
    for (const run of qr.runs) {
        doc.rect(
            x + margin + run.x * moduleSize,
            y + margin + run.y * moduleSize,
            run.width * moduleSize,
            moduleSize,
            'F',
        );
    }
    pageLabel(doc, '02 BACK', size, x, y);
    return doc;
}
