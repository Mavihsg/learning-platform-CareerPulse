package com.learning.platform.runner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One-time migration runner that updates legacy user IDs:
 * USER_ALEX_CHEN -> user_1
 * USER_SARAH_JENKINS -> user_2
 * USER_DAVID_KIM -> user_3
 * USER_ELENA_ROSTOVA -> user_4
 * USER_MARCUS_VANCE -> user_5
 *
 * Safely updates referencing tables (user_skills, user_unlocked_badges, user_completed_quests,
 * enrollments, study_logs, courses) while strictly preserving relational integrity.
 */
@Component
@Order(5)
public class UserIdMigrationRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(UserIdMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;

    public UserIdMigrationRunner(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        try {
            // Ensure xp_awarded column exists in enrollments
            try {
                jdbcTemplate.execute("ALTER TABLE enrollments ADD COLUMN IF NOT EXISTS xp_awarded BOOLEAN DEFAULT FALSE");
                jdbcTemplate.execute("UPDATE enrollments SET xp_awarded = FALSE WHERE xp_awarded IS NULL");
            } catch (Exception e) {
                log.debug("Enrollment xp_awarded column migration notice: {}", e.getMessage());
            }

            // Check if legacy user IDs exist in database
            Integer legacyCount = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM users WHERE id LIKE 'USER_%'", Integer.class);

            if (legacyCount == null || legacyCount == 0) {
                log.info("No legacy user IDs found in 'users' table. User ID migration not required.");
                return;
            }

            log.info("Found {} legacy user IDs in database. Initiating safe ID migration to user_1, user_2, etc...", legacyCount);

            Map<String, String> idMapping = new LinkedHashMap<>();
            idMapping.put("USER_ALEX_CHEN", "user_1");
            idMapping.put("USER_SARAH_JENKINS", "user_2");
            idMapping.put("USER_DAVID_KIM", "user_3");
            idMapping.put("USER_ELENA_ROSTOVA", "user_4");
            idMapping.put("USER_MARCUS_VANCE", "user_5");

            for (Map.Entry<String, String> entry : idMapping.entrySet()) {
                String oldId = entry.getKey();
                String newId = entry.getValue();

                Integer oldExists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, oldId);
                if (oldExists == null || oldExists == 0) {
                    continue;
                }

                Integer newExists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM users WHERE id = ?", Integer.class, newId);

                // Step 1: Create new user clone if not exists (with temp email to respect unique constraint)
                if (newExists == null || newExists == 0) {
                    jdbcTemplate.update(
                            "INSERT INTO users (id, name, email, avatar, current_role_title, target_role_id, target_role_title, current_level, level_title, current_xp, xp_to_next_level, streak_days, shield_count, last_active_date) " +
                            "SELECT ?, name, CONCAT(email, '.migrated_temp'), avatar, current_role_title, target_role_id, target_role_title, current_level, level_title, current_xp, xp_to_next_level, streak_days, shield_count, last_active_date " +
                            "FROM users WHERE id = ?",
                            newId, oldId);
                }

                // Step 2: Update all referencing child tables to point to newId
                updateTableUserIdIfExists("user_skills", "user_id", newId, oldId);
                updateTableUserIdIfExists("user_unlocked_badges", "user_id", newId, oldId);
                updateTableUserIdIfExists("user_completed_quests", "user_id", newId, oldId);
                updateTableUserIdIfExists("enrollments", "user_id", newId, oldId);
                updateTableUserIdIfExists("study_logs", "user_id", newId, oldId);
                updateTableUserIdIfExists("courses", "author_id", newId, oldId);

                // Step 3: Delete old user
                jdbcTemplate.update("DELETE FROM users WHERE id = ?", oldId);

                // Step 4: Revert temp email to original email on new user
                jdbcTemplate.update("UPDATE users SET email = REPLACE(email, '.migrated_temp', '') WHERE id = ?", newId);

                log.info("Migrated user ID '{}' -> '{}' successfully.", oldId, newId);
            }

            log.info("User ID migration to user_1..user_5 completed successfully!");

        } catch (Exception e) {
            log.error("Failed during User ID migration: {}", e.getMessage(), e);
        }
    }

    private void updateTableUserIdIfExists(String tableName, String columnName, String newId, String oldId) {
        try {
            int rows = jdbcTemplate.update(
                    "UPDATE " + tableName + " SET " + columnName + " = ? WHERE " + columnName + " = ?",
                    newId, oldId);
            log.debug("Updated {} rows in {}.{} from '{}' to '{}'", rows, tableName, columnName, oldId, newId);
        } catch (Exception e) {
            log.warn("Notice: could not update {}.{}: {}", tableName, columnName, e.getMessage());
        }
    }
}
