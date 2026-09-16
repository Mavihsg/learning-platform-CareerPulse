package com.learning.platform.controller;

import com.learning.platform.model.Enrollment;
import com.learning.platform.repository.EnrollmentRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PublicVerificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EnrollmentRepository enrollmentRepository;

    @Test
    @DisplayName("GET /api/public/verify/{credentialId} should return 200 OK with authentic credential details")
    void testVerifyValidCredential() throws Exception {
        // Find or use any completed enrollment from DB seed
        List<Enrollment> enrollments = enrollmentRepository.findAll();
        String testCredId = "CP-CERT-2026-10001";
        for (Enrollment e : enrollments) {
            if (e.getCredentialId() != null && !e.getCredentialId().isBlank()) {
                testCredId = e.getCredentialId();
                break;
            }
        }

        mockMvc.perform(get("/api/public/verify/" + testCredId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.credentialId").value(testCredId))
                .andExpect(jsonPath("$.data.learnerName").exists())
                .andExpect(jsonPath("$.data.courseTitle").exists())
                .andExpect(jsonPath("$.data.verificationUrl").exists())
                .andExpect(jsonPath("$.data.issuer").value("CareerPulse Academic & Certification Registry"));
    }

    @Test
    @DisplayName("GET /api/public/verify/{invalidId} should return 404 NOT FOUND for nonexistent credential")
    void testVerifyInvalidCredential() throws Exception {
        mockMvc.perform(get("/api/public/verify/CP-INVALID-99999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Credential ID not found or unverified: CP-INVALID-99999999"));
    }
}
