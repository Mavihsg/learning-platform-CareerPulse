package com.learning.platform.service;

import com.learning.platform.repository.*;
import com.learning.platform.runner.DataInitializerRunner;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class SystemService {

    private final DataInitializerRunner dataInitializerRunner;
    private final SkillRepository skillRepository;
    private final RoleBenchmarkRepository roleBenchmarkRepository;
    private final CourseRepository courseRepository;
    private final BadgeRepository badgeRepository;
    private final QuestRepository questRepository;
    private final UserRepository userRepository;

    public SystemService(DataInitializerRunner dataInitializerRunner,
                         SkillRepository skillRepository,
                         RoleBenchmarkRepository roleBenchmarkRepository,
                         CourseRepository courseRepository,
                         BadgeRepository badgeRepository,
                         QuestRepository questRepository,
                         UserRepository userRepository) {
        this.dataInitializerRunner = dataInitializerRunner;
        this.skillRepository = skillRepository;
        this.roleBenchmarkRepository = roleBenchmarkRepository;
        this.courseRepository = courseRepository;
        this.badgeRepository = badgeRepository;
        this.questRepository = questRepository;
        this.userRepository = userRepository;
    }

    public Map<String, Object> getSystemHealth() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("database", "H2_IN_MEMORY");
        health.put("seedDataLoaded", dataInitializerRunner.isInitialized());
        return health;
    }

    public Map<String, Object> getSystemStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("totalSkills", skillRepository.count());
        stats.put("totalRoles", roleBenchmarkRepository.count());
        stats.put("totalCourses", courseRepository.count());
        stats.put("totalBadges", badgeRepository.count());
        stats.put("totalQuests", questRepository.count());
        stats.put("totalUsers", userRepository.count());
        stats.put("initialized", dataInitializerRunner.isInitialized());
        return stats;
    }
}
