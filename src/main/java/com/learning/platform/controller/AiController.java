package com.learning.platform.controller;

import com.learning.platform.dto.AiPlanResponseDto;
import com.learning.platform.dto.ApiResponse;
import com.learning.platform.service.AiPlanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
@CrossOrigin(origins = "*")
public class AiController {

    private final AiPlanService aiPlanService;

    public AiController(AiPlanService aiPlanService) {
        this.aiPlanService = aiPlanService;
    }

    @PostMapping("/generate-plan")
    public ResponseEntity<ApiResponse<AiPlanResponseDto>> generatePlan(@RequestBody Map<String, String> body) {
        String prompt = body.getOrDefault("prompt", "");
        if (prompt.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Prompt cannot be empty"));
        }

        try {
            AiPlanResponseDto plan = aiPlanService.generateCoursePlan(prompt);
            return ResponseEntity.ok(ApiResponse.ok("Course plan generated successfully", plan));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(ApiResponse.error("AI generation failed: " + e.getMessage()));
        }
    }
}
