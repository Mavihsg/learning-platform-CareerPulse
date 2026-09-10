package com.learning.platform.dto;

import java.util.List;

public class TeamAnalyticsDto {
    private int totalLearners;
    private double averageCompletionRate;
    private int totalHoursLogged;
    private int activeStreaksCount;
    private List<TeamMemberProgressDto> members;
    private List<CourseStatsDto> courseStats;

    public static class TeamMemberProgressDto {
        private String userId;
        private String name;
        private String email;
        private String avatar;
        private String roleTitle;
        private String coreTrackTitle;
        private int progressPercentage;
        private int lessonsCompleted;
        private int totalLessons;
        private int streakDays;
        private int hoursLogged;
        private String statusTag; // ON_TRACK, AT_RISK, COMPLETED, ADVANCED

        public TeamMemberProgressDto() {}

        public TeamMemberProgressDto(String userId, String name, String email, String avatar, String roleTitle, String coreTrackTitle, int progressPercentage, int lessonsCompleted, int totalLessons, int streakDays, int hoursLogged, String statusTag) {
            this.userId = userId;
            this.name = name;
            this.email = email;
            this.avatar = avatar;
            this.roleTitle = roleTitle;
            this.coreTrackTitle = coreTrackTitle;
            this.progressPercentage = progressPercentage;
            this.lessonsCompleted = lessonsCompleted;
            this.totalLessons = totalLessons;
            this.streakDays = streakDays;
            this.hoursLogged = hoursLogged;
            this.statusTag = statusTag;
        }

        public String getUserId() { return userId; }
        public void setUserId(String userId) { this.userId = userId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
        public String getRoleTitle() { return roleTitle; }
        public void setRoleTitle(String roleTitle) { this.roleTitle = roleTitle; }
        public String getCoreTrackTitle() { return coreTrackTitle; }
        public void setCoreTrackTitle(String coreTrackTitle) { this.coreTrackTitle = coreTrackTitle; }
        public int getProgressPercentage() { return progressPercentage; }
        public void setProgressPercentage(int progressPercentage) { this.progressPercentage = progressPercentage; }
        public int getLessonsCompleted() { return lessonsCompleted; }
        public void setLessonsCompleted(int lessonsCompleted) { this.lessonsCompleted = lessonsCompleted; }
        public int getTotalLessons() { return totalLessons; }
        public void setTotalLessons(int totalLessons) { this.totalLessons = totalLessons; }
        public int getStreakDays() { return streakDays; }
        public void setStreakDays(int streakDays) { this.streakDays = streakDays; }
        public int getHoursLogged() { return hoursLogged; }
        public void setHoursLogged(int hoursLogged) { this.hoursLogged = hoursLogged; }
        public String getStatusTag() { return statusTag; }
        public void setStatusTag(String statusTag) { this.statusTag = statusTag; }
    }

    public static class CourseStatsDto {
        private String courseId;
        private String title;
        private int enrolledCount;
        private int completedCount;
        private double averageProgressPercentage;

        public CourseStatsDto() {}

        public CourseStatsDto(String courseId, String title, int enrolledCount, int completedCount, double averageProgressPercentage) {
            this.courseId = courseId;
            this.title = title;
            this.enrolledCount = enrolledCount;
            this.completedCount = completedCount;
            this.averageProgressPercentage = averageProgressPercentage;
        }

        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public int getEnrolledCount() { return enrolledCount; }
        public void setEnrolledCount(int enrolledCount) { this.enrolledCount = enrolledCount; }
        public int getCompletedCount() { return completedCount; }
        public void setCompletedCount(int completedCount) { this.completedCount = completedCount; }
        public double getAverageProgressPercentage() { return averageProgressPercentage; }
        public void setAverageProgressPercentage(double averageProgressPercentage) { this.averageProgressPercentage = averageProgressPercentage; }
    }

    public TeamAnalyticsDto() {}

    public int getTotalLearners() { return totalLearners; }
    public void setTotalLearners(int totalLearners) { this.totalLearners = totalLearners; }
    public double getAverageCompletionRate() { return averageCompletionRate; }
    public void setAverageCompletionRate(double averageCompletionRate) { this.averageCompletionRate = averageCompletionRate; }
    public int getTotalHoursLogged() { return totalHoursLogged; }
    public void setTotalHoursLogged(int totalHoursLogged) { this.totalHoursLogged = totalHoursLogged; }
    public int getActiveStreaksCount() { return activeStreaksCount; }
    public void setActiveStreaksCount(int activeStreaksCount) { this.activeStreaksCount = activeStreaksCount; }
    public List<TeamMemberProgressDto> getMembers() { return members; }
    public void setMembers(List<TeamMemberProgressDto> members) { this.members = members; }
    public List<CourseStatsDto> getCourseStats() { return courseStats; }
    public void setCourseStats(List<CourseStatsDto> courseStats) { this.courseStats = courseStats; }
}
