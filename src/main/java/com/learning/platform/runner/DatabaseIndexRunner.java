package com.learning.platform.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Creates secondary database indexes on all high-frequency query paths
 * across PostgreSQL tables on application startup to ensure instant lookup
 * performance and eliminate table scans.
 */
@Component
@Order(5)
public class DatabaseIndexRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatabaseIndexRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public DatabaseIndexRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking and creating database indexes for optimal query performance...");
        long start = System.currentTimeMillis();

        // Deduplicate any existing duplicate enrollments in database by merging lessons into lowest id
        try {
            jdbcTemplate.execute(
                "UPDATE enrollment_completed_lessons " +
                "SET enrollment_id = target.min_id " +
                "FROM (" +
                "    SELECT user_id, course_id, MIN(id) as min_id " +
                "    FROM enrollments " +
                "    GROUP BY user_id, course_id " +
                "    HAVING COUNT(*) > 1" +
                ") target " +
                "JOIN enrollments dup " +
                "ON dup.user_id = target.user_id AND dup.course_id = target.course_id AND dup.id != target.min_id " +
                "WHERE enrollment_completed_lessons.enrollment_id = dup.id"
            );
            jdbcTemplate.execute(
                "DELETE FROM enrollments " +
                "WHERE id IN (" +
                "    SELECT dup.id " +
                "    FROM enrollments dup " +
                "    JOIN (" +
                "        SELECT user_id, course_id, MIN(id) as min_id " +
                "        FROM enrollments " +
                "        GROUP BY user_id, course_id " +
                "        HAVING COUNT(*) > 1" +
                "    ) target " +
                "    ON dup.user_id = target.user_id AND dup.course_id = target.course_id AND dup.id != target.min_id" +
                ")"
            );
        } catch (Exception e) {
            log.debug("Deduplication note: {}", e.getMessage());
        }

        String[] indexStatements = new String[]{
                // Enrollments indexes
                "CREATE INDEX IF NOT EXISTS idx_enrollments_user_id ON enrollments(user_id)",
                "CREATE UNIQUE INDEX IF NOT EXISTS uq_enrollments_user_course ON enrollments(user_id, course_id)",
                "CREATE INDEX IF NOT EXISTS idx_enrollments_user_core ON enrollments(user_id, is_core_track)",
                "CREATE INDEX IF NOT EXISTS idx_enrollments_status ON enrollments(status)",

                // Study logs indexes
                "CREATE INDEX IF NOT EXISTS idx_study_logs_user_date ON study_logs(user_id, log_date)",
                "CREATE INDEX IF NOT EXISTS idx_study_logs_date ON study_logs(log_date)",

                // Course hierarchy indexes
                "CREATE INDEX IF NOT EXISTS idx_course_modules_course_id ON course_modules(course_id)",
                "CREATE INDEX IF NOT EXISTS idx_lessons_module_id ON lessons(module_id)",

                // User attributes and collections
                "CREATE INDEX IF NOT EXISTS idx_user_skills_user_id ON user_skills(user_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_badges_user_id ON user_unlocked_badges(user_id)",
                "CREATE INDEX IF NOT EXISTS idx_user_quests_user_id ON user_completed_quests(user_id)",
                "CREATE INDEX IF NOT EXISTS idx_enrollment_comp_lessons ON enrollment_completed_lessons(enrollment_id)",
                "CREATE INDEX IF NOT EXISTS idx_enrollment_comp_modules ON enrollment_completed_modules(enrollment_id)"
        };

        int createdCount = 0;
        for (String sql : indexStatements) {
            try {
                jdbcTemplate.execute(sql);
                createdCount++;
            } catch (Exception e) {
                log.debug("Index creation note for statement '{}': {}", sql, e.getMessage());
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.info("Database index verification completed in {} ms ({} statements processed).", duration, createdCount);
    }
}
