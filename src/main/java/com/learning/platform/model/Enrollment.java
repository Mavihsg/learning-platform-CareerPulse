package com.learning.platform.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Table(name = "enrollments")
public class Enrollment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String courseId;

    private int progressPercentage = 0; // 0 to 100

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.IN_PROGRESS;

    private LocalDateTime enrolledAt = LocalDateTime.now();

    private LocalDateTime completedAt;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "enrollment_completed_modules", joinColumns = @JoinColumn(name = "enrollment_id"))
    @org.hibernate.annotations.BatchSize(size = 25)
    private Set<String> completedModuleIds = new HashSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "enrollment_completed_lessons", joinColumns = @JoinColumn(name = "enrollment_id"))
    @org.hibernate.annotations.BatchSize(size = 25)
    private Set<String> completedLessonIds = new HashSet<>();

    private int totalLessons = 0;

    private int completedLessonsCount = 0;

    private double remainingHours = 0.0;

    private boolean isCoreTrack = false;

    private String targetDate;

    private boolean quizPassed = false;

    private int quizScore = 0;

    @Column(name = "xp_awarded")
    private Boolean xpAwarded = Boolean.FALSE;

    @Column(name = "credential_id")
    private String credentialId;

    public enum Status {
        NOT_STARTED,
        IN_PROGRESS,
        COMPLETED
    }

    public Enrollment() {
    }

    public Enrollment(String userId, String courseId) {
        this.userId = userId;
        this.courseId = courseId;
        this.status = Status.IN_PROGRESS;
        this.enrolledAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public int getProgressPercentage() {
        return progressPercentage;
    }

    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = progressPercentage;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public LocalDateTime getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(LocalDateTime enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public List<String> getCompletedModuleIds() {
        return new ArrayList<>(completedModuleIds);
    }

    public void setCompletedModuleIds(List<String> completedModuleIds) {
        this.completedModuleIds = completedModuleIds != null ? new HashSet<>(completedModuleIds) : new HashSet<>();
    }

    public Set<String> getLessonSet() {
        if (completedLessonIds == null) {
            completedLessonIds = new HashSet<>();
        }
        return completedLessonIds;
    }

    public boolean toggleLessonId(String lessonId) {
        if (completedLessonIds == null) {
            completedLessonIds = new HashSet<>();
        }
        if (completedLessonIds.contains(lessonId)) {
            completedLessonIds.remove(lessonId);
            return false;
        } else {
            completedLessonIds.add(lessonId);
            return true;
        }
    }

    public List<String> getCompletedLessonIds() {
        return new ArrayList<>(completedLessonIds != null ? completedLessonIds : Set.of());
    }

    public void setCompletedLessonIds(List<String> completedLessonIds) {
        if (this.completedLessonIds == null) {
            this.completedLessonIds = new HashSet<>();
        }
        this.completedLessonIds.clear();
        if (completedLessonIds != null) {
            this.completedLessonIds.addAll(completedLessonIds);
        }
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public int getCompletedLessonsCount() {
        return completedLessonsCount;
    }

    public void setCompletedLessonsCount(int completedLessonsCount) {
        this.completedLessonsCount = completedLessonsCount;
    }

    public double getRemainingHours() {
        return remainingHours;
    }

    public void setRemainingHours(double remainingHours) {
        this.remainingHours = remainingHours;
    }

    public boolean isCoreTrack() {
        return isCoreTrack;
    }

    public boolean getIsCoreTrack() {
        return isCoreTrack;
    }

    public void setCoreTrack(boolean coreTrack) {
        isCoreTrack = coreTrack;
    }

    public void setIsCoreTrack(boolean isCoreTrack) {
        this.isCoreTrack = isCoreTrack;
    }

    public String getTargetDate() {
        return targetDate;
    }

    public void setTargetDate(String targetDate) {
        this.targetDate = targetDate;
    }

    public boolean isQuizPassed() {
        return quizPassed;
    }

    public void setQuizPassed(boolean quizPassed) {
        this.quizPassed = quizPassed;
    }

    public int getQuizScore() {
        return quizScore;
    }

    public void setQuizScore(int quizScore) {
        this.quizScore = quizScore;
    }

    public boolean isXpAwarded() {
        return Boolean.TRUE.equals(xpAwarded);
    }

    public void setXpAwarded(boolean xpAwarded) {
        this.xpAwarded = xpAwarded;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }
}
