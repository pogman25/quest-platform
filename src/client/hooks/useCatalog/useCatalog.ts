import { useContext } from 'react';
import { CatalogContext } from '../../app/CatalogContext/CatalogContext';

export function useCatalog() {
    const catalog = useContext(CatalogContext);
    if (!catalog) {
        throw new Error('Catalog requested outside the authenticated admin area.');
    }
    return catalog;
}
