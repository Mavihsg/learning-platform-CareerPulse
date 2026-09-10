package com.learning.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "quizzes")
public class Quiz {

    @Id
    private String id; // e.g., "QUIZ_SPRING_01"

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id")
    @JsonBackReference
    private Course course;

    @Column(nullable = false)
    private String title;

    @Column(length = 1000)
    private String description;

    private int passScorePercentage = 70;

    private int xpReward = 100;

    @OneToMany(mappedBy = "quiz", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JsonManagedReference
    @org.hibernate.annotations.BatchSize(size = 25)
    private List<QuizQuestion> questions = new ArrayList<>();

    public Quiz() {
    }

    public Quiz(String id, String title, String description, int passScorePercentage, int xpReward) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.passScorePercentage = passScorePercentage;
        this.xpReward = xpReward;
    }

    public void addQuestion(QuizQuestion question) {
        questions.add(question);
        question.setQuiz(this);
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public int getPassScorePercentage() {
        return passScorePercentage;
    }

    public void setPassScorePercentage(int passScorePercentage) {
        this.passScorePercentage = passScorePercentage;
    }

    public int getXpReward() {
        return xpReward;
    }

    public void setXpReward(int xpReward) {
        this.xpReward = xpReward;
    }

    public List<QuizQuestion> getQuestions() {
        return questions;
    }

    public void setQuestions(List<QuizQuestion> questions) {
        this.questions = questions;
    }
}
