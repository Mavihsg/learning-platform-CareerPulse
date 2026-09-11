package com.learning.platform.ui;

import com.learning.platform.model.Badge;
import com.learning.platform.model.Course;
import com.learning.platform.model.Enrollment;
import com.learning.platform.model.User;
import com.learning.platform.repository.BadgeRepository;
import com.learning.platform.repository.CourseRepository;
import com.learning.platform.repository.EnrollmentRepository;
import com.learning.platform.repository.UserRepository;
import com.microsoft.playwright.Locator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class DatabaseToFrontendIntegrityUiTest extends BasePlaywrightUiTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("DB-to-UI: User entities in H2 database directly match rendered values across Profile, Sidebar, and HUD")
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

        // Verify Dashboard Hero stats match DB entity
        String expectedFormattedXp = String.format("%,d XP", dbIsha.getCurrentXp());
        assertEquals(expectedFormattedXp, page.locator("#dash-hero-xp").innerText().trim(),
                "Dashboard hero XP must match DB user.currentXp");
        assertEquals("Level " + dbIsha.getCurrentLevel(), page.locator("#dash-hero-level").innerText().trim(),
                "Dashboard hero level must match DB user.currentLevel");
        assertEquals(dbIsha.getLevelTitle(), page.locator("#dash-hero-level-title").innerText().trim(),
                "Dashboard hero level title must match DB user.levelTitle");
        assertEquals(dbIsha.getStreakDays() + " Days", page.locator("#dash-hero-streak").innerText().trim(),
                "Dashboard hero streak must match DB user.streakDays");

        // Verify Profile view matches DB entity
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
        assertEquals(dbIsha.getStreakDays() + "d", page.locator("#profile-streak").innerText().trim(),
                "Profile streak must match DB user.streakDays");

        // Account Details Card in Profile
        assertEquals(dbIsha.getName(), page.locator("#profile-detail-name").innerText().trim(),
                "Profile detail full name must match DB user.name");
        assertEquals(dbIsha.getEmail(), page.locator("#profile-detail-email").innerText().trim(),
                "Profile detail email must match DB user.email");
        assertEquals(dbIsha.getCurrentRoleTitle(), page.locator("#profile-detail-role").innerText().trim(),
                "Profile detail current role must match DB user.currentRoleTitle");

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
    @DisplayName("DB-to-UI: Enrolled courses and progress in H2 database directly match My Learning view")
    void testEnrolledCoursesDirectlyMatchDatabase() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Fetch all enrollments for user_2 from H2 database
        List<Enrollment> dbEnrollments = enrollmentRepository.findByUserId("user_2");
        assertFalse(dbEnrollments.isEmpty(), "User user_2 must have enrolled courses in H2 database");

        // Navigate to My Learning
        page.locator("#nav-my-learning").click();
        page.waitForSelector("#my-learning-grid .my-learning-card", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        Locator renderedMyLearningCards = page.locator("#my-learning-grid .my-learning-card");
        assertEquals(dbEnrollments.size(), renderedMyLearningCards.count(),
                "My Learning cards count must match DB enrollment count (" + dbEnrollments.size() + ")");

        // Verify each enrolled course title appears in My Learning
        for (Enrollment enrollment : dbEnrollments) {
            Course course = courseRepository.findById(enrollment.getCourseId()).orElse(null);
            if (course != null) {
                Locator cardTitle = page.locator("#my-learning-grid .mylearning-title:has-text('" + course.getTitle() + "')");
                assertTrue(cardTitle.count() > 0,
                        "Enrolled course '" + course.getTitle() + "' must appear in My Learning");
            }
        }

        captureScreenshot("db_my_learning_verified");
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
    @DisplayName("DB-to-UI: Leaderboard podium rankings and total count directly match database")
    void testLeaderboardChampionMatchesDatabase() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Sort all users by XP descending from H2 database
        List<User> allUsersSorted = userRepository.findAll().stream()
                .sorted(Comparator.comparingInt(User::getCurrentXp).reversed())
                .collect(Collectors.toList());

        assertFalse(allUsersSorted.isEmpty(), "Users table must not be empty");
        User dbRank1 = allUsersSorted.get(0);
        User dbRank2 = allUsersSorted.size() > 1 ? allUsersSorted.get(1) : null;
        User dbRank3 = allUsersSorted.size() > 2 ? allUsersSorted.get(2) : null;

        // Navigate to Leaderboard
        page.locator("#nav-leaderboard").click();
        page.waitForSelector("#leaderboard-podium-section .podium-rank-1", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // 1. Verify Rank 1 Champion on the podium
        String podiumRank1Name = page.locator("#leaderboard-podium-section .podium-rank-1 .podium-user-name").innerText().trim();
        assertTrue(podiumRank1Name.contains(dbRank1.getName()),
                "Leaderboard Rank 1 Champion (" + podiumRank1Name + ") must match DB top user (" + dbRank1.getName() + ")");

        // 2. Verify Rank 2 on the podium
        if (dbRank2 != null) {
            String podiumRank2Name = page.locator("#leaderboard-podium-section .podium-rank-2 .podium-user-name").innerText().trim();
            assertTrue(podiumRank2Name.contains(dbRank2.getName()),
                    "Leaderboard Rank 2 (" + podiumRank2Name + ") must match DB second user (" + dbRank2.getName() + ")");
        }

        // 3. Verify Rank 3 on the podium
        if (dbRank3 != null) {
            String podiumRank3Name = page.locator("#leaderboard-podium-section .podium-rank-3 .podium-user-name").innerText().trim();
            assertTrue(podiumRank3Name.contains(dbRank3.getName()),
                    "Leaderboard Rank 3 (" + podiumRank3Name + ") must match DB third user (" + dbRank3.getName() + ")");
        }

        // 4. Verify Total Learners count matches DB users count
        String totalCountText = page.locator("#leaderboard-total-count").innerText().trim();
        assertTrue(totalCountText.contains(String.valueOf(allUsersSorted.size())),
                "Total learners counter must contain DB total users count (" + allUsersSorted.size() + ")");

        captureScreenshot("db_leaderboard_champion_verified");
    }

    @Test
    @DisplayName("DB-to-UI: Course Plan overview header and title directly match database course entity")
    void testPlanOverviewDirectlyMatchesDatabase() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        // Fetch first course from H2 database
        Course dbCourse = courseRepository.findAll().get(0);

        // Open this course plan
        page.evaluate("() => App.openPlan('" + dbCourse.getId() + "')");
        page.waitForSelector("#plan-hero-title", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(6000));

        assertEquals(dbCourse.getTitle(), page.locator("#plan-hero-title").innerText().trim(),
                "Plan hero title must match DB course.title");

        captureScreenshot("db_plan_overview_verified");
    }
}
