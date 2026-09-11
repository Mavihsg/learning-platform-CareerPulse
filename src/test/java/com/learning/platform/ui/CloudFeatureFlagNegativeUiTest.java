package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import static org.junit.jupiter.api.Assertions.*;

@TestPropertySource(properties = {
        "app.features.demo-users-enabled=false"
})
class CloudFeatureFlagNegativeUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("UI-E2E Cloud: Demo user accounts are hidden and quick login is guarded when feature flag is disabled")
    void testDemoUsersHiddenInCloudMode() {
        openAuthModal();

        // In cloud / production mode, quick-demo-section should be hidden (display: none)
        Locator demoSection = page.locator("#quick-demo-section");
        assertFalse(demoSection.isVisible(), "Quick demo section must be hidden in cloud environment");

        // Attempting to call quickLogin directly should be blocked by the App.quickLogin runtime guard
        // Override alert dialog so it does not block the browser
        page.onDialog(dialog -> dialog.accept());
        page.evaluate("() => App.quickLogin('user_2')");

        // Verify auth modal remains open because quick login was safely rejected
        Locator authModal = page.locator("#auth-modal");
        assertTrue(authModal.isVisible(), "Auth modal should stay open because quick login was rejected");

        captureScreenshot("cloud_demo_users_hidden");
    }
}
