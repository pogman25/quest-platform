package ru.pogman.questplatform.domain

enum class QuestFailure {
    PASSWORD_TOO_SHORT,
    PASSWORD_MISMATCH,
    WRONG_PASSWORD,
    PASSWORD_ALREADY_SET,
    ORGANIZER_REQUIRED,
    GAME_INVALID,
    GAME_NOT_FOUND,
    TEAM_NAME_REQUIRED,
    CODE_INVALID,
    RUN_NOT_FOUND,
    RUN_NOT_STARTED,
    PHOTO_INVALID,
    PHOTO_SOURCE_UNAVAILABLE,
    STORAGE_ERROR,
}

class QuestException(val failure: QuestFailure) : IllegalStateException(failure.name)
