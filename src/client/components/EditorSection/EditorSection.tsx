import type { ReactNode } from 'react';

interface EditorSectionProps {
    title: string;
    detail?: string;
    children: ReactNode;
}

export function EditorSection({ title, detail, children }: EditorSectionProps) {
    return (
        <section className="panel editor-section">
            <header className="section-head">
                <h2>{title}</h2>
                {detail && <p>{detail}</p>}
            </header>
            {children}
        </section>
    );
}
