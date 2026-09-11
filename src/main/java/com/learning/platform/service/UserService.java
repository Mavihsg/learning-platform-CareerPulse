package com.learning.platform.service;

import com.learning.platform.dto.UserProfileDto;
import com.learning.platform.model.Enrollment;
import com.learning.platform.model.RoleBenchmark;
import com.learning.platform.model.StudyLog;
import com.learning.platform.model.User;
import com.learning.platform.model.UserSkill;
import com.learning.platform.repository.EnrollmentRepository;
import com.learning.platform.repository.RoleBenchmarkRepository;
import com.learning.platform.repository.StudyLogRepository;
import com.learning.platform.repository.UserRepository;
import com.learning.platform.repository.UserSkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Service
public class UserService {

    private static final Logger log = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final RoleBenchmarkRepository roleBenchmarkRepository;
    private final UserSkillRepository userSkillRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final StudyLogRepository studyLogRepository;
    private final ResendEmailService resendEmailService;

    public UserService(UserRepository userRepository,
                       RoleBenchmarkRepository roleBenchmarkRepository,
                       UserSkillRepository userSkillRepository,
                       EnrollmentRepository enrollmentRepository,
                       StudyLogRepository studyLogRepository,
                       ResendEmailService resendEmailService) {
        this.userRepository = userRepository;
        this.roleBenchmarkRepository = roleBenchmarkRepository;
        this.userSkillRepository = userSkillRepository;
        this.enrollmentRepository = enrollmentRepository;
        this.studyLogRepository = studyLogRepository;
        this.resendEmailService = resendEmailService;
    }

