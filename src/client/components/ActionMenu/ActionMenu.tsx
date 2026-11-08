import { useEffect, useId, useRef, useState } from 'react';
import type { FocusEvent, KeyboardEvent, ReactNode } from 'react';
import './ActionMenu.css';

type CloseMenu = (restoreFocus?: boolean) => void;

interface ActionMenuProps {
    label: string;
    trigger: ReactNode;
    children: (closeMenu: CloseMenu) => ReactNode;
}

export function ActionMenu({ label, trigger, children }: ActionMenuProps) {
    const [open, setOpen] = useState(false);
    const menuId = useId();
    const container = useRef<HTMLDivElement>(null);
    const button = useRef<HTMLButtonElement>(null);

    useEffect(() => {
        if (!open) {
            return;
        }

        const handleOutsidePointer = (event: PointerEvent) => {
            if (event.target instanceof Node && !container.current?.contains(event.target)) {
                setOpen(false);
            }
        };

        document.addEventListener('pointerdown', handleOutsidePointer);
        return () => document.removeEventListener('pointerdown', handleOutsidePointer);
    }, [open]);

    const closeMenu = (restoreFocus = true) => {
        setOpen(false);
        if (restoreFocus) {
            button.current?.focus();
        }
    };

    const handleToggle = () => {
        setOpen((previous) => !previous);
    };

    const handleKeyDown = (event: KeyboardEvent<HTMLDivElement>) => {
        if (event.key !== 'Escape' || !open) {
            return;
        }
        event.preventDefault();
        closeMenu();
    };

    const handleBlur = (event: FocusEvent<HTMLDivElement>) => {
        if (event.relatedTarget && !event.currentTarget.contains(event.relatedTarget)) {
            setOpen(false);
        }
    };

    return (
        <div className="action-menu" ref={container} onKeyDown={handleKeyDown} onBlur={handleBlur}>
            <button
                className="action-menu-trigger"
                type="button"
                ref={button}
                aria-label={label}
                aria-expanded={open}
                aria-controls={menuId}
                onClick={handleToggle}
            >
                {trigger}
            </button>
            <div className="action-menu-popup" id={menuId} hidden={!open}>
                {children(closeMenu)}
            </div>
        </div>
    );
}
