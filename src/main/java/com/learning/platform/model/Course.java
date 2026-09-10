package com.learning.platform.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "courses")
public class Course {

    @Id
    private String id; // e.g., "COURSE_SPRING_CLOUD", "COURSE_K8S_ARCH"

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private String category; // Backend, Cloud, Architecture, DevOps, Security

    @Column(nullable = false)
    private String difficultyLevel; // BEGINNER, INTERMEDIATE, ADVANCED

    private int estimatedHours;

    private int xpReward;

    private String icon;

    private double rating = 4.8;

    private int enrolledCount = 0;

    private String status = "PUBLISHED"; // DRAFT, PUBLISHED

    private String authorName = "Curriculum Team";
    private String authorId;

    private String track = "Data";

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_target_skills", joinColumns = @JoinColumn(name = "course_id"))
    @org.hibernate.annotations.BatchSize(size = 25)
    private List<String> targetSkillIds = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "course_prerequisites", joinColumns = @JoinColumn(name = "course_id"))
    @org.hibernate.annotations.BatchSize(size = 25)
    private List<String> prerequisiteCourseIds = new ArrayList<>();

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    @JsonManagedReference
    @org.hibernate.annotations.BatchSize(size = 25)
    private List<CourseModule> modules = new ArrayList<>();

    @OneToOne(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    private Quiz quiz;

    public Course() {
    }

    public Course(String id, String title, String description, String category, String difficultyLevel, int estimatedHours, int xpReward, String icon) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.difficultyLevel = difficultyLevel;
        this.estimatedHours = estimatedHours;
        this.xpReward = xpReward;
        this.icon = icon;
    }

    public void addModule(CourseModule module) {
        modules.add(module);
        module.setCourse(this);
    }

    public void setQuiz(Quiz quiz) {
        this.quiz = quiz;
        if (quiz != null) {
            quiz.setCourse(this);
        }
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

    public int getEstimatedHours() {
        return estimatedHours;
    }

    public void setEstimatedHours(int estimatedHours) {
        this.estimatedHours = estimatedHours;
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

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public int getEnrolledCount() {
        return enrolledCount;
    }

    public void setEnrolledCount(int enrolledCount) {
        this.enrolledCount = enrolledCount;
    }

    public List<String> getTargetSkillIds() {
        return targetSkillIds;
    }

    public void setTargetSkillIds(List<String> targetSkillIds) {
        this.targetSkillIds = targetSkillIds;
    }

    public List<String> getPrerequisiteCourseIds() {
        return prerequisiteCourseIds;
    }

    public void setPrerequisiteCourseIds(List<String> prerequisiteCourseIds) {
        this.prerequisiteCourseIds = prerequisiteCourseIds;
    }

    public List<CourseModule> getModules() {
        return modules;
    }

    public void setModules(List<CourseModule> modules) {
        this.modules = modules;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getAuthorName() {
        return authorName;
    }

    public void setAuthorName(String authorName) {
        this.authorName = authorName;
    }

    public String getAuthorId() {
        return authorId;
    }

    public void setAuthorId(String authorId) {
        this.authorId = authorId;
    }

    public String getTrack() {
        return track;
    }

    public void setTrack(String track) {
        this.track = track;
    }

    public Quiz getQuiz() {
        return quiz;
    }
}
