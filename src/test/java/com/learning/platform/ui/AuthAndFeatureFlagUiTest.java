package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthAndFeatureFlagUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("UI-E2E: Home page loads with valid title and Netflix intro overlay")
    void testHomePageAndIntro() {
        page.navigate(getBaseUrl() + "/");
        String title = page.title();
        assertTrue(title.contains("Career Pulse"), "Page title should contain Career Pulse, got: " + title);

        // Verify netflix intro element exists in DOM
        Locator introOverlay = page.locator("#netflix-intro-overlay");
        assertTrue(introOverlay.count() > 0, "Netflix intro overlay must exist in DOM");

        captureScreenshot("homepage_intro");
    }

    @Test
    @DisplayName("UI-E2E: Auth modal opens centered with clean dimensions")
    void testAuthModalDisplay() {
        openAuthModal();

        Locator authModal = page.locator("#auth-modal");
        assertTrue(authModal.isVisible(), "Auth modal should be visible");

        Locator authCard = page.locator(".auth-card");
        assertTrue(authCard.isVisible(), "Auth card must be visible inside modal");

        // Verify email input exists
        Locator emailInput = page.locator("#auth-email");
        assertTrue(emailInput.isVisible(), "Email input field should be visible");

        captureScreenshot("auth_modal_open");
    }

    @Test
    @DisplayName("UI-E2E: Quick demo accounts section is visible by default in local environment")
    void testDemoUsersSectionVisibility() {
        openAuthModal();

        Locator demoSection = page.locator("#quick-demo-section");
        assertTrue(demoSection.isVisible(), "Quick demo section should be visible in local dev profile");

        // Verify chips for demo profiles exist
        Locator demoChips = page.locator(".demo-user-chip");
        assertTrue(demoChips.count() >= 2, "Should contain demo user chips (Shivam, Isha, etc.)");

        captureScreenshot("demo_users_visible");
    }

    @Test
    @DisplayName("UI-E2E: Quick login switches active profile to Isha Agarwal and updates UI state")
    void testQuickLoginWorkflow() {
        openAuthModal();

        // Perform quick login for user_2 (Isha Agarwal)
        page.evaluate("() => App.quickLogin('user_2')");

        // Wait for sidebar user name update
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        String userName = page.locator("#sidebar-user-name").innerText();
        assertTrue(userName.contains("Isha"), "User name should display Isha Agarwal, got: " + userName);

        // Verify auth modal closed after successful login
        Locator authModal = page.locator("#auth-modal");
        assertFalse(authModal.isVisible(), "Auth modal should close after successful login");

        captureScreenshot("quick_login_isha_success");
    }
}
