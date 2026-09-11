package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class QuizArenaInteractiveUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("UI-E2E: Full interactive Daily Quiz Sprint completion and results scoring")
    void testCompleteDailyQuizWorkflow() {
        navigateHome();

        // Login as Isha Agarwal
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Quiz Arena
        page.locator("#nav-quiz-arena").click();
        page.waitForSelector("#view-quiz-arena.active", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(5000));

        // Start Daily Challenge
        Locator startDailyBtn = page.locator("#btn-start-daily");
        assertTrue(startDailyBtn.isVisible(), "Start Daily Quiz button should be visible");
        startDailyBtn.click();

        // Player card should open
        page.waitForSelector("#arena-player", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));
        Locator playerCard = page.locator("#arena-player");
        assertTrue(playerCard.isVisible(), "Quiz player card should become visible");

        // Answer Question 1
        page.waitForSelector(".quiz-option-tile", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(5000));
        Locator optionsQ1 = page.locator(".quiz-option-tile");
        assertTrue(optionsQ1.count() >= 2, "Quiz question must have multiple answer options");
        optionsQ1.first().click();

        // Verify explanation feedback appears
        Locator explBanner = page.locator("#player-explanation-banner");
        assertTrue(explBanner.isVisible(), "Explanation banner should reveal answer feedback");

        // Advance to Question 2
        page.locator("#player-btn-next").click();
        page.waitForTimeout(300);

        // Answer Question 2
        Locator optionsQ2 = page.locator(".quiz-option-tile");
        optionsQ2.first().click();
        page.locator("#player-btn-next").click();
        page.waitForTimeout(300);

        // Answer Question 3
        Locator optionsQ3 = page.locator(".quiz-option-tile");
        optionsQ3.first().click();

        // Submit Quiz
        page.locator("#player-btn-next").click();

        // Verify Results card is displayed
        page.waitForSelector("#arena-results", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(8000));
        Locator resultsCard = page.locator("#arena-results");
        assertTrue(resultsCard.isVisible(), "Quiz results evaluation card must be visible");

        captureScreenshot("quiz_interactive_results");
    }
}
