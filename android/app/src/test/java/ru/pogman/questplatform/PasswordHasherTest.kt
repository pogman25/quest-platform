package ru.pogman.questplatform

import org.junit.Assert.*
import org.junit.Test
import ru.pogman.questplatform.data.PasswordHasher

class PasswordHasherTest {
    @Test
    fun usesRandomSaltAndVerifiesUsingStoredWorkFactor() {
        val hasher = PasswordHasher(iterations = 1_000)
        val first = hasher.create("пароль-теста")
        val second = hasher.create("пароль-теста")
        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.hash.contentEquals(second.hash))
        assertEquals(32, first.hash.size)
        assertTrue(PasswordHasher(iterations = 2_000).verify("пароль-теста", first))
        assertFalse(hasher.verify("другой-пароль", first))
    }
}
