package com.learning.platform.dto;

import java.util.List;

public class LeaderboardDto {

    private String timeframe; // WEEKLY, ALL_TIME, STREAK
    private String resetCountdown; // e.g. "5 days, 11 hours remaining"
    private List<LeaderboardEntryDto> entries;
    private LeaderboardEntryDto currentUserEntry;
    private int currentUserRank;
    private String gapToNextRankMessage;
    private int gapToNextRankXp;
    private int gapToFirstXp;

    public static class LeaderboardEntryDto {
        private int rank;
        private String userId;
        private String name;
        private String email;
        private String avatar;
        private String currentRoleTitle;
        private int level;
        private String levelTitle;
        private int totalXp;
        private int weeklyXp;
        private int streakDays;
        private double weeklyStudyHours;
        private int completedLessonsCount;
        private int completedCoursesCount;
        private boolean isCurrentUser;

        public LeaderboardEntryDto() {}

        public int getRank() { return rank; }
        public void setRank(int rank) { this.rank = rank; }
        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getUserName() { return name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getUserEmail() { return email; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public String getCurrentRoleTitle() { return currentRoleTitle; }
        public void setCurrentRoleTitle(String currentRoleTitle) { this.currentRoleTitle = currentRoleTitle; }
        public int getLevel() { return level; }
        public void setLevel(int level) { this.level = level; }
        public String getLevelTitle() { return levelTitle; }
        public void setLevelTitle(String levelTitle) { this.levelTitle = levelTitle; }
        public int getTotalXp() { return totalXp; }
        public void setTotalXp(int totalXp) { this.totalXp = totalXp; }
        public int getWeeklyXp() { return weeklyXp; }
        public void setWeeklyXp(int weeklyXp) { this.weeklyXp = weeklyXp; }
        public int getStreakDays() { return streakDays; }
        public void setStreakDays(int streakDays) { this.streakDays = streakDays; }
        public double getWeeklyStudyHours() { return weeklyStudyHours; }
        public void setWeeklyStudyHours(double weeklyStudyHours) { this.weeklyStudyHours = weeklyStudyHours; }
        public int getCompletedLessonsCount() { return completedLessonsCount; }
        public void setCompletedLessonsCount(int completedLessonsCount) { this.completedLessonsCount = completedLessonsCount; }
        public int getCompletedCoursesCount() { return completedCoursesCount; }
        public void setCompletedCoursesCount(int completedCoursesCount) { this.completedCoursesCount = completedCoursesCount; }
        public boolean isCurrentUser() { return isCurrentUser; }
        public void setCurrentUser(boolean currentUser) { isCurrentUser = currentUser; }
    }

    public LeaderboardDto() {}

    public String getTimeframe() { return timeframe; }
    public void setTimeframe(String timeframe) { this.timeframe = timeframe; }
    public String getResetCountdown() { return resetCountdown; }
    public void setResetCountdown(String resetCountdown) { this.resetCountdown = resetCountdown; }
    public List<LeaderboardEntryDto> getEntries() { return entries; }
    public void setEntries(List<LeaderboardEntryDto> entries) { this.entries = entries; }
    public LeaderboardEntryDto getCurrentUserEntry() { return currentUserEntry; }
    public void setCurrentUserEntry(LeaderboardEntryDto currentUserEntry) { this.currentUserEntry = currentUserEntry; }
    public LeaderboardEntryDto getActiveUserEntry() { return currentUserEntry; }
    public int getCurrentUserRank() { return currentUserRank; }
    public void setCurrentUserRank(int currentUserRank) { this.currentUserRank = currentUserRank; }
    public int getActiveUserRank() { return currentUserRank; }
    public String getGapToNextRankMessage() { return gapToNextRankMessage; }
    public void setGapToNextRankMessage(String gapToNextRankMessage) { this.gapToNextRankMessage = gapToNextRankMessage; }
    public int getGapToNextRankXp() { return gapToNextRankXp; }
    public void setGapToNextRankXp(int gapToNextRankXp) { this.gapToNextRankXp = gapToNextRankXp; }
    public int getGapToFirstXp() { return gapToFirstXp; }
    public void setGapToFirstXp(int gapToFirstXp) { this.gapToFirstXp = gapToFirstXp; }
}
