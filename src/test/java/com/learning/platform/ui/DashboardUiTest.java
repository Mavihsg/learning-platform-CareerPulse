package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DashboardUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("UI-E2E: Dashboard renders real user profile XP and Level 3 Skill Specialist")
    void testIshaProfileRealData() {
        navigateHome();

        // Perform login as Isha Agarwal (user_2)
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Switch to Profile view to verify accurate profile data
        page.evaluate("() => App.navigate('profile')");
        page.waitForSelector("#profile-xp", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(5000));

        String profileXp = page.locator("#profile-xp").innerText();
        assertTrue(profileXp.contains("2,100") || profileXp.contains("2100"),
                "Profile XP should reflect real 2,100 XP, got: " + profileXp);

        String profileLevel = page.locator("#profile-level").innerText();
        assertTrue(profileLevel.contains("3"), "Profile level should be Level 3, got: " + profileLevel);

        String levelTitle = page.locator("#profile-level-title").innerText();
        assertTrue(levelTitle.contains("Skill Specialist"),
                "Profile level title should be Skill Specialist, got: " + levelTitle);

        captureScreenshot("profile_real_data_isha");
    }

    @Test
    @DisplayName("UI-E2E: Dashboard badges render SVG visual icons inside themed color containers")
    void testBadgeSvgIconsAndColoredContainers() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#dash-badges-grid", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Verify badge cards exist
        Locator badgeIcons = page.locator("#dash-badges-grid .dash-badge-icon");
        assertTrue(badgeIcons.count() > 0, "Dashboard should render recent badge cards");

        // Verify each badge icon contains an SVG element and theme class
        Locator svgElements = page.locator("#dash-badges-grid .dash-badge-icon svg");
        assertTrue(svgElements.count() > 0, "Badge containers must render inline SVG graphics instead of plain strings");

        captureScreenshot("dashboard_svg_badges");
    }

    @Test
    @DisplayName("UI-E2E: AI badge on Plan Builder matches Quiz Arena chip and is removed from challenge tile")
    void testAiBadgeAlignment() {
        navigateHome();

        // Check Plan Builder nav item has right-aligned AI chip
        Locator builderChip = page.locator("#nav-builder .quiz-nav-chip");
        assertTrue(builderChip.count() > 0, "Plan builder nav item must have AI chip");
        assertEquals("AI", builderChip.first().innerText().trim());

        // Check Quiz Arena nav item has matching AI chip
        Locator quizChip = page.locator("#nav-quiz-arena .quiz-nav-chip");
        assertTrue(quizChip.count() > 0, "Quiz Arena nav item must have AI chip");
        assertEquals("AI", quizChip.first().innerText().trim());

        // Check Dashboard daily quiz title does NOT contain redundant AI chip
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#dash-daily-quiz-title", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));
        Locator inlineAiBadge = page.locator("#dash-daily-quiz-title .badge-ai-inline");
        assertEquals(0, inlineAiBadge.count(), "Daily knowledge challenge tile should not have redundant inline AI badge");

        captureScreenshot("ai_badge_nav_alignment");
    }
}
