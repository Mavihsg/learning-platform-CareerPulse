package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.dto.DashboardOverviewDto;
import com.learning.platform.dto.LeaderboardDto;
import com.learning.platform.dto.TeamAnalyticsDto;
import com.learning.platform.dto.WeeklyActivityDto;
import com.learning.platform.service.AnalyticsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@CrossOrigin(origins = "*")
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping("/user/{userId}/weekly")
    public ResponseEntity<ApiResponse<WeeklyActivityDto>> getWeeklyActivity(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getWeeklyActivity(userId)));
    }

    @GetMapping("/user/{userId}/dashboard")
    public ResponseEntity<ApiResponse<DashboardOverviewDto>> getDashboardOverview(@PathVariable String userId) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getDashboardOverview(userId)));
    }

    @GetMapping("/team/overview")
    public ResponseEntity<ApiResponse<TeamAnalyticsDto>> getTeamAnalytics() {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getTeamAnalytics()));
    }

    @GetMapping("/leaderboard")
    public ResponseEntity<ApiResponse<LeaderboardDto>> getLeaderboard(
            @RequestParam(defaultValue = "WEEKLY") String timeframe,
            @RequestParam(required = false) String userId) {
        return ResponseEntity.ok(ApiResponse.ok(analyticsService.getLeaderboard(timeframe, userId)));
    }
}
