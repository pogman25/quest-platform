package ru.pogman.questplatform

import org.junit.Assert.*
import org.junit.Test
import ru.pogman.questplatform.data.QuestConfigs
import ru.pogman.questplatform.data.decodeNdefText
import ru.pogman.questplatform.domain.*

class QuestRulesTest {
    @Test
    fun allMechanicsRespectTheirAnswerRules() {
        val photo = PhotoQuestConfig("Фото", "demo:room", listOf("ёлка"))
        assertEquals(
            AnswerVerdict.CORRECT,
            evaluateAnswer(QuestType.PHOTO, photo, QuestAnswer.Text(" ЕЛКА ")),
        )
        val word = WordQuestConfig("Слово", "метель")
        assertEquals(
            AnswerVerdict.CORRECT,
            evaluateAnswer(QuestType.STENCIL, word, QuestAnswer.Text(" МЕТЕЛЬ ")),
        )
        val tags =
            NfcQuestConfig(
                "Метка",
                listOf(
                    NfcTagConfig("yes", "Подлинник", "right"),
                    NfcTagConfig("no", "Ложная", "wrong"),
                ),
                "yes",
            )
        assertEquals(
            AnswerVerdict.CORRECT,
            evaluateAnswer(QuestType.NFC, tags, QuestAnswer.Token("right")),
        )
        assertEquals(
            AnswerVerdict.INCORRECT,
            evaluateAnswer(QuestType.NFC, tags, QuestAnswer.Token("wrong")),
        )
        assertEquals(
            AnswerVerdict.UNKNOWN_TAG,
            evaluateAnswer(QuestType.NFC, tags, QuestAnswer.Token("other-game")),
        )
        val qr = QrQuestConfig("Пазл", "demo:room", "puzzle-token")
        assertEquals(
            AnswerVerdict.CORRECT,
            evaluateAnswer(QuestType.QR, qr, QuestAnswer.Token("puzzle-token")),
        )
        assertEquals(
            AnswerVerdict.INCORRECT,
            evaluateAnswer(QuestType.QR, qr, QuestAnswer.Text("puzzle-token")),
        )
        val cells = (1..9).map { ChoiceCell(it.toString(), it.toString(), "⭐") }
        val objects = ChoiceQuestConfig("Предметы", cells, listOf("1", "2", "3"))
        assertEquals(
            AnswerVerdict.CORRECT,
            evaluateAnswer(
                QuestType.OBJECTS,
                objects,
                QuestAnswer.Selection(listOf("3", "1", "2")),
            ),
        )
        assertEquals(
            AnswerVerdict.INCORRECT,
            evaluateAnswer(
                QuestType.OBJECTS,
                objects,
                QuestAnswer.Selection(listOf("1", "1", "2")),
            ),
        )
        val tree = ChoiceQuestConfig("Ёлка", cells, listOf("1", "2", "3", "4"))
        assertEquals(
            AnswerVerdict.CORRECT,
            evaluateAnswer(QuestType.TREE, tree, QuestAnswer.Selection(listOf("1", "2", "3", "4"))),
        )
        assertEquals(
            AnswerVerdict.INCORRECT,
            evaluateAnswer(QuestType.TREE, tree, QuestAnswer.Selection(listOf("4", "3", "2", "1"))),
        )
    }

    @Test
    fun finalCodeKeepsLeadingZeroAndLongestClueUsesItsOwnClock() {
        val quests =
            listOf(
                PlayerQuest(QuestType.PHOTO, 1, "", null, emptyList(), 4_000, 9_000, "0"),
                PlayerQuest(QuestType.STENCIL, 2, "", null, emptyList(), 2_000, 9_000, "7"),
            )
        val run =
            PlayerRun("run", "Игра", "Команда", "", "", 1_000, 9_000, quests.reversed(), "game")
        assertEquals("07", run.finalCode)
        assertEquals(QuestType.STENCIL, run.slowestQuest!!.type)
        assertNull(run.copy(finishedAt = null).finalCode)
        assertNull(run.copy(quests = listOf(quests[0].copy(reward = null), quests[1])).finalCode)
    }

    @Test
    fun oldPhotoConfigAndNdefTextPayloadsRemainReadable() {
        val old = """{"instruction":"Фото","photoRef":"demo:room","answers":["лампа"]}"""
        assertEquals(
            PhotoQuestConfig("Фото", "demo:room", listOf("лампа")),
            QuestConfigs().decode(QuestType.PHOTO, old),
        )
        assertEquals(
            "токен",
            decodeNdefText(byteArrayOf(2) + "ruтокен".toByteArray(Charsets.UTF_8)),
        )
        assertEquals(
            "token",
            decodeNdefText(
                byteArrayOf(0x82.toByte()) +
                    "en".toByteArray() +
                    "token".toByteArray(Charsets.UTF_16)
            ),
        )
        assertNull(decodeNdefText(byteArrayOf(7, 1, 2)))
    }
}
