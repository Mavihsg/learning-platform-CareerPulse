package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/config")
@CrossOrigin(origins = "*")
public class ConfigController {

    @Value("${app.features.demo-users-enabled:true}")
    private boolean demoUsersEnabled;

    @Value("${google.client.id:516054535351-f3bdp0ra9g91304bnmavf38p6jttomfk.apps.googleusercontent.com}")
    private String googleClientId;

    @Value("${spring.profiles.active:local}")
    private String activeProfile;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPublicConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put("demoUsersEnabled", demoUsersEnabled);
        config.put("googleClientId", googleClientId);
        config.put("activeProfile", activeProfile);
        config.put("environment", "prod".equalsIgnoreCase(activeProfile) ? "production" : "development");
        return ResponseEntity.ok(ApiResponse.ok(config));
    }
}
