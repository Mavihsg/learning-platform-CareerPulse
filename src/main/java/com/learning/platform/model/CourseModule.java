package com.learning.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;

@Entity
@Table(name = "course_modules")
public class CourseModule {

    @Id
    private String id; // e.g., "MOD_SPRING_01"

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonBackReference
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(length = 2000)
    private String summary;

    private int durationMinutes;

    private int xpReward;

    private int orderIndex;

    @OneToMany(mappedBy = "courseModule", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("orderIndex ASC")
    @org.hibernate.annotations.BatchSize(size = 25)
    private java.util.List<Lesson> lessons = new java.util.ArrayList<>();

    public CourseModule() {
    }

    public CourseModule(String id, String title, String summary, int durationMinutes, int xpReward, int orderIndex) {
        this.id = id;
        this.title = title;
        this.summary = summary;
        this.durationMinutes = durationMinutes;
        this.xpReward = xpReward;
        this.orderIndex = orderIndex;
    }

    public void addLesson(Lesson lesson) {
        lessons.add(lesson);
        lesson.setCourseModule(this);
    }

    public java.util.List<Lesson> getLessons() {
        return lessons;
    }

    public void setLessons(java.util.List<Lesson> lessons) {
        this.lessons = lessons;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Course getCourse() {
        return course;
    }

    public void setCourse(Course course) {
        this.course = course;
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

    public int getDurationMinutes() {
        return durationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public int getOrderIndex() {
        return orderIndex;
    }

    public void setOrderIndex(int orderIndex) {
        this.orderIndex = orderIndex;
    }
}
