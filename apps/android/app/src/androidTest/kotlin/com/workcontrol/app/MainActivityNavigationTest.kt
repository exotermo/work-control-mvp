package com.workcontrol.app

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import org.junit.Rule
import org.junit.Test

/**
 * Smoke tests for the navigation paths that are already part of the MVP shell.
 *
 * The assertions intentionally use user-facing accessibility semantics instead of coordinates,
 * so they do not depend on screen dimensions or on the device navigation mode.
 */
class MainActivityNavigationTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun launch_startsOnHome() {
        waitForText("Work Control")

        composeRule.onNodeWithText("Work Control").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Atualizar").assertIsDisplayed()
    }

    @Test
    fun tasks_opensTask184AndReturnsToList() {
        waitForText("Work Control")
        composeRule.onNodeWithContentDescription("Tarefas").performClick()
        waitForText("PROJETO ATLAS · 5 TOTAL")

        val task184 = hasText("Investigar e corrigir bug #184") and hasClickAction()
        composeRule.onNodeWithTag("task-list").performScrollToNode(task184)
        composeRule.onNode(task184).performSemanticsAction(SemanticsActions.OnClick)
        waitForContentDescription("Voltar")
        composeRule.onNodeWithText("Investigar e corrigir bug #184").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Voltar").performClick()
        waitForText("PROJETO ATLAS · 5 TOTAL")
        composeRule.onNodeWithText("#184").assertIsDisplayed()
    }

    @Test
    fun topLevelPlaceholders_areReachableFromBottomNavigation() {
        waitForText("Work Control")
        composeRule.onNodeWithContentDescription("Máquinas").performClick()
        waitForText("4 REGISTRADAS · 3 ONLINE")
        composeRule.onNodeWithText("Em construção").assertIsDisplayed()

        composeRule.onNodeWithContentDescription("Arquivos").performClick()
        waitForText("WORKSTATION-01")
        composeRule.onNodeWithText("Em construção").assertIsDisplayed()
    }

    @Test
    fun quickActions_opensSheetAndNavigatesToNewTask() {
        waitForText("Work Control")
        composeRule.onNodeWithContentDescription("Ações rápidas").performClick()
        waitForText("Nova tarefa")

        composeRule.onNodeWithText("Terminal / SSH").assertIsDisplayed()
        composeRule.onNodeWithText("Acessar máquina").assertExists()
        composeRule.onNodeWithText("Nova tarefa").performClick()

        waitForText("Nova Tarefa")
        composeRule.onNodeWithText("Em construção").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Voltar").performClick()
        waitForText("Work Control")
    }

    private fun waitForText(text: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun waitForContentDescription(description: String) {
        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
