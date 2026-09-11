package com.learning.platform.ui;

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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.io.File;
import java.nio.file.Files;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

class NeonDatabaseToFrontendIntegrityUiTest extends BasePlaywrightUiTest {

    static {
        // Automatically load local .env credentials if present without committing secrets to git
        try {
            File envFile = new File(".env");
            if (envFile.exists()) {
                Files.readAllLines(envFile.toPath()).forEach(line -> {
                    line = line.trim();
                    if (!line.startsWith("#") && line.contains("=")) {
                        String[] parts = line.split("=", 2);
                        String key = parts[0].trim();
                        String val = parts[1].trim();
                        if (System.getProperty(key) == null && System.getenv(key) == null) {
                            System.setProperty(key, val);
                        }
                    }
                });
            }
        } catch (Exception ignored) {}
    }

    @DynamicPropertySource
    static void configureNeonProperties(DynamicPropertyRegistry registry) {
        String url = System.getProperty("SPRING_DATASOURCE_URL", System.getenv("SPRING_DATASOURCE_URL"));
        String user = System.getProperty("SPRING_DATASOURCE_USERNAME", System.getenv("SPRING_DATASOURCE_USERNAME"));
        String pass = System.getProperty("SPRING_DATASOURCE_PASSWORD", System.getenv("SPRING_DATASOURCE_PASSWORD"));

        if (url != null && !url.isBlank()) registry.add("spring.datasource.url", () -> url);
        if (user != null && !user.isBlank()) registry.add("spring.datasource.username", () -> user);
        if (pass != null && !pass.isBlank()) registry.add("spring.datasource.password", () -> pass);

        registry.add("spring.datasource.driverClassName", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.database-platform", () -> "org.hibernate.dialect.PostgreSQLDialect");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "update");
        registry.add("app.features.demo-users-enabled", () -> "true");
    }

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("NEON-DB-to-UI: Real user entities in cloud Neon PostgreSQL match rendered frontend values")
    void testNeonUserEntitiesMatchFrontend() {
        navigateHome();

        // 1. Query user_2 (Isha) from cloud Neon PostgreSQL
        User neonIsha = userRepository.findById("user_2")
                .orElseThrow(() -> new AssertionError("User user_2 must exist in Neon PostgreSQL database"));

        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(8000));

        // Verify sidebar matches Neon PostgreSQL DB
        assertEquals(neonIsha.getName(), page.locator("#sidebar-user-name").innerText().trim(),
                "Sidebar user name must match Neon DB user.name");
        assertEquals(neonIsha.getCurrentRoleTitle(), page.locator("#sidebar-user-role").innerText().trim(),
                "Sidebar user role must match Neon DB user.currentRoleTitle");

        // Verify Dashboard Hero stats match Neon DB
        String expectedFormattedXp = String.format("%,d XP", neonIsha.getCurrentXp());
        assertEquals(expectedFormattedXp, page.locator("#dash-hero-xp").innerText().trim(),
                "Dashboard hero XP must match Neon DB user.currentXp");
        assertEquals("Level " + neonIsha.getCurrentLevel(), page.locator("#dash-hero-level").innerText().trim(),
                "Dashboard hero level must match Neon DB user.currentLevel");
        assertEquals(neonIsha.getLevelTitle(), page.locator("#dash-hero-level-title").innerText().trim(),
                "Dashboard hero level title must match Neon DB user.levelTitle");

        // Verify Profile page matches Neon DB
        page.evaluate("() => App.navigate('profile')");
        page.waitForSelector("#profile-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(8000));

        assertEquals(neonIsha.getName(), page.locator("#profile-name").innerText().trim(),
                "Profile name must match Neon DB user.name");
        assertEquals(neonIsha.getCurrentRoleTitle(), page.locator("#profile-role-title").innerText().trim(),
                "Profile role title must match Neon DB user.currentRoleTitle");
        assertEquals(neonIsha.getEmail(), page.locator("#profile-email").innerText().trim(),
                "Profile email must match Neon DB user.email");
        assertTrue(page.locator("#profile-level").innerText().trim().contains(String.valueOf(neonIsha.getCurrentLevel())),
                "Profile level must contain Neon DB user.currentLevel");
        assertEquals(neonIsha.getLevelTitle(), page.locator("#profile-level-title").innerText().trim(),
                "Profile level title must match Neon DB user.levelTitle");

        captureScreenshot("neon_db_user_integrity_verified");
    }

    @Test
    @DisplayName("NEON-DB-to-UI: All courses in cloud Neon PostgreSQL match Course Catalog")
    void testNeonCourseCatalogMatchesFrontend() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(8000));

