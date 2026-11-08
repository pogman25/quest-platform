import { useOutletContext } from 'react-router';
import type { Game, Quest } from '../../../shared/quests';

export interface GameRouteData {
    game?: Game;
    quest?: Quest;
}

export function useGameRoute() {
    return useOutletContext<GameRouteData>();
}
