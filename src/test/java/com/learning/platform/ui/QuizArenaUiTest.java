package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuizArenaUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("UI-E2E: Quiz Arena navigation and knowledge arena views load properly")
    void testQuizArenaNavigation() {
        navigateHome();

        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Quiz Arena
        page.locator("#nav-quiz-arena").click();
        page.waitForSelector("#view-quiz-arena", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(5000));

        Locator quizView = page.locator("#view-quiz-arena");
        assertTrue(quizView.isVisible(), "Quiz Arena view should be visible");

        // Verify Daily Challenge card exists
        Locator startDailyBtn = page.locator("#btn-start-daily");
        assertTrue(startDailyBtn.isVisible(), "Start Daily Quiz button should be visible");

        captureScreenshot("quiz_arena_view");
    }
}
