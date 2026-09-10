package com.learning.platform.controller;

import com.learning.platform.dto.AuthRequestDto;
import com.fasterxml.jackson.databind.ObjectMapper;
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/auth/login should authenticate existing user")
    void testLoginSuccess() throws Exception {
        AuthRequestDto request = new AuthRequestDto("manish.kessler@enterprise.io", "password");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.name").value("Manish Kessler"));
    }

    @Test
    @DisplayName("POST /api/auth/register should create and return new learner profile")
    void testRegisterSuccess() throws Exception {
        AuthRequestDto request = new AuthRequestDto();
        request.setName("Jordan Lee");
        request.setEmail("jordan.lee." + System.currentTimeMillis() + "@enterprise.io");
        request.setCurrentRoleTitle("Frontend Specialist");
        request.setTargetRoleId("ROLE_SR_BACKEND_ENG");

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.user.name").value("Jordan Lee"));
    }

    @Test
    @DisplayName("GET /api/auth/me should return current user profile")
    void testGetCurrentUser() throws Exception {
        mockMvc.perform(get("/api/auth/me?userId=user_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value("user_1"));
    }
}
