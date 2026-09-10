package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.dto.DiscussionDto.*;
import com.learning.platform.service.DiscussionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/discussions")
@CrossOrigin(origins = "*")
public class DiscussionController {

    private final DiscussionService discussionService;

    public DiscussionController(DiscussionService discussionService) {
        this.discussionService = discussionService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ThreadSummaryDto>>> getThreads(
            @RequestParam(required = false) String courseId,
            @RequestParam(required = false, defaultValue = "ALL") String status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false, defaultValue = "NEWEST") String sort,
            @RequestParam(required = false, defaultValue = "user_1") String userId) {
        List<ThreadSummaryDto> threads = discussionService.getThreads(courseId, status, search, sort, userId);
        return ResponseEntity.ok(ApiResponse.ok("Threads retrieved", threads));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ThreadDetailDto>> getThreadDetail(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "user_1") String userId) {
        return discussionService.getThreadDetail(id, userId)
                .map(t -> ResponseEntity.ok(ApiResponse.ok("Thread details retrieved", t)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<ThreadDetailDto>> createThread(@RequestBody CreateThreadRequest request) {
        ThreadDetailDto created = discussionService.createThread(request);
        return ResponseEntity.ok(ApiResponse.ok("Discussion thread created successfully", created));
    }

    @PostMapping("/{id}/replies")
    public ResponseEntity<ApiResponse<ReplyDto>> addReply(
            @PathVariable String id,
            @RequestBody CreateReplyRequest request) {
        ReplyDto reply = discussionService.addReply(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Reply posted successfully", reply));
    }

    @PostMapping("/{id}/ai-answer")
    public ResponseEntity<ApiResponse<ReplyDto>> requestAiAnswer(@PathVariable String id) {
        ReplyDto aiReply = discussionService.requestAiAnswer(id);
        return ResponseEntity.ok(ApiResponse.ok("AI Mentor answer generated", aiReply));
    }

    @PostMapping("/{id}/upvote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleThreadUpvote(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "user_1") String userId) {
        Map<String, Object> result = discussionService.toggleThreadUpvote(id, userId);
        return ResponseEntity.ok(ApiResponse.ok("Upvote updated", result));
    }

    @PostMapping("/{id}/replies/{replyId}/upvote")
    public ResponseEntity<ApiResponse<Map<String, Object>>> toggleReplyUpvote(
            @PathVariable String id,
            @PathVariable String replyId,
            @RequestParam(required = false, defaultValue = "user_1") String userId) {
        Map<String, Object> result = discussionService.toggleReplyUpvote(id, replyId, userId);
        return ResponseEntity.ok(ApiResponse.ok("Reply upvote updated", result));
    }

    @PostMapping("/{id}/resolve")
    public ResponseEntity<ApiResponse<ThreadDetailDto>> resolveThread(
            @PathVariable String id,
            @RequestBody ResolveRequest request) {
        ThreadDetailDto resolved = discussionService.resolveThread(
                id,
                request.getAcceptedReplyId(),
                request.getResolvedByUserId()
        );
        return ResponseEntity.ok(ApiResponse.ok("Thread marked as resolved", resolved));
    }
}
