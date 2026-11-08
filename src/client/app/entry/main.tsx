import { QueryClientProvider } from '@tanstack/react-query';
import { IntlProvider } from 'react-intl';
import { createRoot } from 'react-dom/client';
import { RouterProvider } from 'react-router/dom';
import { CatalogContext } from '../CatalogContext/CatalogContext';
import { createAdminQueryClient } from '../queryClient/queryClient';
import { loadBootstrap } from '../bootstrap/bootstrap';
import { messages } from '../messages/ru';
import { legacyRoutePath } from '../../utils/paths';
import { createRouter } from '../router/router';
import '../../styles/styles.css';

async function start() {
    const root = document.getElementById('root')!;
    if (window.location.protocol === 'file:') {
        root.textContent = messages['app.serverRequired'];
        return;
    }
    const previousPath = legacyRoutePath(window.location);
    if (previousPath) {
        window.location.replace(previousPath + window.location.search);
        return;
    }
    try {
        const { bootstrap, importError } = await loadBootstrap();
        const queryClient = createAdminQueryClient(bootstrap);
        createRoot(root).render(
            <QueryClientProvider client={queryClient}>
                <CatalogContext value={bootstrap?.catalog ?? null}>
                    <IntlProvider locale="ru" defaultLocale="ru" messages={messages}>
                        <RouterProvider router={createRouter(Boolean(bootstrap), importError)} />
                    </IntlProvider>
                </CatalogContext>
            </QueryClientProvider>,
        );
    } catch {
        root.textContent = messages['app.loadFailed'];
    }
}

void start();
