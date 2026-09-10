package com.learning.platform.dto;

import java.util.List;

public class DashboardOverviewDto {
    private String userId;
    private String userName;
    private String greeting;
    private String subtitle;

    // Real User gamification & persona fields
    private int currentLevel;
    private String levelTitle;
    private int currentXp;
    private int xpToNextLevel;
    private int streakDays;
    private int shieldCount;
    private String currentRoleTitle;
    private String targetRoleId;
    private String targetRoleTitle;

    // Core track hero metrics
    private String coreTrackId;
    private String coreTrackTitle;
    private String coreTrackSubtitle;
    private String coreTrackCategory;
    private int coreTrackProgressPercentage;
    private int coreTrackLessonsDone;
    private int coreTrackTotalLessons;
    private double coreTrackRemainingHours;
    private int timeLoggedHours;
    private int plansEnrolledCount;

    // Up next actionable lesson
    private UpNextLessonDto upNextLesson;

    // Weekly activity
    private WeeklyActivityDto weeklyActivity;

    // Other plans
    private List<OtherPlanDto> otherPlans;

    // Real unlocked badges
    private List<UserBadgeDto> recentBadges;

    // Real community / AI mentor discussion spotlight
    private SpotlightDiscussionDto spotlightDiscussion;

    // Mini Leaderboard widget
    private MiniLeaderboardDto miniLeaderboard;

    public static class UserBadgeDto {
        private String id;
        private String title;
        private String description;
        private String icon;
        private String category;
        private int xpBonus;

        public UserBadgeDto() {}

        public UserBadgeDto(String id, String title, String description, String icon, String category, int xpBonus) {
            this.id = id;
            this.title = title;
            this.description = description;
            this.icon = icon;
            this.category = category;
            this.xpBonus = xpBonus;
        }

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getIcon() { return icon; }
        public void setIcon(String icon) { this.icon = icon; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getXpBonus() { return xpBonus; }
        public void setXpBonus(int xpBonus) { this.xpBonus = xpBonus; }
    }

    public static class SpotlightDiscussionDto {
        private String threadId;
        private String courseTitle;
        private String title;
        private String snippet;
        private String category;
        private boolean hasAiAnswer;
        private int replyCount;

        public SpotlightDiscussionDto() {}

        public SpotlightDiscussionDto(String threadId, String courseTitle, String title, String snippet, String category, boolean hasAiAnswer, int replyCount) {
            this.threadId = threadId;
            this.courseTitle = courseTitle;
            this.title = title;
            this.snippet = snippet;
            this.category = category;
            this.hasAiAnswer = hasAiAnswer;
            this.replyCount = replyCount;
        }

        public String getThreadId() { return threadId; }
        public void setThreadId(String threadId) { this.threadId = threadId; }
        public String getCourseTitle() { return courseTitle; }
        public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getSnippet() { return snippet; }
        public void setSnippet(String snippet) { this.snippet = snippet; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public boolean isHasAiAnswer() { return hasAiAnswer; }
        public void setHasAiAnswer(boolean hasAiAnswer) { this.hasAiAnswer = hasAiAnswer; }
        public int getReplyCount() { return replyCount; }
        public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
    }

    public static class UpNextLessonDto {
        private String courseId;
        private String moduleId;
        private String moduleTitle;
        private String lessonId;
        private String lessonTitle;
        private String resourceType; // VIDEO, READING, EXERCISE, PROJECT
        private int durationMinutes;

        public UpNextLessonDto() {}

        public UpNextLessonDto(String courseId, String moduleId, String moduleTitle, String lessonId, String lessonTitle, String resourceType, int durationMinutes) {
            this.courseId = courseId;
            this.moduleId = moduleId;
            this.moduleTitle = moduleTitle;
            this.lessonId = lessonId;
            this.lessonTitle = lessonTitle;
            this.resourceType = resourceType;
            this.durationMinutes = durationMinutes;
        }

        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        public String getModuleId() { return moduleId; }
        public void setModuleId(String moduleId) { this.moduleId = moduleId; }
        public String getModuleTitle() { return moduleTitle; }
        public void setModuleTitle(String moduleTitle) { this.moduleTitle = moduleTitle; }
        public String getLessonId() { return lessonId; }
        public void setLessonId(String lessonId) { this.lessonId = lessonId; }
        public String getLessonTitle() { return lessonTitle; }
        public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }
        public String getResourceType() { return resourceType; }
        public void setResourceType(String resourceType) { this.resourceType = resourceType; }
        public int getDurationMinutes() { return durationMinutes; }
        public void setDurationMinutes(int durationMinutes) { this.durationMinutes = durationMinutes; }
    }

