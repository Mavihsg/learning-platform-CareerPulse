package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.dto.AuthRequestDto;
import com.learning.platform.dto.AuthResponseDto;
import com.learning.platform.dto.UserProfileDto;
import com.learning.platform.model.User;
import com.learning.platform.model.UserSkill;
import com.learning.platform.repository.UserRepository;
import com.learning.platform.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "*")
public class AuthController {

    private final UserRepository userRepository;
    private final UserService userService;

    public AuthController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponseDto>> login(@RequestBody AuthRequestDto request) {
        String query = request.getEmail() != null ? request.getEmail().trim() : "";
        if (query.isEmpty()) {
            return ResponseEntity.status(400).body(ApiResponse.error("Work email or User ID is required"));
        }

        Optional<User> userOpt = userRepository.findAll().stream()
                .filter(u -> u.getEmail().equalsIgnoreCase(query) 
                        || u.getId().equalsIgnoreCase(query) 
                        || u.getName().equalsIgnoreCase(query)
                        || (u.getId().equals("user_1") && query.equalsIgnoreCase("shivam.gupta@enterprise.io")))
                .findFirst();

        if (userOpt.isPresent()) {
            User user = userOpt.get();
            UserProfileDto dto = userService.toDto(user);
            String token = "TOKEN_" + UUID.randomUUID().toString().substring(0, 12);
            return ResponseEntity.ok(ApiResponse.ok("Login successful", AuthResponseDto.success(token, dto)));
        }

        // If email was provided with '@', auto-provision a new user account for this email
        // so the user gets their OWN clean personal account instead of falling back to someone else!
        if (query.contains("@")) {
            String prefix = query.split("@")[0];
            String name = java.util.Arrays.stream(prefix.split("[._-]"))
                    .filter(w -> !w.isEmpty())
                    .map(w -> Character.toUpperCase(w.charAt(0)) + (w.length() > 1 ? w.substring(1).toLowerCase() : ""))
                    .collect(java.util.stream.Collectors.joining(" "));
            if (name.isEmpty()) name = "Learner";
            User newUser = userService.registerUser(name, query, "Software Engineer", "ROLE_SR_BACKEND_ENG");
            UserProfileDto dto = userService.toDto(newUser);
            String token = "TOKEN_" + UUID.randomUUID().toString().substring(0, 12);
            return ResponseEntity.ok(ApiResponse.ok("Account provisioned and authenticated", AuthResponseDto.success(token, dto)));
        }

        return ResponseEntity.status(401).body(ApiResponse.error("User not found. Please enter a valid work email or select a demo profile."));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponseDto>> register(@RequestBody AuthRequestDto request) {
        User saved = userService.registerUser(
                request.getName(),
                request.getEmail(),
                request.getCurrentRoleTitle(),
                request.getTargetRoleId()
        );
        UserProfileDto dto = userService.toDto(saved);
        String token = "TOKEN_" + UUID.randomUUID().toString().substring(0, 12);

        return ResponseEntity.ok(ApiResponse.ok("Account created successfully", AuthResponseDto.success(token, dto)));
    }

    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponseDto>> googleAuth(@RequestBody java.util.Map<String, String> body) {
        String name = body.getOrDefault("name", "Google User");
        String email = body.getOrDefault("email", "user@gmail.com");
        String avatar = body.getOrDefault("avatar", "");

        User user = userService.findOrCreateGoogleUser(name, email, avatar);
        UserProfileDto dto = userService.toDto(user);
        String token = "TOKEN_GOOGLE_" + UUID.randomUUID().toString().substring(0, 12);

        return ResponseEntity.ok(ApiResponse.ok("Google sign-in successful", AuthResponseDto.success(token, dto)));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserProfileDto>> getCurrentUser(@RequestParam(defaultValue = "user_1") String userId) {
        return userRepository.findById(userId)
                .map(u -> ResponseEntity.ok(ApiResponse.ok(userService.toDto(u))))
                .orElseGet(() -> ResponseEntity.status(404).body(ApiResponse.error("User not found")));
    }
}
