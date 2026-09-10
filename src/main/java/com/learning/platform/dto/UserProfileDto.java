package com.learning.platform.dto;

import com.learning.platform.model.UserSkill;
import java.util.List;

public class UserProfileDto {
    private String id;
    private String name;
    private String email;
    private String avatar;
    private String currentRoleTitle;
    private String targetRoleId;
    private String targetRoleTitle;
    private int currentLevel;
    private String levelTitle;
    private int currentXp;
    private int xpToNextLevel;
    private int streakDays;
    private int shieldCount;
    private List<UserSkill> skills;
    private List<String> unlockedBadgeIds;
    private List<String> completedQuestIds;

    public UserProfileDto() {
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
