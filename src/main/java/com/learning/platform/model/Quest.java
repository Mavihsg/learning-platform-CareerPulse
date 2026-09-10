package com.learning.platform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "quests")
public class Quest {

    @Id
    private String id; // e.g., "QUEST_DAILY_LESSON_1", "QUEST_WEEKLY_QUIZ_3"

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    @Column(nullable = false)
    private String questType; // DAILY, WEEKLY, SPECIAL

    private String targetAction; // COMPLETE_MODULE, PASS_QUIZ, EARN_XP, STREAK_LOGIN

    private int targetCount; // e.g., 1 module, 2 quizzes

    private int xpReward;

    private String icon;

    public Quest() {
    }

    public Quest(String id, String title, String description, String questType, String targetAction, int targetCount, int xpReward, String icon) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.questType = questType;
        this.targetAction = targetAction;
        this.targetCount = targetCount;
        this.xpReward = xpReward;
        this.icon = icon;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getQuestType() {
        return questType;
    }

    public void setQuestType(String questType) {
        this.questType = questType;
    }

    public String getTargetAction() {
        return targetAction;
    }

    public void setTargetAction(String targetAction) {
        this.targetAction = targetAction;
    }

    public int getTargetCount() {
        return targetCount;
    }

    public void setTargetCount(int targetCount) {
        this.targetCount = targetCount;
    }

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }
}
