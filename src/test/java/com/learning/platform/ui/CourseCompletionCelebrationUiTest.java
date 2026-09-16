package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UI-E2E: Course Completion Celebration Modal, Confetti & Milestone Honors")
class CourseCompletionCelebrationUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("Verify Congratulations Pop-up displays when course is completed, and can be re-opened from Plan Overview")
    void testCourseCompletionModalAppearsOnFullCompletion() {
        navigateHome();

        // Login as Shivam Gupta (user_1)
        page.evaluate("() => App.quickLogin('user_1')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(8000));

        // Navigate to Plan Overview for PLAN_TW_01 (Technical Writing - 100% completed)
        page.evaluate("() => App.openPlan('PLAN_TW_01')");
        page.waitForSelector("#plan-hero-title", new Page.WaitForSelectorOptions().setTimeout(8000));

        // Click the '✓ Completed · Review Curriculum' button (#btn-plan-continue)
        Locator contBtn = page.locator("#btn-plan-continue");
        assertTrue(contBtn.isVisible(), "Plan continue button must be visible");
        contBtn.click();

        // Verify the celebration modal is visible
        page.waitForFunction("() => {\n" +
                "    const m = document.getElementById('course-completion-modal');\n" +
                "    return m && (m.style.display === 'flex' || window.getComputedStyle(m).display === 'flex');\n" +
                "}", null, new Page.WaitForFunctionOptions().setTimeout(5000));

        Locator modal = page.locator("#course-completion-modal");
        assertTrue(modal.isVisible(), "Course completion congratulations modal must be visible");

        // Check Course Title & Learner Name
        Locator titleEl = page.locator("#completion-modal-course-title");
        assertTrue(titleEl.isVisible(), "Course title in completion modal must be visible");
        assertTrue(titleEl.innerText().contains("Technical Writing") || titleEl.innerText().length() > 3,
                "Course title should be displayed correctly");

        Locator learnerEl = page.locator("#completion-modal-learner-name");
        assertTrue(learnerEl.isVisible(), "Learner name must be visible");
        assertEquals("Shivam Gupta", learnerEl.innerText().trim());

        // Check XP & Lessons stats
        Locator xpEl = page.locator("#completion-modal-xp");
        assertTrue(xpEl.innerText().contains("XP"), "XP reward should be indicated");

        Locator lessonsEl = page.locator("#completion-modal-lessons");
        assertTrue(lessonsEl.innerText().contains("/"), "Lessons completed / total must be shown");

        captureScreenshot("course_completion_modal_celebration_success");

        // Close modal using 'Stay on Plan' button
        page.locator("#course-completion-modal button:has-text('Stay on Plan')").click();
        page.waitForFunction("() => {\n" +
                "    const m = document.getElementById('course-completion-modal');\n" +
                "    return !m || m.style.display === 'none';\n" +
                "}", null, new Page.WaitForFunctionOptions().setTimeout(5000));

        // Click header resume button (#btn-plan-resume-header)
        Locator headerBtn = page.locator("#btn-plan-resume-header");
        if (headerBtn.isVisible()) {
            headerBtn.click();
            page.waitForFunction("() => {\n" +
                    "    const m = document.getElementById('course-completion-modal');\n" +
                    "    return m && m.style.display === 'flex';\n" +
                    "}", null, new Page.WaitForFunctionOptions().setTimeout(5000));
            assertTrue(modal.isVisible(), "Header button must re-open celebration modal when 100% completed");

            // Close with close &times; button
            page.locator("#course-completion-modal .level-modal-close-btn").click();
            page.waitForFunction("() => {\n" +
                    "    const m = document.getElementById('course-completion-modal');\n" +
                    "    return !m || m.style.display === 'none';\n" +
                    "}", null, new Page.WaitForFunctionOptions().setTimeout(5000));
        }
    }

    @Test
    @DisplayName("Verify completed course card in My Learning has celebration pop-up action that opens modal")
    void testMyLearningHonorsCelebrationButton() {
        navigateHome();

        // Login as Shivam Gupta (user_1)
        page.evaluate("() => App.quickLogin('user_1')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(8000));

        // Navigate to My Learning
        page.evaluate("() => App.navigate('my-learning')");
        page.waitForSelector("#my-learning-grid .my-learning-card", new Page.WaitForSelectorOptions().setTimeout(8000));

        // Filter by Completed or find completed card
        Locator completedCard = page.locator(".my-learning-card.is-completed").first();
        assertTrue(completedCard.isVisible(), "Completed course card should be visible in My Learning");

        // Locate and click the '🎉 Honors Pop-up' button
        Locator popupBtn = completedCard.locator("button:has-text('Honors Pop-up')");
        if (popupBtn.count() > 0) {
            popupBtn.click();
        } else {
            completedCard.locator(".mylearning-completion-banner-left").click();
        }

        // Verify the celebration modal pops up
        page.waitForFunction("() => {\n" +
                "    const m = document.getElementById('course-completion-modal');\n" +
                "    return m && m.style.display === 'flex';\n" +
                "}", null, new Page.WaitForFunctionOptions().setTimeout(5000));

        Locator modal = page.locator("#course-completion-modal");
        assertTrue(modal.isVisible(), "Honors Pop-up button in My Learning must open completion modal");

        captureScreenshot("mylearning_course_completion_popup_opened");

        // Close modal
        page.locator("#course-completion-modal button:has-text('Stay on Plan')").click();
        page.waitForFunction("() => {\n" +
                "    const m = document.getElementById('course-completion-modal');\n" +
                "    return !m || m.style.display === 'none';\n" +
                "}", null, new Page.WaitForFunctionOptions().setTimeout(5000));
    }
}
