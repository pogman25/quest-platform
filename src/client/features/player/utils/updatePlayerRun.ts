import type { QuestType } from '../../../../shared/quests';
import type { AttemptResult, PlayerRun } from '../../../../shared/runs';

export function updatePlayerRun(
    state: PlayerRun | undefined,
    type: QuestType,
    result: AttemptResult,
): PlayerRun | undefined {
    if (!state) {
        return state;
    }
    return {
        ...state,
        finished: state.finished || result.finished,
        result: result.result ?? state.result,
        finishedAt: result.finishedAt ?? state.finishedAt,
        quests: state.quests.map((quest) =>
            quest.type === type
                ? {
                      ...quest,
                      startedAt: result.startedAt ?? quest.startedAt,
                      solvedAt: result.solvedAt ?? quest.solvedAt,
                      durationSeconds: result.durationSeconds ?? quest.durationSeconds,
                      status:
                          quest.status === 'solved' || result.status === 'correct'
                              ? 'solved'
                              : quest.status === 'locked' || result.remainingAttempts === 0
                                ? 'locked'
                                : 'in_progress',
                      reward: result.reward ?? quest.reward,
                      remainingAttempts:
                          quest.remainingAttempts === undefined
                              ? result.remainingAttempts
                              : Math.min(
                                    quest.remainingAttempts,
                                    result.remainingAttempts ?? quest.remainingAttempts,
                                ),
                  }
                : quest,
        ),
    };
}
