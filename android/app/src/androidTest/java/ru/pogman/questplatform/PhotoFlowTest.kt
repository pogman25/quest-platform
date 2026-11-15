package ru.pogman.questplatform

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.text.AnnotatedString
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class PhotoFlowTest {
    private val fixture = IsolatedAppRule()
    private val composeRule = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(fixture).around(composeRule)

    @Before
    fun waitForHome() {
        composeRule.waitForIdle()
        waitFor("Как играть")
    }

    @Test
    fun organizerCanPrepareAndTeamCanFinishWithUnlimitedRetries() {
        click("Ведущий", scroll = false)
        fill("Пароль", "password1")
        fill("Повторите пароль", "password1")
        click("Задать пароль")
        click("Настроить")
        fill("Название игры", "Пробное дело")
        click("Сохранить игру")
        click("Подготовить игру")
        fill("Название команды", "Следопыты")
        fill("Цифра замка", "0")
        click("Передать команде")
        waitFor("Начать расследование")
        assertFalse(fixture.container.access.authorized.value)
        runBlocking {
            val id = fixture.container.database.quests().settings()!!.activeRunId!!
            assertNull(fixture.container.database.quests().run(id)!!.run.startedAt)
        }
        click("Начать расследование")
        click("Открыть улику")
        composeRule.waitUntil(timeoutMillis = 10_000) {
            composeRule.onAllNodesWithTag("questPhoto").fetchSemanticsNodes().isNotEmpty()
        }
        fill("Какой предмет исчез?", "книга")
        composeRule.activityRule.scenario.recreate()
        waitFor("Проверить ответ")
        composeRule.onNodeWithText("книга").assertExists()
        click("Проверить ответ")
        waitFor("Ответ пока не подошёл. Посмотрите внимательнее и попробуйте ещё раз.")
        fill("Какой предмет исчез?", " ЛАМПА ")
        click("Проверить ответ")
        waitFor("Дело раскрыто!")
        composeRule.onNodeWithTag("finalCode").assertTextEquals("0")
        click("К блокноту команды")
        click("Ведущий", scroll = false)
        waitFor("Вход ведущего")
        fill("Пароль", "wrong-password")
        click("Войти")
        waitFor("Неверный пароль.")
        fill("Пароль", "password1")
        click("Войти")
        click("Прохождения команд")
        click("Показать ответы")
        composeRule.onNodeWithText("Попыток: 2 · Ошибок: 1").assertExists()
        composeRule.onNodeWithText("книга", substring = true).assertExists()
        composeRule.onNodeWithText("ЛАМПА", substring = true).assertExists()
        click("Сбросить результаты")
        composeRule.onNodeWithText("Отмена").performClick()
        composeRule.onNodeWithText("Попыток: 2 · Ошибок: 1").assertExists()
        click("Сбросить результаты")
        composeRule.onNodeWithText("Сбросить и начать заново").performClick()
        waitFor("Начать расследование")
        assertFalse(fixture.container.access.authorized.value)
        runBlocking {
            val id = fixture.container.database.quests().settings()!!.activeRunId!!
            val restarted = fixture.container.database.quests().run(id)!!
            assertEquals("Следопыты", restarted.run.teamName)
            assertEquals("Пробное дело", restarted.run.gameTitle)
            assertNull(restarted.run.startedAt)
            assertNull(restarted.run.finishedAt)
            assertTrue(restarted.attempts.isEmpty())
        }
        click("Начать расследование")
        click("Открыть улику")
        waitFor("Какой предмет исчез?")
        composeRule
            .onNodeWithText("Какой предмет исчез?")
            .assert(
                SemanticsMatcher.expectValue(SemanticsProperties.EditableText, AnnotatedString(""))
            )
        fill("Какой предмет исчез?", "лампа")
        click("Проверить ответ")
        waitFor("Дело раскрыто!")
        composeRule.onNodeWithTag("finalCode").assertTextEquals("0")
    }

    @Test
    fun organizerSessionSurvivesRotationAndClosesWhenReturningToPlayer() {
        click("Ведущий", scroll = false)
        fill("Пароль", "password1")
        fill("Повторите пароль", "password1")
        click("Задать пароль")
        waitFor("Создать игру")
        composeRule.activityRule.scenario.recreate()
        waitFor("Создать игру")
        assertTrue(fixture.container.access.authorized.value)
        click("К игре", scroll = false)
        waitFor("Как играть")
        assertFalse(fixture.container.access.authorized.value)
        click("Ведущий", scroll = false)
        waitFor("Вход ведущего")
        composeRule.onNodeWithText("Создать игру").assertDoesNotExist()
        fill("Пароль", "password1")
        click("Войти")
        waitFor("Создать игру")
        // Simulate loss of the in-memory session while a private route is restored.
        composeRule.runOnIdle { fixture.container.access.logout() }
        composeRule.activityRule.scenario.recreate()
        waitFor("Вход ведущего")
        composeRule.onNodeWithText("Создать игру").assertDoesNotExist()
    }

    private fun waitFor(text: String) {
        try {
            composeRule.waitUntil(timeoutMillis = 15_000) {
                composeRule.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
            }
        } catch (error: ComposeTimeoutException) {
            throw AssertionError(
                "Waiting for: $text\n${composeRule.onRoot().printToString()}",
                error,
            )
        }
    }

    private fun click(text: String, scroll: Boolean = true) {
        waitFor(text)
        // Finish the IME animation before scrolling and calculating touch coordinates.
        val activity = composeRule.activity
        composeRule.runOnUiThread {
            WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                .hide(WindowInsetsCompat.Type.ime())
        }
        composeRule.waitUntil(timeoutMillis = 5_000) {
            ViewCompat.getRootWindowInsets(activity.window.decorView)
                ?.getInsets(WindowInsetsCompat.Type.ime())
                ?.bottom == 0
        }
        composeRule.waitForIdle()
        val node = composeRule.onNodeWithText(text)
        if (scroll) {
            node.performScrollTo()
        }
        node.assertIsDisplayed().assertIsEnabled().performClick()
    }

    private fun fill(label: String, value: String) {
        waitFor(label)
        composeRule.onNodeWithText(label).performScrollTo().performTextReplacement(value)
    }
}
