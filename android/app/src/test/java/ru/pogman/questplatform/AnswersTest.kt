package ru.pogman.questplatform

import org.junit.Assert.*
import org.junit.Test
import ru.pogman.questplatform.domain.elapsedSeconds
import ru.pogman.questplatform.domain.formatDuration
import ru.pogman.questplatform.domain.matchesPhotoAnswer

class AnswersTest {
    @Test
    fun acceptsRussianVariantsWithoutAcceptingPartialAnswers() {
        assertTrue(
            matchesPhotoAnswer("  НАСТОЛЬНАЯ\t ЛАМПА \n", listOf("лампа", "настольная лампа"))
        )
        assertTrue(matchesPhotoAnswer("ЁЛКА", listOf("елка")))
        assertTrue(matchesPhotoAnswer("е\u0308лка", listOf("ёлка")))
        assertFalse(matchesPhotoAnswer("настольная", listOf("настольная лампа")))
        assertFalse(matchesPhotoAnswer("  ", listOf("")))
    }

    @Test
    fun computesElapsedTimeFromPersistedTimestamps() {
        assertNull(elapsedSeconds(null, null, 99_000))
        assertEquals(8L, elapsedSeconds(1_000, null, 9_900))
        assertEquals(3L, elapsedSeconds(1_000, 4_999, 99_000))
        assertEquals(0L, elapsedSeconds(5_000, null, 1_000))
        assertEquals("125:03", formatDuration(7_503))
    }
}
