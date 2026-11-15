package ru.pogman.questplatform.data

import androidx.room.withTransaction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import ru.pogman.questplatform.domain.QuestException
import ru.pogman.questplatform.domain.QuestFailure

class OrganizerAccess(
    private val database: QuestDatabase,
    private val hasher: PasswordHasher = PasswordHasher(),
) {
    private val session = MutableStateFlow(false)
    val authorized = session.asStateFlow()
    val passwordConfigured = database.quests().observeCredentials().map { it != null }

    fun requireOrganizer() {
        if (!session.value) {
            throw QuestException(QuestFailure.ORGANIZER_REQUIRED)
        }
    }

    suspend fun setup(password: String, confirmation: String) {
        validate(password, confirmation)
        val digest = withContext(Dispatchers.Default) { hasher.create(password) }
        database.withTransaction {
            if (database.quests().credentials() != null) {
                throw QuestException(QuestFailure.PASSWORD_ALREADY_SET)
            }
            database
                .quests()
                .insertCredentials(
                    CredentialRow(
                        salt = digest.salt,
                        hash = digest.hash,
                        iterations = digest.iterations,
                    )
                )
        }
        session.value = true
    }

    suspend fun login(password: String) {
        if (!verify(password)) {
            throw QuestException(QuestFailure.WRONG_PASSWORD)
        }
        session.value = true
    }

    suspend fun changePassword(current: String, password: String, confirmation: String) {
        requireOrganizer()
        validate(password, confirmation)
        if (!verify(current)) {
            throw QuestException(QuestFailure.WRONG_PASSWORD)
        }
        val digest = withContext(Dispatchers.Default) { hasher.create(password) }
        database.withTransaction {
            requireOrganizer()
            database
                .quests()
                .updateCredentials(
                    CredentialRow(
                        salt = digest.salt,
                        hash = digest.hash,
                        iterations = digest.iterations,
                    )
                )
        }
    }

    fun logout() {
        session.value = false
    }

    private suspend fun verify(password: String): Boolean {
        val stored = database.quests().credentials() ?: return false
        return withContext(Dispatchers.Default) {
            hasher.verify(password, PasswordDigest(stored.salt, stored.hash, stored.iterations))
        }
    }

    private fun validate(password: String, confirmation: String) {
        if (password.length !in 8..128 || password.isBlank()) {
            throw QuestException(QuestFailure.PASSWORD_TOO_SHORT)
        }
        if (password != confirmation) {
            throw QuestException(QuestFailure.PASSWORD_MISMATCH)
        }
    }
}
