import js from '@eslint/js';
import { defineConfig, globalIgnores } from 'eslint/config';
import prettier from 'eslint-config-prettier/flat';
import reactHooks from 'eslint-plugin-react-hooks';
import globals from 'globals';
import tseslint from 'typescript-eslint';

export default defineConfig(
    globalIgnores(['dist/', '.data/', 'coverage/']),
    {
        files: ['**/*.{ts,tsx,mjs}'],
        extends: [js.configs.recommended, tseslint.configs.recommended, prettier],
        rules: { curly: ['error', 'all'] },
    },
    {
        files: ['src/client/**/*.{ts,tsx}'],
        languageOptions: { globals: globals.browser },
        plugins: { 'react-hooks': reactHooks },
        rules: {
            'react-hooks/rules-of-hooks': 'error',
            'react-hooks/exhaustive-deps': 'error',
        },
    },
    {
        files: ['src/server/**/*.ts', '*.config.{ts,mjs}'],
        languageOptions: { globals: globals.node },
    },
);
