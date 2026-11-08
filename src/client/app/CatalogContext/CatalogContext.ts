import { createContext } from 'react';
import type { Catalog } from '../../../shared/catalog';

export const CatalogContext = createContext<Catalog | null>(null);
