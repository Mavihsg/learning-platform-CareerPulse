package com.learning.platform.model;

import jakarta.persistence.*;
import java.time.LocalDate;

@Entity
@Table(name = "study_logs")
public class StudyLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String userId;

    @Column(nullable = false)
    private String dayOfWeek; // M, T, W, TH, F, SA, SU

    private int minutesLogged = 0;

    private LocalDate logDate = LocalDate.now();

    public StudyLog() {
    }

    public StudyLog(String userId, String dayOfWeek, int minutesLogged, LocalDate logDate) {
        this.userId = userId;
        this.dayOfWeek = dayOfWeek;
        this.minutesLogged = minutesLogged;
        this.logDate = logDate;
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

    public String getDayOfWeek() {
        return dayOfWeek;
    }

    public void setDayOfWeek(String dayOfWeek) {
        this.dayOfWeek = dayOfWeek;
    }

    public int getMinutesLogged() {
        return minutesLogged;
    }

    public void setMinutesLogged(int minutesLogged) {
        this.minutesLogged = minutesLogged;
    }

    public LocalDate getLogDate() {
        return logDate;
    }

    public void setLogDate(LocalDate logDate) {
        this.logDate = logDate;
    }
}