        // Fetch all courses directly from Neon PostgreSQL
        List<Course> neonCourses = courseRepository.findAll();
        assertFalse(neonCourses.isEmpty(), "Neon PostgreSQL course table must contain courses");

        // Navigate to Catalog
        page.locator("#nav-catalog").click();
        page.waitForSelector("#catalog-course-grid .catalog-card", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(8000));

        Locator renderedCards = page.locator("#catalog-course-grid .catalog-card");
        assertEquals(neonCourses.size(), renderedCards.count(),
                "Catalog card count must equal Neon DB course count (" + neonCourses.size() + ")");

        // Verify each course from Neon DB is rendered on the frontend catalog
        for (Course course : neonCourses) {
            Locator matchingTitle = page.locator("#catalog-course-grid .core-course-title:has-text('" + course.getTitle() + "')");
            assertTrue(matchingTitle.count() > 0,
                    "Neon DB course '" + course.getTitle() + "' must be rendered on the frontend catalog");
        }

        captureScreenshot("neon_db_catalog_verified");
    }

    @Test
    @DisplayName("NEON-DB-to-UI: Enrolled courses for active learner match My Learning view")
    void testNeonEnrolledCoursesMatchFrontend() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(8000));

        // Fetch enrollments from Neon PostgreSQL
        List<Enrollment> neonEnrollments = enrollmentRepository.findByUserId("user_2");
        assertFalse(neonEnrollments.isEmpty(), "User user_2 must have enrolled courses in Neon DB");

        // Navigate to My Learning
        page.locator("#nav-my-learning").click();
        page.waitForSelector("#my-learning-grid .my-learning-card", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(8000));

        Locator renderedCards = page.locator("#my-learning-grid .my-learning-card");
        assertEquals(neonEnrollments.size(), renderedCards.count(),
                "My Learning cards count must match Neon DB enrollment count (" + neonEnrollments.size() + ")");

        captureScreenshot("neon_db_my_learning_verified");
    }

    @Test
    @DisplayName("NEON-DB-to-UI: Top leaderboard champion matches highest XP user in Neon PostgreSQL")
    void testNeonLeaderboardMatchesFrontend() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(8000));

        // Find user with highest XP in Neon PostgreSQL
        List<User> allUsers = userRepository.findAll().stream()
                .sorted(Comparator.comparingInt(User::getCurrentXp).reversed())
                .collect(Collectors.toList());

        assertFalse(allUsers.isEmpty(), "Neon DB users table must not be empty");
        User topUser = allUsers.get(0);

        // Navigate to Leaderboard
        page.locator("#nav-leaderboard").click();
        page.waitForSelector("#leaderboard-podium-section .podium-rank-1", new com.microsoft.playwright.Page.WaitForSelectorOptions().setTimeout(8000));

        String podiumChampionName = page.locator("#leaderboard-podium-section .podium-rank-1 .podium-user-name").innerText().trim();
        assertTrue(podiumChampionName.contains(topUser.getName()),
                "Leaderboard Rank 1 Champion (" + podiumChampionName + ") must match Neon DB top user (" + topUser.getName() + ")");

        captureScreenshot("neon_db_leaderboard_verified");
    }
}
