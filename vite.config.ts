import react from '@vitejs/plugin-react';
import { defineConfig } from 'vite';
import { viteSingleFile } from 'vite-plugin-singlefile';

export default defineConfig({
    plugins: [react(), viteSingleFile()],
    server: { proxy: { '/api': process.env.QUEST_API_TARGET ?? 'http://127.0.0.1:4173' } },
    preview: { port: 4174 },
});
