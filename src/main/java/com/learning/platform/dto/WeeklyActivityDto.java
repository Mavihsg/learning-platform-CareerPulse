package com.learning.platform.dto;

import java.util.List;

public class WeeklyActivityDto {
    private List<DailyLogDto> dailyLogs;
    private int totalMinutesLogged;
    private int targetMinutes = 240;
    private boolean targetMet;
    private String targetMetDay = "Friday";
    private String targetMetDayAbbr = "F";
    private int streakDays = 12;

    public static class DailyLogDto {
        private String day; // M, T, W, TH, F, SA, SU
        private int minutes;

        public DailyLogDto() {}

        public DailyLogDto(String day, int minutes) {
            this.day = day;
            this.minutes = minutes;
        }

        public String getDay() { return day; }
        public void setDay(String day) { this.day = day; }
        public int getMinutes() { return minutes; }
        public void setMinutes(int minutes) { this.minutes = minutes; }
    }

    public WeeklyActivityDto() {}

    public WeeklyActivityDto(List<DailyLogDto> dailyLogs, int totalMinutesLogged, int targetMinutes, boolean targetMet, String targetMetDay, String targetMetDayAbbr, int streakDays) {
        this.dailyLogs = dailyLogs;
        this.totalMinutesLogged = totalMinutesLogged;
        this.targetMinutes = targetMinutes;
        this.targetMet = targetMet;
        this.targetMetDay = targetMetDay;
        this.targetMetDayAbbr = targetMetDayAbbr;
        this.streakDays = streakDays;
    }

    public List<DailyLogDto> getDailyLogs() { return dailyLogs; }
    public void setDailyLogs(List<DailyLogDto> dailyLogs) { this.dailyLogs = dailyLogs; }
    public int getTotalMinutesLogged() { return totalMinutesLogged; }
    public void setTotalMinutesLogged(int totalMinutesLogged) { this.totalMinutesLogged = totalMinutesLogged; }
    public int getTargetMinutes() { return targetMinutes; }
    public void setTargetMinutes(int targetMinutes) { this.targetMinutes = targetMinutes; }
    public boolean isTargetMet() { return targetMet; }
    public void setTargetMet(boolean targetMet) { this.targetMet = targetMet; }
    public String getTargetMetDay() { return targetMetDay; }
    public void setTargetMetDay(String targetMetDay) { this.targetMetDay = targetMetDay; }
    public String getTargetMetDayAbbr() { return targetMetDayAbbr; }
    public void setTargetMetDayAbbr(String targetMetDayAbbr) { this.targetMetDayAbbr = targetMetDayAbbr; }
    public int getStreakDays() { return streakDays; }
    public void setStreakDays(int streakDays) { this.streakDays = streakDays; }
}
