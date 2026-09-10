package com.learning.platform.model;

import jakarta.persistence.*;

@Entity
@Table(name = "badges")
public class Badge {

    @Id
    private String id; // e.g., "BADGE_FIRST_CODE", "BADGE_STREAK_7", "BADGE_CLOUD_PIONEER"

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private String icon; // Icon name / SVG code / emoji

    @Column(nullable = false)
    private String category; // MILESTONE, STREAK, SPEED, MASTERY, COMMUNITY

    private int xpBonus;

    private String triggerType; // QUIZ_SCORE, LESSON_COUNT, STREAK_DAYS, XP_TOTAL, ROLE_READY

    private int triggerThreshold;

    public Badge() {
    }

    public Badge(String id, String title, String description, String icon, String category, int xpBonus, String triggerType, int triggerThreshold) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.icon = icon;
        this.category = category;
        this.xpBonus = xpBonus;
        this.triggerType = triggerType;
        this.triggerThreshold = triggerThreshold;
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

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public int getXpBonus() {
        return xpBonus;
    }

    public void setXpBonus(int xpBonus) {
        this.xpBonus = xpBonus;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public int getTriggerThreshold() {
        return triggerThreshold;
    }

    public void setTriggerThreshold(int triggerThreshold) {
        this.triggerThreshold = triggerThreshold;
    }
}
