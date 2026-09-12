package com.learning.platform.ui;

import com.microsoft.playwright.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class RealTimeTrackingAndEmailConfigUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("Issue 1: Reading scroll progress dynamically updates when scrolling modal body")
    void testReadingScrollProgressUpdatesOnModalBodyScroll() {
        navigateHome();

        // Ensure user is logged in
        page.evaluate("() => App.quickLogin('user_3')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Plan Overview for Applied Data Engineering
        page.evaluate("() => App.openPlan('PLAN_ADE_01')");
        page.waitForSelector("#modules-tree-container .lesson-row", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Open Lesson 2: Normalization in practice (which is READING)
        page.evaluate("() => App.openLessonModal('PLAN_ADE_01', 'LES_ADE_02')");
        page.waitForSelector("#lesson-player-modal", new Page.WaitForSelectorOptions().setTimeout(5000));

        // Verify Reading activity tracker is displayed
        Locator tracker = page.locator("#lesson-activity-tracker");
        assertTrue(tracker.isVisible(), "Activity tracker should be visible for reading lesson");

        Locator metric = page.locator("#activity-metric-text");
        assertTrue(metric.innerText().contains("Scroll Progress"), "Metric text should mention Scroll Progress");

        // Scroll the modal body (.lesson-modal-body) which was previously failing
        page.evaluate("() => { const mb = document.querySelector('.lesson-modal-body'); if (mb) { mb.scrollTop = mb.scrollHeight; mb.dispatchEvent(new Event('scroll')); } }");
        page.waitForTimeout(600);

        // Verify scroll progress updated beyond 0%
        String updatedMetric = metric.innerText();
        assertFalse(updatedMetric.contains("0% / 85%"), "Scroll progress should update and not stay stuck at 0%: " + updatedMetric);

        captureScreenshot("reading_scroll_progress_updated");

        // Close lesson modal
        page.locator("#lesson-player-modal .btn-modal-close").click();
    }

    @Test
    @DisplayName("Issue 2: Video session auto-completes when 80% threshold is reached")
    void testVideoAutoCompletionAt80Percent() {
        navigateHome();

        page.evaluate("() => App.quickLogin('user_3')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Plan Overview for Applied Data Engineering
        page.evaluate("() => App.openPlan('PLAN_ADE_01')");
        page.waitForSelector("#modules-tree-container .lesson-row", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Open Lesson 1: Data modeling primer (which is VIDEO)
        page.evaluate("() => App.openLessonModal('PLAN_ADE_01', 'LES_ADE_01')");
        page.waitForSelector("#lesson-player-modal", new Page.WaitForSelectorOptions().setTimeout(5000));

        // Click Dev Fast Forward to 80%
        page.locator("#btn-dev-fastforward").click();
        page.waitForTimeout(600);

        // Verify Auto-Completion triggered: button says '✓ Completed'
        Locator completeBtn = page.locator("#btn-modal-complete");
        assertTrue(completeBtn.innerText().contains("Completed"), "Button should reflect auto-completed status: " + completeBtn.innerText());

        captureScreenshot("video_auto_completion_80_pct");

        // Close modal
        page.locator("#lesson-player-modal .btn-modal-close").click();
    }

    @Test
    @DisplayName("Issue 3: Resend Email Center allows runtime API Key configuration and shows clear status")
    void testResendEmailCenterApiKeyConfigurationAndNotice() {
        navigateHome();

        page.evaluate("() => App.quickLogin('user_3')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Open Email Modal
        page.evaluate("() => App.openEmailModal()");
        page.waitForSelector("#email-inbox-modal", new Page.WaitForSelectorOptions().setTimeout(5000));

        // Switch to test tab
        page.locator("#tab-email-test").click();
        page.waitForTimeout(300);

        // Verify Resend connection card is present
        Locator statusPill = page.locator("#resend-live-status-pill");
        assertTrue(statusPill.isVisible(), "Resend live status pill should be visible");

        Locator keyInput = page.locator("#resend-api-key-input");
        assertTrue(keyInput.isVisible(), "API key input field should be present");

        // Test configuring key
        page.fill("#resend-api-key-input", "re_testKey1234567890abcdef");
        page.locator("#btn-connect-resend-key").click();
        page.waitForTimeout(600);

        // Verify status feedback updated to live ready
        Locator feedback = page.locator("#resend-key-status-msg");
        assertTrue(feedback.isVisible());
        assertTrue(feedback.innerText().contains("Live email delivery") || feedback.innerText().contains("connected"),
                "Feedback message should indicate key connected: " + feedback.innerText());

        captureScreenshot("resend_key_configured");

        // Close email modal
        page.locator("#email-inbox-modal .btn-modal-close").click();
    }
}
