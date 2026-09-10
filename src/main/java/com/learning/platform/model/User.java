package com.learning.platform.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "users")
public class User {

    @Id
    private String id; // e.g., "USER_ALEX_CHEN"

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    private String avatar;

    private String currentRoleTitle;

    private String targetRoleId;

    private String targetRoleTitle;

    private int currentLevel = 1;

    private String levelTitle = "Novice Explorer"; // Novice Explorer, Code Apprentice, Skill Specialist, Lead Architect, Grandmaster

    private int currentXp = 0;

    private int xpToNextLevel = 500;

    private int streakDays = 0;

    private int shieldCount = 1;

    private String lastActiveDate;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @org.hibernate.annotations.BatchSize(size = 25)
    private List<UserSkill> skills = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_unlocked_badges", joinColumns = @JoinColumn(name = "user_id"))
    @org.hibernate.annotations.BatchSize(size = 25)
    private List<String> unlockedBadgeIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "user_completed_quests", joinColumns = @JoinColumn(name = "user_id"))
    @org.hibernate.annotations.BatchSize(size = 25)
    private List<String> completedQuestIds = new ArrayList<>();

    public User() {
    }

    public User(String id, String name, String email, String currentRoleTitle, String targetRoleId, String targetRoleTitle) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.currentRoleTitle = currentRoleTitle;
        this.targetRoleId = targetRoleId;
        this.targetRoleTitle = targetRoleTitle;
    }

    public void addSkill(UserSkill skill) {
        skills.add(skill);
        skill.setUser(this);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public String getCurrentRoleTitle() {
        return currentRoleTitle;
    }

    public void setCurrentRoleTitle(String currentRoleTitle) {
        this.currentRoleTitle = currentRoleTitle;
    }

    public String getTargetRoleId() {
        return targetRoleId;
    }

    public void setTargetRoleId(String targetRoleId) {
        this.targetRoleId = targetRoleId;
    }

    public String getTargetRoleTitle() {
        return targetRoleTitle;
    }

    public void setTargetRoleTitle(String targetRoleTitle) {
        this.targetRoleTitle = targetRoleTitle;
    }

    public int getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(int currentLevel) {
        this.currentLevel = currentLevel;
    }

    public String getLevelTitle() {
        return levelTitle;
    }

    public void setLevelTitle(String levelTitle) {
        this.levelTitle = levelTitle;
    }

    public int getCurrentXp() {
        return currentXp;
    }

    public void setCurrentXp(int currentXp) {
        this.currentXp = currentXp;
    }

    public int getXpToNextLevel() {
        return xpToNextLevel;
    }

    public void setXpToNextLevel(int xpToNextLevel) {
        this.xpToNextLevel = xpToNextLevel;
    }

    public int getStreakDays() {
        return streakDays;
    }

    public void setStreakDays(int streakDays) {
        this.streakDays = streakDays;
    }

    public int getShieldCount() {
        return shieldCount;
    }

    public void setShieldCount(int shieldCount) {
        this.shieldCount = shieldCount;
    }

    public String getLastActiveDate() {
        return lastActiveDate;
    }

    public void setLastActiveDate(String lastActiveDate) {
        this.lastActiveDate = lastActiveDate;
    }

    public List<UserSkill> getSkills() {
        return skills;
    }

    public void setSkills(List<UserSkill> skills) {
        this.skills = skills;
    }

    public List<String> getUnlockedBadgeIds() {
        return unlockedBadgeIds;
    }

    public void setUnlockedBadgeIds(List<String> unlockedBadgeIds) {
        this.unlockedBadgeIds = unlockedBadgeIds;
    }

    public List<String> getCompletedQuestIds() {
        return completedQuestIds;
    }

    public void setCompletedQuestIds(List<String> completedQuestIds) {
        this.completedQuestIds = completedQuestIds;
    }
}
