package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.dto.CredentialVerificationDto;
import com.learning.platform.service.CourseService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public")
@CrossOrigin(origins = "*")
public class PublicVerificationController {

    private final CourseService courseService;

    public PublicVerificationController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/verify/{credentialId}")
    public ResponseEntity<ApiResponse<CredentialVerificationDto>> verifyCredential(
            @PathVariable String credentialId) {
        return courseService.verifyCredential(credentialId)
                .map(dto -> ResponseEntity.ok(ApiResponse.ok("Credential verified successfully", dto)))
                .orElseGet(() -> ResponseEntity.status(404)
                        .body(ApiResponse.error("Credential ID not found or unverified: " + credentialId)));
    }
}
