package com.learning.platform.service;

import com.learning.platform.dto.DiscussionDto.*;
import com.learning.platform.model.Course;
import com.learning.platform.model.DiscussionReply;
import com.learning.platform.model.DiscussionThread;
import com.learning.platform.model.User;
import com.learning.platform.repository.DiscussionReplyRepository;
import com.learning.platform.repository.DiscussionThreadRepository;
import com.learning.platform.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DiscussionService {

    private static final Logger log = LoggerFactory.getLogger(DiscussionService.class);

    private final DiscussionThreadRepository threadRepository;
    private final DiscussionReplyRepository replyRepository;
    private final UserRepository userRepository;
    private final CourseService courseService;
    private final UserService userService;
    private final AiDiscussionService aiDiscussionService;

    public DiscussionService(DiscussionThreadRepository threadRepository,
                             DiscussionReplyRepository replyRepository,
                             UserRepository userRepository,
                             CourseService courseService,
                             UserService userService,
                             AiDiscussionService aiDiscussionService) {
        this.threadRepository = threadRepository;
        this.replyRepository = replyRepository;
        this.userRepository = userRepository;
        this.courseService = courseService;
        this.userService = userService;
        this.aiDiscussionService = aiDiscussionService;
    }

    @Transactional(readOnly = true)
    public List<ThreadSummaryDto> getThreads(String courseId, String status, String search, String sortField, String currentUserId) {
        Sort sort = switch (sortField != null ? sortField.toUpperCase() : "NEWEST") {
            case "MOST_UPVOTED" -> Sort.by(Sort.Direction.DESC, "upvotes").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            case "MOST_REPLIES" -> Sort.by(Sort.Direction.DESC, "replyCount").and(Sort.by(Sort.Direction.DESC, "createdAt"));
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };

        List<DiscussionThread> threads;
        if ((courseId != null && !courseId.isBlank()) || (search != null && !search.isBlank())) {
            threads = threadRepository.searchThreads(
                    courseId != null && !courseId.isBlank() ? courseId : null,
                    search != null && !search.isBlank() ? search : null,
                    sort
            );
        } else {
            threads = threadRepository.findAll(sort);
        }

        // Apply status filter in memory
        String filterStatus = status != null ? status.toUpperCase() : "ALL";
        return threads.stream()
                .filter(t -> {
                    return switch (filterStatus) {
                        case "UNRESOLVED" -> !t.isResolved();
                        case "RESOLVED" -> t.isResolved();
                        case "AI_ANSWERED" -> t.isHasAiAnswer();
                        default -> true;
                    };
                })
                .map(t -> toSummaryDto(t, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Optional<ThreadDetailDto> getThreadDetail(String threadId, String currentUserId) {
        return threadRepository.findById(threadId).map(t -> toDetailDto(t, currentUserId));
    }

    @Transactional
    public ThreadDetailDto createThread(CreateThreadRequest request) {
        String threadId = "THREAD_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String authorId = (request.getAuthorId() != null && !request.getAuthorId().isBlank()) ? request.getAuthorId() : "user_1";

        User author = userRepository.findById(authorId).orElse(null);
        String authorName = author != null ? author.getName() : "Shivam Gupta";
        String authorAvatar = author != null ? author.getAvatar() : "⚡";
        String authorRole = author != null && author.getCurrentRoleTitle() != null ? author.getCurrentRoleTitle() : "Learner";

        String courseTitle = "General Discussion";
        if (request.getCourseId() != null && !request.getCourseId().isBlank()) {
            try {
                Course course = courseService.getCachedCourse(request.getCourseId());
                if (course != null && course.getTitle() != null) {
                    courseTitle = course.getTitle();
                }
            } catch (Exception e) {
                log.warn("Could not find course title for courseId {}: {}", request.getCourseId(), e.getMessage());
            }
        }

        DiscussionThread thread = new DiscussionThread(
                threadId,
                request.getCourseId() != null ? request.getCourseId() : "GENERAL",
                courseTitle,
                authorId,
                authorName,
                authorAvatar,
                authorRole,
                request.getTitle(),
                request.getContent()
        );

        if (request.getLessonId() != null && !request.getLessonId().isBlank()) {
            thread.setLessonId(request.getLessonId());
            thread.setLessonTitle(request.getLessonTitle());
        }

        if (request.getTags() != null && !request.getTags().isEmpty()) {
            thread.setTags(new ArrayList<>(request.getTags()));
        }

        thread = threadRepository.save(thread);

        // Award XP for posting question (+15 XP)
        try {
            userService.recordDiscussionXp(authorId, 15, "Posted discussion question: " + request.getTitle());
        } catch (Exception e) {
            log.warn("Failed to award discussion question XP: {}", e.getMessage());
        }

        // If requested instant AI answer, trigger AI Mentor
        if (request.isRequestAiAnswer()) {
            try {
                generateAndSaveAiReply(thread);
                thread = threadRepository.save(thread);
            } catch (Exception e) {
                log.error("Failed to generate initial AI answer for thread {}: {}", threadId, e.getMessage());
            }
        }

        return toDetailDto(thread, authorId);
    }

    @Transactional
    public ReplyDto addReply(String threadId, CreateReplyRequest request) {
        DiscussionThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Discussion thread not found: " + threadId));

        String replyId = "REPLY_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        String authorId = (request.getAuthorId() != null && !request.getAuthorId().isBlank()) ? request.getAuthorId() : "user_1";

        User author = userRepository.findById(authorId).orElse(null);
        String authorName = author != null ? author.getName() : "Shivam Gupta";
        String authorAvatar = author != null ? author.getAvatar() : "💬";
        String authorRole = author != null && author.getCurrentRoleTitle() != null ? author.getCurrentRoleTitle() : "Community Contributor";

        DiscussionReply reply = new DiscussionReply(
                replyId,
                thread,
                authorId,
                authorName,
                authorAvatar,
                authorRole,
                request.getContent(),
                request.isAiGenerated()
        );

        replyRepository.save(reply);
        thread.addReply(reply);
        threadRepository.save(thread);

        // Award XP (+25 XP) for contributing answer if not AI
        if (!request.isAiGenerated()) {
            try {
                userService.recordDiscussionXp(authorId, 25, "Replied to discussion thread: " + thread.getTitle());
            } catch (Exception e) {
                log.warn("Failed to award discussion reply XP: {}", e.getMessage());
            }
        }

        return toReplyDto(reply, authorId);
    }

    @Transactional
    public ReplyDto requestAiAnswer(String threadId) {
        DiscussionThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Discussion thread not found: " + threadId));

        DiscussionReply aiReply = generateAndSaveAiReply(thread);
        threadRepository.save(thread);
        return toReplyDto(aiReply, thread.getAuthorId());
    }

    private DiscussionReply generateAndSaveAiReply(DiscussionThread thread) {
        String answerMarkdown = aiDiscussionService.generateAiAnswer(thread);

        // Check if thread already has an AI-generated reply
        Optional<DiscussionReply> existingAiReply = thread.getReplies().stream()
                .filter(DiscussionReply::isAiGenerated)
                .findFirst();

        DiscussionReply reply;
        if (existingAiReply.isPresent()) {
            reply = existingAiReply.get();
            reply.setContent(answerMarkdown);
            reply.setCreatedAt(LocalDateTime.now());
        } else {
            String replyId = "REPLY_AI_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
            reply = new DiscussionReply(
                    replyId,
                    thread,
                    "ai_mentor",
                    "CareerPulse AI Mentor",
                    "🤖",
                    "Staff Technical Mentor",
                    answerMarkdown,
                    true
            );
            thread.addReply(reply);
        }

        replyRepository.save(reply);
        thread.setHasAiAnswer(true);
        return reply;
    }

    @Transactional
    public Map<String, Object> toggleThreadUpvote(String threadId, String userId) {
        DiscussionThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

        String uid = (userId != null && !userId.isBlank()) ? userId : "user_1";
        List<String> upvoters = thread.getUpvotedUserIds();
        boolean nowUpvoted;
        if (upvoters.contains(uid)) {
            upvoters.remove(uid);
            thread.setUpvotes(Math.max(0, thread.getUpvotes() - 1));
            nowUpvoted = false;
        } else {
            upvoters.add(uid);
            thread.setUpvotes(thread.getUpvotes() + 1);
            nowUpvoted = true;
        }

        threadRepository.save(thread);
        return Map.of(
                "threadId", threadId,
                "upvotes", thread.getUpvotes(),
                "isUpvoted", nowUpvoted
        );
    }

    @Transactional
    public Map<String, Object> toggleReplyUpvote(String threadId, String replyId, String userId) {
        DiscussionReply reply = replyRepository.findById(replyId)
                .orElseThrow(() -> new IllegalArgumentException("Reply not found: " + replyId));

        String uid = (userId != null && !userId.isBlank()) ? userId : "user_1";
        List<String> upvoters = reply.getUpvotedUserIds();
        boolean nowUpvoted;
        if (upvoters.contains(uid)) {
            upvoters.remove(uid);
            reply.setUpvotes(Math.max(0, reply.getUpvotes() - 1));
            nowUpvoted = false;
        } else {
            upvoters.add(uid);
            reply.setUpvotes(reply.getUpvotes() + 1);
            nowUpvoted = true;
        }

        replyRepository.save(reply);
        return Map.of(
                "replyId", replyId,
                "upvotes", reply.getUpvotes(),
                "isUpvoted", nowUpvoted
        );
    }

    @Transactional
    public ThreadDetailDto resolveThread(String threadId, String replyId, String userId) {
        DiscussionThread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new IllegalArgumentException("Thread not found: " + threadId));

        thread.setResolved(true);
        thread.setAcceptedReplyId(replyId);

        if (replyId != null && !replyId.isBlank()) {
            replyRepository.findById(replyId).ifPresent(reply -> {
                reply.setAcceptedSolution(true);
                replyRepository.save(reply);

                // Award author of accepted solution +50 XP
                if (!reply.isAiGenerated() && reply.getAuthorId() != null) {
                    try {
                        userService.recordDiscussionXp(reply.getAuthorId(), 50, "Accepted solution for thread: " + thread.getTitle());
                    } catch (Exception e) {
                        log.warn("Failed to award accepted solution XP: {}", e.getMessage());
                    }
                }
            });
        }

        thread.setUpdatedAt(LocalDateTime.now());
        threadRepository.save(thread);
        return toDetailDto(thread, userId);
    }

    private ThreadSummaryDto toSummaryDto(DiscussionThread thread, String currentUserId) {
        ThreadSummaryDto dto = new ThreadSummaryDto();
        dto.setId(thread.getId());
        dto.setCourseId(thread.getCourseId());
        dto.setCourseTitle(thread.getCourseTitle());
        dto.setLessonId(thread.getLessonId());
        dto.setLessonTitle(thread.getLessonTitle());
        dto.setAuthorId(thread.getAuthorId());
        dto.setAuthorName(thread.getAuthorName());
        dto.setAuthorAvatar(thread.getAuthorAvatar());
        dto.setAuthorRole(thread.getAuthorRole());
        dto.setTitle(thread.getTitle());

        String content = thread.getContent() != null ? thread.getContent() : "";
        dto.setContentSnippet(content.length() > 140 ? content.substring(0, 137) + "..." : content);

        dto.setTags(thread.getTags() != null ? new ArrayList<>(thread.getTags()) : new ArrayList<>());
        dto.setUpvotes(thread.getUpvotes());
        dto.setReplyCount(thread.getReplyCount());
        dto.setResolved(thread.isResolved());
        dto.setHasAiAnswer(thread.isHasAiAnswer());
        dto.setCreatedAt(thread.getCreatedAt());
        dto.setUpdatedAt(thread.getUpdatedAt());

        String uid = (currentUserId != null && !currentUserId.isBlank()) ? currentUserId : "user_1";
        dto.setUpvotedByCurrentUser(thread.getUpvotedUserIds() != null && thread.getUpvotedUserIds().contains(uid));

        return dto;
    }

    private ThreadDetailDto toDetailDto(DiscussionThread thread, String currentUserId) {
        ThreadDetailDto dto = new ThreadDetailDto();
        dto.setId(thread.getId());
        dto.setCourseId(thread.getCourseId());
        dto.setCourseTitle(thread.getCourseTitle());
        dto.setLessonId(thread.getLessonId());
        dto.setLessonTitle(thread.getLessonTitle());
        dto.setAuthorId(thread.getAuthorId());
        dto.setAuthorName(thread.getAuthorName());
        dto.setAuthorAvatar(thread.getAuthorAvatar());
        dto.setAuthorRole(thread.getAuthorRole());
        dto.setTitle(thread.getTitle());
        dto.setContent(thread.getContent());
        dto.setTags(thread.getTags() != null ? new ArrayList<>(thread.getTags()) : new ArrayList<>());
        dto.setUpvotes(thread.getUpvotes());
        dto.setReplyCount(thread.getReplyCount());
        dto.setResolved(thread.isResolved());
        dto.setAcceptedReplyId(thread.getAcceptedReplyId());
        dto.setHasAiAnswer(thread.isHasAiAnswer());
        dto.setCreatedAt(thread.getCreatedAt());
        dto.setUpdatedAt(thread.getUpdatedAt());

        String uid = (currentUserId != null && !currentUserId.isBlank()) ? currentUserId : "user_1";
        dto.setUpvotedByCurrentUser(thread.getUpvotedUserIds() != null && thread.getUpvotedUserIds().contains(uid));

        List<ReplyDto> replies = new ArrayList<>();
        if (thread.getReplies() != null) {
            for (DiscussionReply r : thread.getReplies()) {
                replies.add(toReplyDto(r, uid));
            }
        }
        dto.setReplies(replies);

        return dto;
    }

    private ReplyDto toReplyDto(DiscussionReply reply, String currentUserId) {
        ReplyDto dto = new ReplyDto();
        dto.setId(reply.getId());
        dto.setThreadId(reply.getThread() != null ? reply.getThread().getId() : null);
        dto.setAuthorId(reply.getAuthorId());
        dto.setAuthorName(reply.getAuthorName());
        dto.setAuthorAvatar(reply.getAuthorAvatar());
        dto.setAuthorRole(reply.getAuthorRole());
        dto.setContent(reply.getContent());
        dto.setAiGenerated(reply.isAiGenerated());
        dto.setAcceptedSolution(reply.isAcceptedSolution());
        dto.setUpvotes(reply.getUpvotes());
        dto.setCreatedAt(reply.getCreatedAt());

        String uid = (currentUserId != null && !currentUserId.isBlank()) ? currentUserId : "user_1";
        dto.setUpvotedByCurrentUser(reply.getUpvotedUserIds() != null && reply.getUpvotedUserIds().contains(uid));

        return dto;
    }
}
