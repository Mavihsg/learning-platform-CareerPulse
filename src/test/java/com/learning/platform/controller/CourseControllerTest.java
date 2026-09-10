package com.learning.platform.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.platform.dto.PlanBuilderRequestDto;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class CourseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/courses should return all courses with modules")
    void testGetAllCourses() throws Exception {
        mockMvc.perform(get("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].modules").isArray());
    }

    @Test
    @DisplayName("GET /api/courses/{id} should return single course details")
    void testGetCourseById() throws Exception {
        mockMvc.perform(get("/api/courses/PLAN_ADE_01")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Applied Data Engineering"));
    }

    @Test
    @DisplayName("POST, PUT and DELETE /api/courses should manage course lifecycle")
    void testCourseLifecycle() throws Exception {
        // 1. Create
        PlanBuilderRequestDto createDto = new PlanBuilderRequestDto();
        createDto.setTitle("Site Reliability Engineering");
        createDto.setTrack("Platform");
        PlanBuilderRequestDto.ModuleDto mDto = new PlanBuilderRequestDto.ModuleDto();
        mDto.setTitle("Incident Response");
        PlanBuilderRequestDto.LessonDto lDto = new PlanBuilderRequestDto.LessonDto();
        lDto.setTitle("Post-mortem Best Practices");
        lDto.setResourceType("READING");
        lDto.setDurationMinutes(25);
        mDto.setLessons(List.of(lDto));
        createDto.setModules(List.of(mDto));

        String response = mockMvc.perform(post("/api/courses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn().getResponse().getContentAsString();

        String courseId = objectMapper.readTree(response).get("data").get("id").asText();

        // 2. Update
        createDto.setTitle("Site Reliability Engineering (Updated)");
        mockMvc.perform(put("/api/courses/" + courseId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.title").value("Site Reliability Engineering (Updated)"));

        // 3. Delete
        mockMvc.perform(delete("/api/courses/" + courseId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
