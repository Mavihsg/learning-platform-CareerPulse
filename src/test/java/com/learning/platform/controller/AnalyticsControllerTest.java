package com.learning.platform.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/analytics/user/{id}/weekly should return 7-day study activity chart data")
    void testGetWeeklyActivity() throws Exception {
        mockMvc.perform(get("/api/analytics/user/user_1/weekly")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.dailyLogs").isArray())
                .andExpect(jsonPath("$.data.totalMinutesLogged").value(250))
                .andExpect(jsonPath("$.data.targetMet").value(true));
    }

    @Test
    @DisplayName("GET /api/analytics/user/{id}/dashboard should return core track, Up Next, and other plans")
    void testGetDashboardOverview() throws Exception {
        mockMvc.perform(get("/api/analytics/user/user_1/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.coreTrackTitle").value("Applied Data Engineering"))
                .andExpect(jsonPath("$.data.coreTrackProgressPercentage").value(38))
                .andExpect(jsonPath("$.data.upNextLesson.lessonTitle").value("Build a daily ingest"))
                .andExpect(jsonPath("$.data.otherPlans").isArray());
    }

    @Test
    @DisplayName("GET /api/analytics/team/overview should return team-wide learner progress")
    void testGetTeamAnalytics() throws Exception {
        mockMvc.perform(get("/api/analytics/team/overview")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalLearners").value(5))
                .andExpect(jsonPath("$.data.members").isArray())
                .andExpect(jsonPath("$.data.courseStats").isArray());
    }

    @Test
    @DisplayName("POST /api/courses/{courseId}/lessons/{lessonId}/toggle should toggle lesson completion")
    void testToggleLesson() throws Exception {
        mockMvc.perform(post("/api/courses/PLAN_ADE_01/lessons/LES_ADE_06/toggle?userId=user_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.completedLessonIds").isArray());
    }
}
