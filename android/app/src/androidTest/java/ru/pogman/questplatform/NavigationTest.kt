package ru.pogman.questplatform

import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain

class NavigationTest {
    private val fixture = IsolatedAppRule()
    val composeRule = createAndroidComposeRule<MainActivity>()
    @get:Rule val rules: RuleChain = RuleChain.outerRule(fixture).around(composeRule)

    @Before
    fun waitForFirstComposition() {
        // The v2 dispatcher queues the first frame; wait before querying the UI tree.
        composeRule.waitForIdle()
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule
                .onAllNodes(androidx.compose.ui.test.hasText("Как играть"))
                .fetchSemanticsNodes()
                .isNotEmpty()
        }
    }

    @Test
    fun instructionsSurviveActivityRecreationAndReturnToPlayer() {
        composeRule.onNodeWithText("Как играть").performClick()
        composeRule.onNodeWithText("Правила расследования").assertIsDisplayed()

        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Правила расследования").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Назад").performClick()
        composeRule.onNodeWithText("Блокнот команды").assertIsDisplayed()
    }

    @Test
    fun organizerEntryDoesNotProvideAccessWithoutPasswordSetup() {
        composeRule.onNodeWithText("Ведущий").performClick()
        composeRule.onNodeWithText("Задать пароль").assertIsNotEnabled()

        composeRule.activityRule.scenario.onActivity { activity ->
            activity.onBackPressedDispatcher.onBackPressed()
        }

        composeRule.onNodeWithText("Блокнот команды").assertIsDisplayed()
    }

    @Test
    fun reopeningMainActivityDoesNotRequireAnotherBackPressToExit() {
        val original = composeRule.activity
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val monitor = instrumentation.addMonitor(MainActivity::class.java.name, null, false)

        try {
            composeRule.activityRule.scenario.onActivity { activity ->
                activity.startActivity(Intent(activity, MainActivity::class.java))
            }

            val reopened = monitor.waitForActivityWithTimeout(1_000)
            if (reopened != null && reopened !== original) {
                instrumentation.runOnMainSync(reopened::finish)
            }
            assertTrue(
                "Opening the app again must reuse the current MainActivity",
                reopened == null || reopened === original,
            )

            composeRule.activityRule.scenario.onActivity { activity ->
                activity.onBackPressedDispatcher.onBackPressed()
            }

            composeRule.waitUntil(timeoutMillis = 5_000) {
                val state = composeRule.activityRule.scenario.state
                state == Lifecycle.State.CREATED || state == Lifecycle.State.DESTROYED
            }
        } finally {
            instrumentation.removeMonitor(monitor)
        }
    }
}
