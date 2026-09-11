package com.learning.platform.ui;

import com.learning.platform.model.Badge;
import com.learning.platform.model.Course;
import com.learning.platform.model.User;
import com.learning.platform.repository.BadgeRepository;
import com.learning.platform.repository.CourseRepository;
import com.learning.platform.repository.UserRepository;
import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseToFrontendIntegrityUiTest extends BasePlaywrightUiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Test
    @DisplayName("DB-to-UI: User entities in H2 database directly match rendered values in frontend DOM")
    void testUserEntitiesDirectlyMatchFrontend() {
        navigateHome();

        // 1. Test Isha Agarwal (user_2) directly from Database
        User dbIsha = userRepository.findById("user_2")
                .orElseThrow(() -> new AssertionError("User user_2 must exist in H2 database"));

        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Verify sidebar matches DB entity
        assertEquals(dbIsha.getName(), page.locator("#sidebar-user-name").innerText().trim(),
                "Sidebar user name must match DB user.name");
        assertEquals(dbIsha.getCurrentRoleTitle(), page.locator("#sidebar-user-role").innerText().trim(),
                "Sidebar user role must match DB user.currentRoleTitle");

        // Verify profile view matches DB entity
        page.evaluate("() => App.navigate('profile')");
        page.waitForSelector("#profile-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        assertEquals(dbIsha.getName(), page.locator("#profile-name").innerText().trim(),
                "Profile name must match DB user.name");
        assertEquals(dbIsha.getCurrentRoleTitle(), page.locator("#profile-role-title").innerText().trim(),
                "Profile role title must match DB user.currentRoleTitle");
        assertEquals(dbIsha.getEmail(), page.locator("#profile-email").innerText().trim(),
                "Profile email must match DB user.email");
        assertTrue(page.locator("#profile-level").innerText().trim().contains(String.valueOf(dbIsha.getCurrentLevel())),
                "Profile level must contain DB user.currentLevel (" + dbIsha.getCurrentLevel() + ")");
        assertEquals(dbIsha.getLevelTitle(), page.locator("#profile-level-title").innerText().trim(),
                "Profile level title must match DB user.levelTitle");

        String formattedDbXp = String.format("%,d", dbIsha.getCurrentXp());
        String uiXp = page.locator("#profile-xp").innerText().trim();
        assertTrue(uiXp.contains(formattedDbXp) || uiXp.contains(String.valueOf(dbIsha.getCurrentXp())),
                "Profile XP must match DB user.currentXp (" + formattedDbXp + "), but found: " + uiXp);

        // 2. Test Shivam Gupta (user_1) directly from Database
        User dbShivam = userRepository.findById("user_1")
                .orElseThrow(() -> new AssertionError("User user_1 must exist in H2 database"));

        page.evaluate("() => App.quickLogin('user_1')");
        page.waitForTimeout(500);
        page.evaluate("() => App.navigate('profile')");
        page.waitForSelector("#profile-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        assertEquals(dbShivam.getName(), page.locator("#profile-name").innerText().trim(),
                "Profile name must match DB user_1 name");
        assertEquals(dbShivam.getCurrentRoleTitle(), page.locator("#profile-role-title").innerText().trim(),
                "Profile role title must match DB user_1 role");
        assertEquals(dbShivam.getEmail(), page.locator("#profile-email").innerText().trim(),
                "Profile email must match DB user_1 email");
        assertTrue(page.locator("#profile-level").innerText().trim().contains(String.valueOf(dbShivam.getCurrentLevel())),
                "Profile level must contain DB user_1 currentLevel");

        captureScreenshot("db_user_integrity_verified");
    }

    @Test
    @DisplayName("DB-to-UI: All courses in H2 database directly match courses rendered in Catalog")
    void testCourseCatalogDirectlyMatchesDatabase() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Fetch all courses directly from H2 database
        List<Course> dbCourses = courseRepository.findAll();
        assertFalse(dbCourses.isEmpty(), "H2 database course table must contain courses");

        // Navigate to Catalog
        page.locator("#nav-catalog").click();
        page.waitForSelector("#catalog-course-grid .catalog-card", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        Locator renderedCards = page.locator("#catalog-course-grid .catalog-card");
        assertEquals(dbCourses.size(), renderedCards.count(),
                "Catalog card count must exactly equal DB course count (" + dbCourses.size() + ")");

        // Verify each database course title is rendered on the frontend catalog
        for (Course course : dbCourses) {
            Locator matchingTitle = page.locator("#catalog-course-grid .core-course-title:has-text('" + course.getTitle() + "')");
            assertTrue(matchingTitle.count() > 0,
                    "Database course '" + course.getTitle() + "' must be rendered on the frontend catalog");
        }

        captureScreenshot("db_courses_catalog_verified");
    }

    @Test
    @DisplayName("DB-to-UI: Badges and earned count directly match database")
    void testBadgesDirectlyMatchDatabase() {
        navigateHome();
        User dbIsha = userRepository.findById("user_2")
                .orElseThrow(() -> new AssertionError("User user_2 must exist in H2 database"));

        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Verify Dashboard badges chips
        page.waitForSelector("#dash-badges-grid .dash-badge-chip", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));
        Locator dashBadges = page.locator("#dash-badges-grid .dash-badge-chip");
        assertTrue(dashBadges.count() > 0, "Dashboard must render badge chips from system data");

        // Navigate to Profile view
        page.evaluate("() => App.navigate('profile')");
        page.waitForSelector("#profile-badges", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Verify earned badges count on profile matches DB unlocked badges count
        String badgesEarnedText = page.locator("#profile-badges").innerText().trim();
        assertEquals(String.valueOf(dbIsha.getUnlockedBadgeIds().size()), badgesEarnedText,
                "Profile earned badges count must match DB user.unlockedBadgeIds.size()");

        captureScreenshot("db_badges_profile_verified");
    }

    @Test
    @DisplayName("DB-to-UI: Leaderboard Rank #1 Champion matches database highest XP user")
    void testLeaderboardChampionMatchesDatabase() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Find user with highest XP in H2 database
        List<User> allUsers = userRepository.findAll();
        User dbChampion = allUsers.stream()
                .max((u1, u2) -> Integer.compare(u1.getCurrentXp(), u2.getCurrentXp()))
                .orElseThrow();

        // Navigate to Leaderboard
        page.locator("#nav-leaderboard").click();
        page.waitForSelector("#leaderboard-podium-section .podium-rank-1", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Check Rank 1 champion on the podium
        String podiumChampionName = page.locator("#leaderboard-podium-section .podium-rank-1 .podium-user-name").innerText().trim();
        assertTrue(podiumChampionName.contains(dbChampion.getName()),
                "Leaderboard Rank 1 Champion (" + podiumChampionName + ") must match DB top user (" + dbChampion.getName() + ")");

        captureScreenshot("db_leaderboard_champion_verified");
    }
}
