package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.dto.UserProfileDto;
import com.learning.platform.model.Badge;
import com.learning.platform.model.User;
import com.learning.platform.repository.BadgeRepository;
import com.learning.platform.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@CrossOrigin(origins = "*")
public class UserController {

    private final UserService userService;
    private final BadgeRepository badgeRepository;

    public UserController(UserService userService, BadgeRepository badgeRepository) {
        this.userService = userService;
        this.badgeRepository = badgeRepository;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<UserProfileDto>>> getAllUsers() {
        List<UserProfileDto> users = userService.getAllUsers().stream()
                .map(userService::toDto)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(users));
    }

    @GetMapping("/badges")
    public ResponseEntity<ApiResponse<List<Badge>>> getAllBadges() {
        return ResponseEntity.ok(ApiResponse.ok(badgeRepository.findAll()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserProfileDto>> getUserById(@PathVariable String id) {
        return userService.getUserById(id)
                .map(u -> ResponseEntity.ok(ApiResponse.ok(userService.toDto(u))))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("User not found with id: " + id)));
    }

    @PutMapping("/{id}/target-role/{roleId}")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateTargetRole(@PathVariable String id, @PathVariable String roleId) {
        return userService.updateTargetRole(id, roleId)
                .map(u -> ResponseEntity.ok(ApiResponse.ok("Target role updated successfully", userService.toDto(u))))
                .orElseGet(() -> ResponseEntity.badRequest().body(ApiResponse.error("Invalid user ID or target role ID")));
    }

    @PostMapping("/{id}/skills")
    public ResponseEntity<ApiResponse<UserProfileDto>> addSkill(
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("skillName");
        String proficiency = (String) body.get("proficiencyLevel");
        Integer score = body.get("score") != null ? Integer.parseInt(body.get("score").toString()) : 75;
        if (name == null || name.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("skillName is required"));
        }
        User updated = userService.addSkill(id, name, proficiency, score);
        return ResponseEntity.ok(ApiResponse.ok("Skill added successfully", userService.toDto(updated)));
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    public ResponseEntity<ApiResponse<UserProfileDto>> removeSkill(
            @PathVariable String id,
            @PathVariable Long skillId) {
        User updated = userService.removeSkill(id, skillId);
        return ResponseEntity.ok(ApiResponse.ok("Skill removed successfully", userService.toDto(updated)));
    }

    @PutMapping("/{id}/avatar")
    public ResponseEntity<ApiResponse<UserProfileDto>> updateAvatar(
            @PathVariable String id,
            @RequestBody Map<String, String> body) {
        String avatar = body.get("avatar");
        if (avatar == null || avatar.isBlank()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("avatar is required"));
        }
        User updated = userService.updateAvatar(id, avatar);
        return ResponseEntity.ok(ApiResponse.ok("Avatar updated successfully", userService.toDto(updated)));
    }
}
