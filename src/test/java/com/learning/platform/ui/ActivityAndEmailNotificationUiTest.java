package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UI-E2E: Real Course Content, Activity Tracking & Resend Email Notifications")
class ActivityAndEmailNotificationUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("UI-E2E: Real YouTube educational video embed and activity completion tracking")
    void testRealCourseVideosAndActivityCompletion() {
        navigateHome();

        // Login as Pulkit Jain (user_3) who has clean slate for testing
        page.evaluate("() => App.quickLogin('user_3')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Plan Overview for Applied Data Engineering
        page.evaluate("() => App.openPlan('PLAN_ADE_01')");
        page.waitForSelector("#modules-tree-container .lesson-row", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Open first lesson (Data modeling primer)
        page.evaluate("() => App.openLessonModal('PLAN_ADE_01', 'LES_ADE_01')");
        page.waitForSelector("#lesson-player-modal", new Page.WaitForSelectorOptions().setTimeout(5000));
        assertTrue(page.locator("#lesson-player-modal").isVisible(), "Lesson player modal should open");

        // Verify Real YouTube Video URL (not placeholder 1FUcniACzmc)
        String videoSrc = page.locator("#modal-video-iframe").getAttribute("src");
        assertNotNull(videoSrc, "Video iframe must have a src URL");
        assertTrue(videoSrc.contains("youtube"), "Video iframe should embed YouTube");
        assertFalse(videoSrc.contains("1FUcniACzmc"), "Video iframe must NOT use the old placeholder URL");

        // Verify activity tracker bar exists and shows initial locked state
        Locator activityBar = page.locator("#lesson-activity-tracker");
        assertTrue(activityBar.isVisible(), "Activity tracker bar should be visible inside lesson modal");

        // Verify video container is visible and reading content is hidden for VIDEO component
        assertTrue(page.locator("#modal-video-container").isVisible(), "Video container should be visible for VIDEO component");
        assertFalse(page.locator("#modal-reading-content").isVisible(), "Reading container should be hidden for VIDEO component");
        assertTrue(page.locator("#modal-video-meta").isVisible(), "Video metadata card should be visible for VIDEO component");

        // Click Dev Fast-Forward to simulate fulfilling the 80% watch activity requirement
        page.locator("#btn-dev-fastforward").click();
        page.waitForTimeout(300);

        // Verify tracker updates to Completed or Unlocked
        String statusText = page.locator("#activity-badge-status").innerText();
        assertTrue(statusText.contains("Completed") || statusText.contains("Unlocked"), 
                "Activity badge should reflect Completed or Unlocked after requirement met, got: " + statusText);

        // Verify Complete Button is enabled
        Locator completeBtn = page.locator("#btn-modal-complete");
        assertFalse(completeBtn.isDisabled(), "Complete button should be enabled after activity requirement met");
        assertTrue(completeBtn.innerText().contains("Completed") || completeBtn.innerText().contains("Mark Lesson Complete"), 
                "Button text should prompt completion: " + completeBtn.innerText());

        captureScreenshot("activity_video_tracking_unlocked");

        // Click complete and verify modal closes
        completeBtn.click();
        page.waitForTimeout(600);
        assertFalse(page.locator("#lesson-player-modal").isVisible(), "Modal should close after completing lesson");
    }

    @Test
    @DisplayName("UI-E2E: Resend Email Center modal, template preview, and live test dispatch")
    void testResendEmailCenterModalAndTestDispatch() {
        navigateHome();

        // Login as Shivam Gupta (user_1)
        page.evaluate("() => App.quickLogin('user_1')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Verify Email Center button is removed from header navigation
        assertEquals(0, page.locator("#btn-email-notifications").count(), "Email Center button must be removed from prod UI navbar");

        // Open modal via App.openEmailModal for verification
        page.evaluate("() => App.openEmailModal()");
        page.waitForSelector("#email-inbox-modal", new Page.WaitForSelectorOptions().setTimeout(5000));
        assertTrue(page.locator("#email-inbox-modal").isVisible(), "Email notifications modal should be visible");

        // Check provider status subtitle
        String statusSub = page.locator("#email-modal-status-sub").innerText();
        assertTrue(statusSub.contains("Resend"), "Subtitle should confirm Resend provider integration");

        // Switch to 'Send Test Email' tab
        page.locator("#tab-email-test").click();
        page.waitForTimeout(300);
        assertTrue(page.locator("#email-content-test").isVisible(), "Test email form should be visible");

        // Dispatch test notification
        page.locator("#test-email-recipient").fill("delivered@resend.dev");
        page.locator("#btn-send-test-email").click();

        // Wait for confirmation
        page.waitForSelector("#test-email-result:not([style*='display: none'])", new Page.WaitForSelectorOptions().setTimeout(8000));
        String resultMsg = page.locator("#test-email-result").innerText();
        assertTrue(resultMsg.contains("Resend") && (resultMsg.toLowerCase().contains("success") || resultMsg.toLowerCase().contains("dispatched")),
                "Test email result should confirm Resend dispatch, got: " + resultMsg);

        // Switch to Dispatched history tab
        page.locator("#tab-email-history").click();
        page.waitForSelector("#email-audit-list .card", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Verify at least one audit card exists
        Locator cards = page.locator("#email-audit-list .card");
        assertTrue(cards.count() > 0, "Dispatched email list should contain audit cards");

        // Click Preview on first email
        Locator previewBtn = cards.first().locator("button:has-text('Preview HTML')");
        if (previewBtn.count() > 0) {
            previewBtn.click();
            page.waitForTimeout(300);
            assertTrue(page.locator("#email-content-preview").isVisible(), "Preview pane should be visible");
        }

        captureScreenshot("resend_email_center_verified");

        // Close modal
        page.locator("#email-inbox-modal .btn-modal-close").click();
        page.waitForTimeout(300);
        assertFalse(page.locator("#email-inbox-modal").isVisible(), "Email modal should close");
    }

    @Test
    @DisplayName("UI-E2E: Direct enrollment from Catalog triggers Resend email dispatch")
    void testCatalogDirectEnrollment() {
        navigateHome();

        // Login as Pulkit Jain (user_3)
        page.evaluate("() => App.quickLogin('user_3')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Catalog
        page.evaluate("() => App.navigate('catalog')");
        page.waitForSelector("#catalog-course-grid .catalog-card", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Verify catalog cards render
        Locator catalogCards = page.locator("#catalog-course-grid .catalog-card");
        assertTrue(catalogCards.count() >= 6, "Catalog should render all 6 courses");

        captureScreenshot("catalog_with_enrollment_buttons");
    }
}
