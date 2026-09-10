package com.learning.platform.dto;

import java.util.List;

public class DashboardOverviewDto {
    private String userId;
    private String userName;
    private String greeting;
    private String subtitle;

    // Core track hero metrics
    private String coreTrackId;
    private String coreTrackTitle;
    private String coreTrackSubtitle;
    private int coreTrackProgressPercentage;
    private int coreTrackLessonsDone;
    private int coreTrackTotalLessons;
    private double coreTrackRemainingHours;
    private int streakDays;
    private int timeLoggedHours;
    private int plansEnrolledCount;

    // Up next actionable lesson
    private UpNextLessonDto upNextLesson;

    // Weekly activity
    private WeeklyActivityDto weeklyActivity;

    // Other plans
    private List<OtherPlanDto> otherPlans;

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
        private int progressPercentage;

        public OtherPlanDto() {}

        public OtherPlanDto(String courseId, String title, int progressPercentage) {
            this.courseId = courseId;
            this.title = title;
            this.progressPercentage = progressPercentage;
        }

        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public int getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }
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

    public String getCoreTrackId() { return coreTrackId; }
    public void setCoreTrackId(String coreTrackId) { this.coreTrackId = coreTrackId; }
    public String getCoreTrackTitle() { return coreTrackTitle; }
    public void setCoreTrackTitle(String coreTrackTitle) { this.coreTrackTitle = coreTrackTitle; }
    public String getCoreTrackSubtitle() { return coreTrackSubtitle; }
    public void setCoreTrackSubtitle(String coreTrackSubtitle) { this.coreTrackSubtitle = coreTrackSubtitle; }
    public int getCoreTrackProgressPercentage() { return coreTrackProgressPercentage; }
    public void setCoreTrackProgressPercentage(int coreTrackProgressPercentage) { this.coreTrackProgressPercentage = coreTrackProgressPercentage; }
    public int getCoreTrackLessonsDone() { return coreTrackLessonsDone; }
    public void setCoreTrackLessonsDone(int coreTrackLessonsDone) { this.coreTrackLessonsDone = coreTrackLessonsDone; }
    public int getCoreTrackTotalLessons() { return coreTrackTotalLessons; }
    public void setCoreTrackTotalLessons(int coreTrackTotalLessons) { this.coreTrackTotalLessons = coreTrackTotalLessons; }
    public double getCoreTrackRemainingHours() { return coreTrackRemainingHours; }
    public void setCoreTrackRemainingHours(double coreTrackRemainingHours) { this.coreTrackRemainingHours = coreTrackRemainingHours; }
    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }
    public int getTimeLoggedHours() { return timeLoggedHours; }
    public void setTimeLoggedHours(int timeLoggedHours) { this.timeLoggedHours = timeLoggedHours; }
    public int getPlansEnrolledCount() { return plansEnrolledCount; }
    public void setPlansEnrolledCount(int plansEnrolledCount) { this.plansEnrolledCount = plansEnrolledCount; }

    public UpNextLessonDto getUpNextLesson() { return upNextLesson; }
    public void setUpNextLesson(UpNextLessonDto upNextLesson) { this.upNextLesson = upNextLesson; }
    public WeeklyActivityDto getWeeklyActivity() { return weeklyActivity; }
    public void setWeeklyActivity(WeeklyActivityDto weeklyActivity) { this.weeklyActivity = weeklyActivity; }
    // Mini Leaderboard widget
    private MiniLeaderboardDto miniLeaderboard;

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

    public List<OtherPlanDto> getOtherPlans() { return otherPlans; }
    public void setOtherPlans(List<OtherPlanDto> otherPlans) { this.otherPlans = otherPlans; }
    public MiniLeaderboardDto getMiniLeaderboard() { return miniLeaderboard; }
    public void setMiniLeaderboard(MiniLeaderboardDto miniLeaderboard) { this.miniLeaderboard = miniLeaderboard; }
}
