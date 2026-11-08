import { iconImages } from '../../../domain/questIcons/questIcons';
import type { IconSlot } from '../../../../shared/quests';

interface SlotIconProps {
    slot: IconSlot;
    size?: number;
}

export function SlotIcon({ slot, size = 24 }: SlotIconProps) {
    if (slot.picture) {
        return <img src={slot.picture.src} alt="" width={size} height={size} />;
    }
    const Icon = iconImages[slot.icon];
    return <Icon size={size} strokeWidth={1.7} aria-hidden="true" />;
}
