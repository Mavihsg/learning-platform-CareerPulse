package com.learning.platform.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/users should return all seeded users")
    void testGetAllUsers() throws Exception {
        mockMvc.perform(get("/api/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].id").exists());
    }

    @Test
    @DisplayName("GET /api/users/{id} should return user profile details")
    void testGetUserById() throws Exception {
        mockMvc.perform(get("/api/users/user_1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.name").value("Manish Kessler"))
                .andExpect(jsonPath("$.data.currentRoleTitle").exists())
                .andExpect(jsonPath("$.data.skills").isArray());
    }

    @Test
    @DisplayName("PUT /api/users/{id}/target-role/{roleId} should update target role")
    void testUpdateTargetRole() throws Exception {
        mockMvc.perform(put("/api/users/user_1/target-role/ROLE_SR_BACKEND_ENG")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.targetRoleId").value("ROLE_SR_BACKEND_ENG"));
    }
}
