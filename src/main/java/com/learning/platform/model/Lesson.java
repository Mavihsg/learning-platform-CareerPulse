package com.learning.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "lessons")
public class Lesson {

    @Id
    private String id; // e.g., "LES_ADE_01"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "module_id")
    @JsonBackReference
    private CourseModule courseModule;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private String resourceType = "READING"; // VIDEO, READING, EXERCISE, PROJECT, LINK

    private int durationMinutes = 20;

    private int orderIndex = 1;

    private int xpReward = 25;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String videoUrl; // YouTube, Vimeo, Loom, or direct video URL

    public Lesson() {
    }

    public Lesson(String id, String title, String summary, String resourceType, int durationMinutes, int orderIndex, int xpReward) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.resourceType = resourceType;
        this.durationMinutes = durationMinutes;
        this.orderIndex = orderIndex;
        this.xpReward = xpReward;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public CourseModule getCourseModule() {
        return courseModule;
    }

    public void setCourseModule(CourseModule courseModule) {
        this.courseModule = courseModule;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String resourceType) {
        this.resourceType = resourceType;
    }

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getVideoUrl() {
        return videoUrl;
    }

    public void setVideoUrl(String videoUrl) {
        this.videoUrl = videoUrl;
    }
}
