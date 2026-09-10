package com.learning.platform.runner;

import com.learning.platform.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class DataInitializerRunnerTest {

    @Autowired
    private DataInitializerRunner dataInitializerRunner;

    @Autowired
    private SkillRepository skillRepository;

    @Autowired
    private RoleBenchmarkRepository roleBenchmarkRepository;

    @Autowired
    private CourseRepository courseRepository;

    @Autowired
    private BadgeRepository badgeRepository;

    @Autowired
    private QuestRepository questRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("Verify flat-file seed datasets loaded completely into H2 in-memory store")
    void testFlatFileSeedingComplete() {
        assertTrue(dataInitializerRunner.isInitialized(), "Data initializer runner should be marked initialized");

        // Verify counts
        assertTrue(skillRepository.count() >= 15, "Should have seeded skills");
        assertTrue(roleBenchmarkRepository.count() >= 3, "Should have seeded roles");
        assertTrue(courseRepository.count() >= 4, "Should have seeded courses");
        assertTrue(badgeRepository.count() >= 5, "Should have seeded badges");
        assertTrue(questRepository.count() >= 4, "Should have seeded quests");
        assertTrue(userRepository.count() >= 2, "Should have seeded users");

        // Verify relationships
        var courses = courseRepository.findAll();
        for (var course : courses) {
            assertNotNull(course.getTitle());
            assertFalse(course.getModules().isEmpty(), "Course should have modules attached");
            assertNotNull(course.getModules().get(0).getLessons(), "Module should have lessons list");
        }

        var manish = userRepository.findById("user_1");
        assertTrue(manish.isPresent(), "Manish Kessler profile should be present");
        assertFalse(manish.get().getSkills().isEmpty(), "Manish should have active skills");
    }
}
