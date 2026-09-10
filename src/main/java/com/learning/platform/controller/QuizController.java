package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.dto.QuizDto;
import com.learning.platform.dto.QuizDto.QuizGenerationRequestDto;
import com.learning.platform.dto.QuizDto.QuizQuestionDto;
import com.learning.platform.dto.QuizDto.QuizResultDto;
import com.learning.platform.dto.QuizDto.QuizResultDto.QuestionReviewDto;
import com.learning.platform.dto.QuizDto.QuizSubmitRequestDto;
import com.learning.platform.service.AiQuizService;
import com.learning.platform.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/quiz")
@CrossOrigin(origins = "*")
public class QuizController {

    private final AiQuizService aiQuizService;
    private final UserService userService;

    public QuizController(AiQuizService aiQuizService, UserService userService) {
        this.aiQuizService = aiQuizService;
        this.userService = userService;
    }

    @PostMapping("/generate")
    public ResponseEntity<ApiResponse<QuizDto>> generateQuiz(@RequestBody QuizGenerationRequestDto request) {
        QuizDto quiz = aiQuizService.generateQuiz(request);
        return ResponseEntity.ok(ApiResponse.ok("Quiz generated successfully", quiz));
    }

    @GetMapping("/daily")
    public ResponseEntity<ApiResponse<QuizDto>> getDailyQuiz(@RequestParam(defaultValue = "user_1") String userId) {
        QuizDto dailyQuiz = aiQuizService.getDailyChallenge(userId);
        return ResponseEntity.ok(ApiResponse.ok("Daily quiz challenge loaded", dailyQuiz));
    }

    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<QuizResultDto>> submitQuiz(@RequestBody QuizSubmitRequestDto request) {
        if (request == null || request.getQuestions() == null || request.getQuestions().isEmpty()) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Invalid quiz submission: questions cannot be empty"));
        }

        List<QuizQuestionDto> questions = request.getQuestions();
        Map<Integer, Integer> answers = request.getAnswers() != null ? request.getAnswers() : Map.of();
        int totalQuestions = questions.size();
        int correctCount = 0;
        List<QuestionReviewDto> reviews = new ArrayList<>();

        for (int i = 0; i < totalQuestions; i++) {
            QuizQuestionDto q = questions.get(i);
            Integer userChoice = answers.get(i);
            int selectedIndex = userChoice != null ? userChoice : -1;
            boolean isCorrect = (selectedIndex == q.getCorrectOptionIndex());
            if (isCorrect) {
                correctCount++;
            }

            reviews.add(new QuestionReviewDto(
                    i,
                    q.getQuestionText(),
                    q.getOptions(),
                    selectedIndex,
                    q.getCorrectOptionIndex(),
                    isCorrect,
                    q.getExplanation()
            ));
        }

        int scorePercentage = (int) Math.round(((double) correctCount / totalQuestions) * 100.0);
        boolean passed = scorePercentage >= 70;

        // Calculate XP reward
        int xpEarned;
        if (request.isDaily()) {
            // Daily challenge gives +75 XP base if passed, +25 XP participation if attempted
            xpEarned = passed ? 75 : 25;
        } else {
            // Standard quizzes award 25 XP per correct question if passed, or 5 XP per correct as participation
            xpEarned = passed ? (correctCount * 25) : Math.max(10, correctCount * 10);
        }

        // Apply completion rewards to user (XP, level, streak, quest, badges)
        String userId = (request.getUserId() != null && !request.getUserId().isBlank()) ? request.getUserId() : "user_1";
        Map<String, Object> rewardResult = userService.recordQuizCompletion(userId, xpEarned, scorePercentage, request.isDaily());

        boolean streakIncremented = Boolean.TRUE.equals(rewardResult.get("streakIncremented"));
        int newStreakDays = rewardResult.get("newStreakDays") instanceof Number ? ((Number) rewardResult.get("newStreakDays")).intValue() : 1;

        @SuppressWarnings("unchecked")
        List<String> newBadges = (List<String>) rewardResult.get("newlyUnlockedBadges");

        QuizResultDto result = new QuizResultDto();
        result.setQuizId(request.getQuizId());
        result.setTotalQuestions(totalQuestions);
        result.setCorrectCount(correctCount);
        result.setScorePercentage(scorePercentage);
        result.setPassed(passed);
        result.setTotalXpEarned(xpEarned);
        result.setStreakIncremented(streakIncremented);
        result.setNewStreakDays(newStreakDays);
        result.setNewBadgesUnlocked(newBadges != null ? newBadges : List.of());
        result.setReviews(reviews);

        return ResponseEntity.ok(ApiResponse.ok("Quiz submitted and evaluated", result));
    }
}