    public static class OtherPlanDto {
        private String courseId;
        private String title;
        private String category;
        private int progressPercentage;
        private int completedLessons;
        private int totalLessons;

        public OtherPlanDto() {}

        public OtherPlanDto(String courseId, String title, int progressPercentage) {
            this.courseId = courseId;
            this.title = title;
            this.progressPercentage = progressPercentage;
            this.category = "Curriculum";
            this.completedLessons = (int) Math.round((progressPercentage / 100.0) * 8);
            this.totalLessons = 8;
        }

        public OtherPlanDto(String courseId, String title, String category, int progressPercentage, int completedLessons, int totalLessons) {
            this.courseId = courseId;
            this.title = title;
            this.category = category;
            this.progressPercentage = progressPercentage;
            this.completedLessons = completedLessons;
            this.totalLessons = totalLessons;
        }

        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }
        public int getCompletedLessons() { return completedLessons; }
        public void setCompletedLessons(int completedLessons) { this.completedLessons = completedLessons; }
        public int getTotalLessons() { return totalLessons; }
        public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }
    }

    public static class MiniLeaderboardDto {
        private List<LeaderboardDto.LeaderboardEntryDto> topThree;
        private LeaderboardDto.LeaderboardEntryDto activeUserEntry;
        private int activeUserRank;
        private boolean activeUserInTopThree;

        public MiniLeaderboardDto() {}

        public MiniLeaderboardDto(List<LeaderboardDto.LeaderboardEntryDto> topThree, LeaderboardDto.LeaderboardEntryDto activeUserEntry, int activeUserRank, boolean activeUserInTopThree) {
            this.topThree = topThree;
            this.activeUserEntry = activeUserEntry;
            this.activeUserRank = activeUserRank;
            this.activeUserInTopThree = activeUserInTopThree;
        }

        public List<LeaderboardDto.LeaderboardEntryDto> getTopThree() { return topThree; }
        public void setTopThree(List<LeaderboardDto.LeaderboardEntryDto> topThree) { this.topThree = topThree; }
        public LeaderboardDto.LeaderboardEntryDto getActiveUserEntry() { return activeUserEntry; }
        public void setActiveUserEntry(LeaderboardDto.LeaderboardEntryDto activeUserEntry) { this.activeUserEntry = activeUserEntry; }
        public int getActiveUserRank() { return activeUserRank; }
        public void setActiveUserRank(int activeUserRank) { this.activeUserRank = activeUserRank; }
        public boolean isActiveUserInTopThree() { return activeUserInTopThree; }
        public void setActiveUserInTopThree(boolean activeUserInTopThree) { this.activeUserInTopThree = activeUserInTopThree; }
    }

    public DashboardOverviewDto() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserName() { return userName; }
    public void setUserName(String userName) { this.userName = userName; }
    public String getGreeting() { return greeting; }
    public void setGreeting(String greeting) { this.greeting = greeting; }
    public String getSubtitle() { return subtitle; }
    public void setSubtitle(String subtitle) { this.subtitle = subtitle; }

    public int getCurrentLevel() { return currentLevel; }
    public void setCurrentLevel(int currentLevel) { this.currentLevel = currentLevel; }
    public String getLevelTitle() { return levelTitle; }
    public void setLevelTitle(String levelTitle) { this.levelTitle = levelTitle; }
    public int getCurrentXp() { return currentXp; }
    public void setCurrentXp(int currentXp) { this.currentXp = currentXp; }
    public int getXpToNextLevel() { return xpToNextLevel; }
    public void setXpToNextLevel(int xpToNextLevel) { this.xpToNextLevel = xpToNextLevel; }
    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }
    public int getShieldCount() { return shieldCount; }
    public void setShieldCount(int shieldCount) { this.shieldCount = shieldCount; }
    public String getCurrentRoleTitle() { return currentRoleTitle; }
    public void setCurrentRoleTitle(String currentRoleTitle) { this.currentRoleTitle = currentRoleTitle; }
    public String getTargetRoleId() { return targetRoleId; }
    public void setTargetRoleId(String targetRoleId) { this.targetRoleId = targetRoleId; }
    public String getTargetRoleTitle() { return targetRoleTitle; }
    public void setTargetRoleTitle(String targetRoleTitle) { this.targetRoleTitle = targetRoleTitle; }

    public String getCoreTrackId() { return coreTrackId; }
    public void setCoreTrackId(String coreTrackId) { this.coreTrackId = coreTrackId; }
    public String getCoreTrackTitle() { return coreTrackTitle; }
    public void setCoreTrackTitle(String coreTrackTitle) { this.coreTrackTitle = coreTrackTitle; }
    public String getCoreTrackSubtitle() { return coreTrackSubtitle; }
    public void setCoreTrackSubtitle(String coreTrackSubtitle) { this.coreTrackSubtitle = coreTrackSubtitle; }
    public String getCoreTrackCategory() { return coreTrackCategory; }
    public void setCoreTrackCategory(String coreTrackCategory) { this.coreTrackCategory = coreTrackCategory; }
    public int getCoreTrackProgressPercentage() { return coreTrackProgressPercentage; }
    public void setCoreTrackProgressPercentage(int coreTrackProgressPercentage) { this.coreTrackProgressPercentage = coreTrackProgressPercentage; }
    public int getCoreTrackLessonsDone() { return coreTrackLessonsDone; }
    public void setCoreTrackLessonsDone(int coreTrackLessonsDone) { this.coreTrackLessonsDone = coreTrackLessonsDone; }
    public int getCoreTrackTotalLessons() { return coreTrackTotalLessons; }
    public void setCoreTrackTotalLessons(int coreTrackTotalLessons) { this.coreTrackTotalLessons = coreTrackTotalLessons; }
    public double getCoreTrackRemainingHours() { return coreTrackRemainingHours; }
    public void setCoreTrackRemainingHours(double coreTrackRemainingHours) { this.coreTrackRemainingHours = coreTrackRemainingHours; }
    public int getTimeLoggedHours() { return timeLoggedHours; }
    public void setTimeLoggedHours(int timeLoggedHours) { this.timeLoggedHours = timeLoggedHours; }
    public int getPlansEnrolledCount() { return plansEnrolledCount; }
    public void setPlansEnrolledCount(int plansEnrolledCount) { this.plansEnrolledCount = plansEnrolledCount; }

    public UpNextLessonDto getUpNextLesson() { return upNextLesson; }
    public void setUpNextLesson(UpNextLessonDto upNextLesson) { this.upNextLesson = upNextLesson; }
    public WeeklyActivityDto getWeeklyActivity() { return weeklyActivity; }
    public void setWeeklyActivity(WeeklyActivityDto weeklyActivity) { this.weeklyActivity = weeklyActivity; }
    public List<OtherPlanDto> getOtherPlans() { return otherPlans; }
    public void setOtherPlans(List<OtherPlanDto> otherPlans) { this.otherPlans = otherPlans; }
    public List<UserBadgeDto> getRecentBadges() { return recentBadges; }
    public void setRecentBadges(List<UserBadgeDto> recentBadges) { this.recentBadges = recentBadges; }
    public SpotlightDiscussionDto getSpotlightDiscussion() { return spotlightDiscussion; }
    public void setSpotlightDiscussion(SpotlightDiscussionDto spotlightDiscussion) { this.spotlightDiscussion = spotlightDiscussion; }
    public MiniLeaderboardDto getMiniLeaderboard() { return miniLeaderboard; }
    public void setMiniLeaderboard(MiniLeaderboardDto miniLeaderboard) { this.miniLeaderboard = miniLeaderboard; }
}
