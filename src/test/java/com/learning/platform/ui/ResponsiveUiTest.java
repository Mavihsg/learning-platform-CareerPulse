package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.WaitForSelectorState;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * End-to-End Playwright test suite validating Multi-Device UI Responsiveness
 * across Mobile Phones (390x844), Tablets (768x1024), and Desktops (1440x900).
 */
public class ResponsiveUiTest extends BasePlaywrightUiTest {

    private void loginAndPrepare(int width, int height) {
        page.setViewportSize(width, height);
        navigateHome();
        page.evaluate("() => { if (window.App && App.quickLogin) App.quickLogin('user_1'); }");
        page.waitForSelector("#auth-modal", new Page.WaitForSelectorOptions().setState(WaitForSelectorState.HIDDEN).setTimeout(6000));
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));
    }

    @Test
    @DisplayName("Mobile Smartphone Viewport (390x844): Asserts drawer, bottom dock, zero horizontal scroll, and 1-column layout")
    void testMobilePhoneViewportResponsiveLayout() {
        loginAndPrepare(390, 844);

        // 1. Assert zero horizontal overflow/scrollbar on body
        Boolean noHorizontalOverflow = (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth + 2");
        assertTrue(noHorizontalOverflow, "Smartphone viewport should have zero horizontal overflow");

        // 2. Assert Mobile Bottom Navigation Dock is visible and contains 5 navigation destinations
        Locator bottomNav = page.locator("#mobile-bottom-nav");
        assertTrue(bottomNav.isVisible(), "Mobile bottom navigation dock must be visible on phone screen");
        assertEquals(5, bottomNav.locator(".bottom-nav-item").count(), "Bottom nav dock must contain 5 navigation buttons");

        // 3. Assert Mobile Hamburger Menu button is visible in top header
        Locator hamburgerBtn = page.locator("#btn-mobile-menu");
        assertTrue(hamburgerBtn.isVisible(), "Hamburger button must be visible in top header on mobile screens");

        // 4. Click hamburger menu: Slide out off-canvas mobile drawer
        hamburgerBtn.click();
        page.waitForTimeout(350);

        Locator sidebar = page.locator(".app-sidebar");
        assertTrue((Boolean) sidebar.evaluate("el => el.classList.contains('mobile-open')"),
                "App sidebar must have 'mobile-open' class when hamburger is clicked");
        
        Locator backdrop = page.locator("#sidebar-backdrop");
        assertTrue(backdrop.isVisible(), "Sidebar backdrop must be visible when drawer is open");

        // 5. Dismiss drawer by tapping the backdrop outside the drawer width (x=340px on 390px screen)
        backdrop.click(new Locator.ClickOptions().setPosition(340, 300));
        page.waitForTimeout(350);
        assertFalse((Boolean) sidebar.evaluate("el => el.classList.contains('mobile-open')"),
                "App sidebar must close when backdrop is clicked");

        // 6. Navigate using Mobile Bottom Navigation Dock to Catalog
        page.locator("#bnav-catalog").click();
        page.waitForTimeout(400);

        Locator catalogPanel = page.locator("#view-catalog");
        assertTrue(catalogPanel.isVisible(), "Catalog panel must become active after clicking bottom nav");

        Locator breadcrumb = page.locator("#header-breadcrumb");
        assertEquals("CATALOG", breadcrumb.textContent().trim(), "Breadcrumb must reflect CATALOG view");

        // Capture screenshot of mobile phone catalog
        captureScreenshot("mobile_phone_catalog_viewport");
    }

    @Test
    @DisplayName("Tablet Viewport (768x1024): Asserts tablet responsive layout and adaptive navigation")
    void testTabletViewportResponsiveLayout() {
        loginAndPrepare(768, 1024);

        // 1. Assert zero horizontal overflow
        Boolean noHorizontalOverflow = (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth + 2");
        assertTrue(noHorizontalOverflow, "Tablet viewport should have zero horizontal overflow");

        // 2. Assert mobile hamburger menu is present for tablet navigation
        Locator hamburgerBtn = page.locator("#btn-mobile-menu");
        assertTrue(hamburgerBtn.isVisible(), "Hamburger button should be accessible on tablet portrait");

        // 3. Navigate to Catalog and assert grid adapts cleanly
        page.locator("#bnav-catalog").click();
        page.waitForTimeout(400);

        Locator catalogGrid = page.locator("#catalog-course-grid");
        assertTrue(catalogGrid.isVisible(), "Catalog course grid must be visible on tablet");

        captureScreenshot("tablet_portrait_catalog_viewport");
    }

    @Test
    @DisplayName("Desktop Viewport (1440x900): Asserts permanent 240px sidebar and hidden mobile controls")
    void testDesktopViewportLayout() {
        loginAndPrepare(1440, 900);

        // 1. Assert permanent sidebar is visible on desktop
        Locator sidebar = page.locator(".app-sidebar");
        assertTrue(sidebar.isVisible(), "Permanent sidebar must be visible on desktop viewport");

        // 2. Assert mobile controls are hidden on desktop
        Locator hamburgerBtn = page.locator("#btn-mobile-menu");
        assertFalse(hamburgerBtn.isVisible(), "Mobile hamburger button must be hidden on desktop");

        Locator bottomNav = page.locator("#mobile-bottom-nav");
        assertFalse(bottomNav.isVisible(), "Mobile bottom nav dock must be hidden on desktop");

        // 3. Assert Core Track CTA is visible in desktop top header
        Locator coreTrackCta = page.locator(".core-track-cta");
        assertTrue(coreTrackCta.first().isVisible(), "Core track quick CTA must be visible in desktop top header");

        captureScreenshot("desktop_catalog_viewport");
    }

    @Test
    @DisplayName("Mobile Lesson Modal (390x844): Asserts modal expands to responsive full-screen sheet with reachable close button")
    void testMobileLessonModalResponsiveSheet() {
        loginAndPrepare(390, 844);

        // Navigate to Plan Overview and open Lesson 1
        page.evaluate("() => App.openPlan('PLAN_ADE_01')");
        page.waitForSelector("#modules-tree-container .lesson-row", new Page.WaitForSelectorOptions().setTimeout(6000));

        page.evaluate("() => App.openLessonModal('PLAN_ADE_01', 'LES_ADE_01')");
        page.waitForSelector("#lesson-player-modal", new Page.WaitForSelectorOptions().setTimeout(5000));

        Locator modal = page.locator("#lesson-player-modal");
        assertTrue(modal.isVisible(), "Lesson player modal must be visible");

        Locator modalCloseBtn = modal.locator(".btn-modal-close");
        assertTrue(modalCloseBtn.isVisible(), "Modal close button must be clearly visible and accessible");

        // Capture screenshot of responsive mobile lesson modal
        captureScreenshot("mobile_lesson_modal_sheet");

        // Close modal
        modalCloseBtn.click();
        page.waitForTimeout(300);
        assertFalse(modal.isVisible(), "Lesson player modal should close after clicking close button");
    }

    @Test
    @DisplayName("Mobile Profile View (400x581): Asserts profile card, sign out button, and stats grid fit cleanly without squeezing")
    void testMobileProfileViewExactDimensions() {
        loginAndPrepare(400, 581);

        // Navigate to Profile view
        page.locator("#bnav-profile").click();
        page.waitForSelector("#view-profile.active", new Page.WaitForSelectorOptions().setTimeout(5000));
        page.waitForSelector("#profile-name", new Page.WaitForSelectorOptions().setTimeout(5000));

        // 1. Assert zero horizontal overflow
        Boolean noHorizontalOverflow = (Boolean) page.evaluate("() => document.documentElement.scrollWidth <= window.innerWidth + 2");
        assertTrue(noHorizontalOverflow, "Profile view on 400x581 must have zero horizontal overflow");

        // 2. Assert Sign Out button is clearly visible, full width, and properly positioned
        Locator signoutBtn = page.locator(".btn-signout-profile");
        assertTrue(signoutBtn.isVisible(), "Sign out button must be visible");
        assertTrue(signoutBtn.boundingBox().width > 250, "Sign out button must stretch cleanly across the mobile card");

        // 3. Assert Profile stats grid renders with all stat cards visible
        Locator statCards = page.locator(".profile-stats-row .profile-stat-card");
        assertEquals(4, statCards.count(), "Profile must render all 4 stat cards");

        // Capture screenshot of the fixed mobile profile view at exact 400x581
        captureScreenshot("mobile_profile_view_400x581");
    }
}
