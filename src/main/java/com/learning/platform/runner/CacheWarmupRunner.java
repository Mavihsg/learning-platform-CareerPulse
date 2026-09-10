package com.learning.platform.runner;

import com.learning.platform.model.User;
import com.learning.platform.repository.UserRepository;
import com.learning.platform.service.AnalyticsService;
import com.learning.platform.service.CourseService;
import com.learning.platform.service.UserService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Pre-warms Spring caches on application startup so that the very first request
 * by any user (courses, enrolled plans, dashboard, profile) returns in milliseconds.
 */
@Component
@Order(100)
public class CacheWarmupRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CacheWarmupRunner.class);

    private final CourseService courseService;
    private final UserService userService;
    private final AnalyticsService analyticsService;
    private final UserRepository userRepository;

    public CacheWarmupRunner(CourseService courseService,
                             UserService userService,
                             AnalyticsService analyticsService,
                             UserRepository userRepository) {
        this.courseService = courseService;
        this.userService = userService;
        this.analyticsService = analyticsService;
        this.userRepository = userRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Starting startup cache pre-warm for instant sub-millisecond API responses...");
        long start = System.currentTimeMillis();
        try {
            // 1. Warm up courses
            courseService.getAllCourses();

            // 2. Warm up all users
            userService.getAllUsers();

            // 3. Warm up shared weekly leaderboard
            try {
                analyticsService.getLeaderboard("WEEKLY", null);
            } catch (Exception ignored) {}

            // 4. Warm up per-user hot paths cleanly
            List<User> users = userRepository.findAll();
            for (User u : users) {
                try {
                    courseService.getEnrolledCourses(u.getId());
                    analyticsService.getDashboardOverview(u.getId());
                } catch (Exception e) {
                    log.debug("Warmup note for user {}: {}", u.getId(), e.getMessage());
                }
            }

            long duration = System.currentTimeMillis() - start;
            log.info("Cache pre-warming completed successfully in {} ms! All hot paths ready in memory.", duration);
        } catch (Exception e) {
            log.warn("Cache pre-warming encountered non-critical exception: {}", e.getMessage());
        }
    }
}