    @Cacheable(value = "all_users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @Cacheable(value = "users", key = "#id")
    public Optional<User> getUserById(String id) {
        return userRepository.findById(id);
    }

    @Transactional
    @CacheEvict(value = {"users", "all_users", "dashboard", "team_analytics", "leaderboard"}, allEntries = true)
    public Optional<User> updateTargetRole(String userId, String targetRoleId) {
        Optional<User> userOpt = userRepository.findById(userId);
        Optional<RoleBenchmark> roleOpt = roleBenchmarkRepository.findById(targetRoleId);

        if (userOpt.isPresent() && roleOpt.isPresent()) {
            User user = userOpt.get();
            RoleBenchmark role = roleOpt.get();
            user.setTargetRoleId(role.getId());
            user.setTargetRoleTitle(role.getTitle());
            return Optional.of(userRepository.save(user));
        }
        return Optional.empty();
    }

    @Transactional
    public Optional<User> updateSkillProficiency(String userId, String skillId, UserSkill.ProficiencyLevel level, int score) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            Optional<UserSkill> existingSkill = userSkillRepository.findByUserIdAndSkillId(userId, skillId);
            if (existingSkill.isPresent()) {
                UserSkill skill = existingSkill.get();
                skill.setProficiencyLevel(level);
                skill.setScore(score);
                userSkillRepository.save(skill);
            } else {
                UserSkill newSkill = new UserSkill(user, skillId, skillId, level, score, true);
                user.addSkill(newSkill);
            }
            return Optional.of(userRepository.save(user));
        }
        return Optional.empty();
    }

    /**
     * Finds the current highest numeric suffix among all "user_N" IDs and returns the next sequential ID ("user_{N+1}").
     * If no "user_N" IDs exist, defaults to "user_1".
     */
    public synchronized String generateNextUserId() {
        List<User> allUsers = userRepository.findAll();
        int maxId = 0;
        for (User u : allUsers) {
            if (u.getId() != null && u.getId().toLowerCase().startsWith("user_")) {
                try {
                    String numStr = u.getId().substring(5);
                    int num = Integer.parseInt(numStr);
                    if (num > maxId) {
                        maxId = num;
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        return "user_" + (maxId + 1);
    }

    @Transactional
    @CacheEvict(value = {"users", "all_users", "dashboard", "team_analytics", "leaderboard"}, allEntries = true)
    public User registerUser(String name, String email, String currentRoleTitle, String targetRoleId) {
        String safeEmail = (email != null && !email.isBlank()) ? email.trim() : "user." + System.currentTimeMillis() + "@enterprise.io";
        String safeName = (name != null && !name.isBlank()) ? name.trim() : "New Learner";
        String id = generateNextUserId();

        String roleTitle = (currentRoleTitle != null && !currentRoleTitle.isBlank()) ? currentRoleTitle : "Junior Software Engineer";
        String targetRole = (targetRoleId != null && !targetRoleId.isBlank()) ? targetRoleId : "ROLE_CLOUD_ARCHITECT";

        User newUser = new User(id, safeName, safeEmail, roleTitle, targetRole, "Cloud Native Solutions Architect");
        newUser.setAvatar("https://api.dicebear.com/7.x/bottts/svg?seed=" + safeName.replaceAll("\\s+", ""));
        newUser.setCurrentLevel(1);
        newUser.setLevelTitle("Novice Explorer");
        newUser.setCurrentXp(100);
        newUser.setXpToNextLevel(500);
        newUser.setStreakDays(1);
        newUser.setShieldCount(1);
        newUser.setLastActiveDate(java.time.LocalDate.now().toString());

        newUser.addSkill(new UserSkill(newUser, "SKILL_JAVA_CORE", "Core Java & Concurrency", UserSkill.ProficiencyLevel.BEGINNER, 50, false));
        newUser.addSkill(new UserSkill(newUser, "SKILL_SQL_POSTGRES", "PostgreSQL & Query Optimization", UserSkill.ProficiencyLevel.BEGINNER, 50, false));

        return userRepository.save(newUser);
    }

    @Transactional
    @CacheEvict(value = {"users", "all_users", "dashboard", "team_analytics", "leaderboard"}, allEntries = true)
    public User findOrCreateGoogleUser(String name, String email, String avatar) {
        Optional<User> existing = userRepository.findByEmail(email);
        if (existing.isPresent()) {
            return existing.get();
        }

        String id = generateNextUserId();
        User newUser = new User(id, name, email, "Software Engineer", "ROLE_SR_BACKEND_ENG", "Senior Backend Engineer");
        newUser.setAvatar(avatar != null && !avatar.isBlank() ? avatar : "https://api.dicebear.com/7.x/bottts/svg?seed=" + name.replaceAll("\\s+", ""));
        newUser.setCurrentLevel(1);
        newUser.setLevelTitle("Novice Explorer");
        newUser.setCurrentXp(100);
        newUser.setXpToNextLevel(500);
        newUser.setStreakDays(1);
        newUser.setShieldCount(1);
        newUser.setLastActiveDate(java.time.LocalDate.now().toString());

        newUser.addSkill(new UserSkill(newUser, "SKILL_JAVA_CORE", "Core Java & Concurrency", UserSkill.ProficiencyLevel.BEGINNER, 50, false));

        return userRepository.save(newUser);
    }

    /**
     * Awards earned course completion XP to a specific user and promotes level if threshold reached.
     */
    @Transactional
    @CacheEvict(value = {"users", "all_users", "dashboard", "team_analytics", "leaderboard"}, allEntries = true)
    public Optional<User> awardCourseCompletionXp(String userId, int xpAmount) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Optional.empty();
        }
        User user = userOpt.get();
        int oldXp = user.getCurrentXp();
        int newXp = oldXp + xpAmount;
        user.setCurrentXp(newXp);

        // Level thresholds:
        // Level 1: 0 - 499 XP (Next Level: 500 XP, Title: "Novice Explorer")
        // Level 2: 500 - 1499 XP (Next Level: 1500 XP, Title: "Code Apprentice")
        // Level 3: 1500 - 2999 XP (Next Level: 3000 XP, Title: "Skill Specialist")
        // Level 4: 3000 - 4999 XP (Next Level: 5000 XP, Title: "Lead Architect")
        // Level 5: 5000+ XP (Max Level, Title: "Grandmaster")
        int newLevel;
        int nextThreshold;
        String levelTitle;

        if (newXp >= 5000) {
            newLevel = 5;
            nextThreshold = 5000;
            levelTitle = "Grandmaster";
        } else if (newXp >= 3000) {
            newLevel = 4;
            nextThreshold = 5000;
            levelTitle = "Lead Architect";
        } else if (newXp >= 1500) {
            newLevel = 3;
            nextThreshold = 3000;
            levelTitle = "Skill Specialist";
        } else if (newXp >= 500) {
            newLevel = 2;
            nextThreshold = 1500;
            levelTitle = "Code Apprentice";
        } else {
            newLevel = 1;
            nextThreshold = 500;
            levelTitle = "Novice Explorer";
        }

        int oldLevel = user.getCurrentLevel();
        user.setCurrentLevel(newLevel);
        user.setXpToNextLevel(nextThreshold);
        user.setLevelTitle(levelTitle);
        evaluateBadges(user);

        if (newLevel > oldLevel) {
            final int lvl = newLevel;
            final String title = levelTitle;
            CompletableFuture.runAsync(() -> {
                try {
                    resendEmailService.sendMilestoneEmail(user, "LEVEL UP", "Level " + lvl + " reached: " + title,
                            "You advanced to Level " + lvl + " (" + title + ")! Your technical mastery is shining.", xpAmount);
                } catch (Exception e) {
                    log.warn("Failed to dispatch level-up email: {}", e.getMessage());
                }
            });
        }

        return Optional.of(userRepository.save(user));
    }

    @Transactional
    public void evaluateBadges(User user) {
        if (user == null || user.getId() == null) return;
        List<String> badges = user.getUnlockedBadgeIds();
        if (badges == null) {
            badges = new ArrayList<>();
            user.setUnlockedBadgeIds(badges);
        }

        long completedCourses = enrollmentRepository.findByUserId(user.getId()).stream()
                .filter(e -> e.getStatus() == Enrollment.Status.COMPLETED || e.getProgressPercentage() >= 100)
                .count();

        boolean changed = false;
        List<String> newlyUnlockedBadges = new ArrayList<>();

        if (completedCourses >= 1 && !badges.contains("BADGE_FIRST_STEP")) {
            badges.add("BADGE_FIRST_STEP");
            newlyUnlockedBadges.add("First Step: Completed 1 Course");
            changed = true;
        }
        if (completedCourses >= 3 && !badges.contains("BADGE_COURSES_3")) {
            badges.add("BADGE_COURSES_3");
            newlyUnlockedBadges.add("Triple Threat: Completed 3 Courses");
            changed = true;
        }
        if (completedCourses >= 5 && !badges.contains("BADGE_COURSES_5")) {
            badges.add("BADGE_COURSES_5");
            newlyUnlockedBadges.add("Polyglot Architect: Completed 5 Courses");
            changed = true;
        }
        if (user.getCurrentXp() >= 1000 && !badges.contains("BADGE_XP_1000")) {
            badges.add("BADGE_XP_1000");
            newlyUnlockedBadges.add("1K Club: Earned 1,000 XP");
            changed = true;
        }
        if (user.getStreakDays() >= 7 && !badges.contains("BADGE_STREAK_7")) {
            badges.add("BADGE_STREAK_7");
            newlyUnlockedBadges.add("Habit Master: 7-Day Streak");
            changed = true;
        } else if (user.getStreakDays() >= 3 && !badges.contains("BADGE_STREAK_3")) {
            badges.add("BADGE_STREAK_3");
            newlyUnlockedBadges.add("Consistency Starter: 3-Day Streak");
            changed = true;
        }

        if (changed) {
            userRepository.save(user);
            for (String badgeName : newlyUnlockedBadges) {
                CompletableFuture.runAsync(() -> {
                    try {
                        resendEmailService.sendMilestoneEmail(user, "BADGE UNLOCKED", badgeName,
                                "Congratulations! You unlocked the milestone badge: " + badgeName + ".", 150);
                    } catch (Exception e) {
                        log.warn("Failed to dispatch badge unlock email: {}", e.getMessage());
                    }
                });
            }
        }
    }

    @Transactional
    @CacheEvict(value = {"users", "all_users", "dashboard", "team_analytics", "leaderboard"}, allEntries = true)
    public User addSkill(String userId, String skillName, String proficiency, Integer score) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        UserSkill.ProficiencyLevel level = UserSkill.ProficiencyLevel.INTERMEDIATE;
        if (proficiency != null) {
            try {
                level = UserSkill.ProficiencyLevel.valueOf(proficiency.toUpperCase().trim());
            } catch (Exception ignored) {}
        }

        String skillId = "SKILL_" + skillName.replaceAll("[^a-zA-Z0-9]", "_").toUpperCase();

        Optional<UserSkill> existing = user.getSkills().stream()
                .filter(s -> s.getSkillName().equalsIgnoreCase(skillName.trim()) || s.getSkillId().equalsIgnoreCase(skillId))
                .findFirst();

        if (existing.isPresent()) {
            UserSkill s = existing.get();
            s.setProficiencyLevel(level);
            if (score != null) s.setScore(score);
        } else {
            UserSkill newSkill = new UserSkill(user, skillId, skillName.trim(), level, score != null ? score : 75, true);
            user.addSkill(newSkill);
        }

        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = {"users", "all_users", "dashboard", "team_analytics", "leaderboard"}, allEntries = true)
    public User removeSkill(String userId, Long skillRecordId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.getSkills().removeIf(s -> s.getId() != null && s.getId().equals(skillRecordId));
        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = {"users", "all_users", "dashboard", "team_analytics", "leaderboard"}, allEntries = true)
    public User updateAvatar(String userId, String avatarUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));
        user.setAvatar(avatarUrl);
        return userRepository.save(user);
    }

    @Transactional
    @CacheEvict(value = {"users", "all_users", "dashboard", "team_analytics", "leaderboard", "weekly_activity"}, allEntries = true)
    public Map<String, Object> recordQuizCompletion(String userId, int xpReward, int scorePercentage, boolean isDaily) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Map.of("success", false, "message", "User not found");
        }
        User user = userOpt.get();

        int oldXp = user.getCurrentXp();
        int newXp = oldXp + xpReward;
        user.setCurrentXp(newXp);

        // Recalculate level thresholds
        int newLevel;
        int nextThreshold;
        String levelTitle;
        if (newXp >= 5000) {
            newLevel = 5; nextThreshold = 5000; levelTitle = "Grandmaster";
        } else if (newXp >= 3000) {
            newLevel = 4; nextThreshold = 5000; levelTitle = "Lead Architect";
        } else if (newXp >= 1500) {
            newLevel = 3; nextThreshold = 3000; levelTitle = "Skill Specialist";
        } else if (newXp >= 500) {
            newLevel = 2; nextThreshold = 1500; levelTitle = "Code Apprentice";
        } else {
            newLevel = 1; nextThreshold = 500; levelTitle = "Novice Explorer";
        }
        user.setCurrentLevel(newLevel);
        user.setXpToNextLevel(nextThreshold);
        user.setLevelTitle(levelTitle);

        boolean streakIncremented = false;
        LocalDate today = LocalDate.now();
        String todayStr = today.toString();
        String lastActive = user.getLastActiveDate();

        if (isDaily) {
            if (lastActive == null || lastActive.isBlank()) {
                user.setStreakDays(Math.max(1, user.getStreakDays() + 1));
                streakIncremented = true;
            } else {
                try {
                    LocalDate lastDate = LocalDate.parse(lastActive);
                    if (lastDate.equals(today.minusDays(1))) {
                        user.setStreakDays(user.getStreakDays() + 1);
                        streakIncremented = true;
                    } else if (lastDate.isBefore(today.minusDays(1))) {
                        if (user.getShieldCount() > 0) {
                            user.setShieldCount(user.getShieldCount() - 1);
                            user.setStreakDays(user.getStreakDays() + 1);
                        } else {
                            user.setStreakDays(1);
                        }
                        streakIncremented = true;
                    }
                } catch (Exception e) {
                    user.setStreakDays(user.getStreakDays() + 1);
                    streakIncremented = true;
                }
            }
            user.setLastActiveDate(todayStr);

            // Mark daily quiz quest completed
            List<String> quests = user.getCompletedQuestIds();
            if (quests == null) {
                quests = new ArrayList<>();
                user.setCompletedQuestIds(quests);
            }
            if (!quests.contains("QUEST_DAILY_QUIZ_1")) {
                quests.add("QUEST_DAILY_QUIZ_1");
            }
        }

        // Evaluate Quiz Ace & milestone badges
        List<String> badges = user.getUnlockedBadgeIds();
        if (badges == null) {
            badges = new ArrayList<>();
            user.setUnlockedBadgeIds(badges);
        }
        List<String> newlyUnlocked = new ArrayList<>();
        if (scorePercentage == 100 && !badges.contains("BADGE_QUIZ_ACE")) {
            badges.add("BADGE_QUIZ_ACE");
            newlyUnlocked.add("BADGE_QUIZ_ACE");
        }

        evaluateBadges(user);
        userRepository.save(user);

        // Log study activity (15 minutes for completing quiz)
        try {
            Optional<StudyLog> logOpt = studyLogRepository.findByUserIdAndLogDate(userId, today);
            if (logOpt.isPresent()) {
                StudyLog sl = logOpt.get();
                sl.setMinutesLogged(sl.getMinutesLogged() + 15);
                studyLogRepository.save(sl);
            } else {
                String dayCode = switch (today.getDayOfWeek()) {
                    case MONDAY -> "M";
                    case TUESDAY -> "T";
                    case WEDNESDAY -> "W";
                    case THURSDAY -> "TH";
                    case FRIDAY -> "F";
                    case SATURDAY -> "SA";
                    case SUNDAY -> "SU";
                };
                studyLogRepository.save(new StudyLog(userId, dayCode, 15, today));
            }
        } catch (Exception ignored) {}

        return Map.of(
                "success", true,
                "streakIncremented", streakIncremented,
                "newStreakDays", user.getStreakDays(),
                "newlyUnlockedBadges", newlyUnlocked,
                "currentXp", user.getCurrentXp(),
                "level", user.getCurrentLevel(),
                "levelTitle", user.getLevelTitle()
        );
    }

    @Transactional
    @CacheEvict(value = {"users", "all_users", "dashboard", "team_analytics", "leaderboard"}, allEntries = true)
    public Map<String, Object> recordDiscussionXp(String userId, int xpReward, String reason) {
        Optional<User> userOpt = userRepository.findById(userId);
        if (userOpt.isEmpty()) {
            return Map.of("success", false, "reason", "User not found");
        }
        User user = userOpt.get();
        int oldXp = user.getCurrentXp();
        int newXp = oldXp + xpReward;
        user.setCurrentXp(newXp);

        int newLevel;
        int nextThreshold;
        String levelTitle;
        if (newXp >= 5000) {
            newLevel = 5; nextThreshold = 5000; levelTitle = "Grandmaster";
        } else if (newXp >= 3000) {
            newLevel = 4; nextThreshold = 5000; levelTitle = "Lead Architect";
        } else if (newXp >= 1500) {
            newLevel = 3; nextThreshold = 3000; levelTitle = "Skill Specialist";
        } else if (newXp >= 500) {
            newLevel = 2; nextThreshold = 1500; levelTitle = "Code Apprentice";
        } else {
            newLevel = 1; nextThreshold = 500; levelTitle = "Novice Explorer";
        }
        user.setCurrentLevel(newLevel);
        user.setXpToNextLevel(nextThreshold);
        user.setLevelTitle(levelTitle);

        evaluateBadges(user);
        userRepository.save(user);

        return Map.of(
                "success", true,
                "currentXp", user.getCurrentXp(),
                "level", user.getCurrentLevel(),
                "levelTitle", user.getLevelTitle(),
                "xpEarned", xpReward,
                "reason", reason != null ? reason : "Discussion contribution"
        );
    }

    public UserProfileDto toDto(User user) {
        UserProfileDto dto = new UserProfileDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setEmail(user.getEmail());
        dto.setAvatar(user.getAvatar());
        dto.setCurrentRoleTitle(user.getCurrentRoleTitle());
        dto.setTargetRoleId(user.getTargetRoleId());
        dto.setTargetRoleTitle(user.getTargetRoleTitle());
        dto.setCurrentLevel(user.getCurrentLevel());
        dto.setLevelTitle(user.getLevelTitle());
        dto.setCurrentXp(user.getCurrentXp());
        dto.setXpToNextLevel(user.getXpToNextLevel());
        dto.setStreakDays(user.getStreakDays());
        dto.setShieldCount(user.getShieldCount());
        dto.setSkills(user.getSkills());
        dto.setUnlockedBadgeIds(user.getUnlockedBadgeIds());
        dto.setCompletedQuestIds(user.getCompletedQuestIds());
        return dto;
    }
}
