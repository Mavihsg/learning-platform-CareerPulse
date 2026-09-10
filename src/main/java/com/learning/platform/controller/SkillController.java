package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.model.Skill;
import com.learning.platform.service.SkillService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/skills")
@CrossOrigin(origins = "*")
public class SkillController {

    private final SkillService skillService;

    public SkillController(SkillService skillService) {
        this.skillService = skillService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Skill>>> getAllSkills() {
        return ResponseEntity.ok(ApiResponse.ok(skillService.getAllSkills()));
    }

    @GetMapping("/categories")
    public ResponseEntity<ApiResponse<List<String>>> getCategories() {
        return ResponseEntity.ok(ApiResponse.ok(skillService.getAllCategories()));
    }

    @GetMapping("/category/{category}")
    public ResponseEntity<ApiResponse<List<Skill>>> getSkillsByCategory(@PathVariable String category) {
        return ResponseEntity.ok(ApiResponse.ok(skillService.getSkillsByCategory(category)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Skill>> getSkillById(@PathVariable String id) {
        return skillService.getSkillById(id)
                .map(s -> ResponseEntity.ok(ApiResponse.ok(s)))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("Skill not found with id: " + id)));
    }
}
