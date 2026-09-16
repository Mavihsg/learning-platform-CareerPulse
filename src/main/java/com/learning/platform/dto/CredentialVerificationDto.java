package com.learning.platform.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CredentialVerificationDto {
    private boolean valid;
    private String credentialId;
    private String learnerName;
    private String learnerEmailMasked;
    private String learnerAvatar;
    private String courseId;
    private String courseTitle;
    private String courseDescription;
    private String track;
    private String category;
    private String difficultyLevel;
    private LocalDateTime issuedAt;
    private String issuedAtFormatted;
    private int xpAwarded;
    private int totalLessons;
    private List<String> skills;
    private String issuer;
    private String verificationUrl;

    public CredentialVerificationDto() {}

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public String getCredentialId() {
        return credentialId;
    }

    public void setCredentialId(String credentialId) {
        this.credentialId = credentialId;
    }

    public String getLearnerName() {
        return learnerName;
    }

    public void setLearnerName(String learnerName) {
        this.learnerName = learnerName;
    }

    public String getLearnerEmailMasked() {
        return learnerEmailMasked;
    }

    public void setLearnerEmailMasked(String learnerEmailMasked) {
        this.learnerEmailMasked = learnerEmailMasked;
    }

    public String getLearnerAvatar() {
        return learnerAvatar;
    }

    public void setLearnerAvatar(String learnerAvatar) {
        this.learnerAvatar = learnerAvatar;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public String getCourseTitle() {
        return courseTitle;
    }

    public void setCourseTitle(String courseTitle) {
        this.courseTitle = courseTitle;
    }

    public String getCourseDescription() {
        return courseDescription;
    }

    public void setCourseDescription(String courseDescription) {
        this.courseDescription = courseDescription;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDifficultyLevel() {
        return difficultyLevel;
    }

    public void setDifficultyLevel(String difficultyLevel) {
        this.difficultyLevel = difficultyLevel;
    }

    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }

    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }

    public String getIssuedAtFormatted() {
        return issuedAtFormatted;
    }

    public void setIssuedAtFormatted(String issuedAtFormatted) {
        this.issuedAtFormatted = issuedAtFormatted;
    }

    public int getXpAwarded() {
        return xpAwarded;
    }

    public void setXpAwarded(int xpAwarded) {
        this.xpAwarded = xpAwarded;
    }

    public int getTotalLessons() {
        return totalLessons;
    }

    public void setTotalLessons(int totalLessons) {
        this.totalLessons = totalLessons;
    }

    public List<String> getSkills() {
        return skills;
    }

    public void setSkills(List<String> skills) {
        this.skills = skills;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getVerificationUrl() {
        return verificationUrl;
    }

    public void setVerificationUrl(String verificationUrl) {
        this.verificationUrl = verificationUrl;
    }
}
