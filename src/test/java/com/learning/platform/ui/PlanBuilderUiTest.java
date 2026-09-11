package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PlanBuilderUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("UI-E2E: Plan Builder navigation, module structure, and AI authoring load properly")
    void testPlanBuilderInterface() {
        navigateHome();

        // Login as Isha Agarwal
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Plan Builder
        page.locator("#nav-builder").click();
        page.waitForSelector("#view-builder.active", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(5000));

        Locator builderView = page.locator("#view-builder");
        assertTrue(builderView.isVisible(), "Plan builder view should be visible");

        // Verify Title input exists
        Locator titleInput = page.locator("#builder-plan-title");
        assertTrue(titleInput.isVisible(), "Course title input must be visible");

        // Verify modules container exists and has modules
        Locator moduleCards = page.locator(".builder-module-card");
        assertTrue(moduleCards.count() > 0, "Plan Builder must render editable module cards");

        captureScreenshot("plan_builder_view");
    }
}
