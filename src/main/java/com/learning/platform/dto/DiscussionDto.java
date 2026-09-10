package com.learning.platform.dto;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DiscussionDto {

    public static class CreateThreadRequest {
        private String courseId;
        private String lessonId;
        private String lessonTitle;
        private String title;
        private String content;
        private List<String> tags = new ArrayList<>();
        private String authorId;
        private boolean requestAiAnswer = false;

        public CreateThreadRequest() {}

        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        public String getLessonId() { return lessonId; }
        public void setLessonId(String lessonId) { this.lessonId = lessonId; }
        public String getLessonTitle() { return lessonTitle; }
        public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public boolean isRequestAiAnswer() { return requestAiAnswer; }
        public void setRequestAiAnswer(boolean requestAiAnswer) { this.requestAiAnswer = requestAiAnswer; }
    }

    public static class CreateReplyRequest {
        private String content;
        private String authorId;
        private boolean isAiGenerated = false;

        public CreateReplyRequest() {}

        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public boolean isAiGenerated() { return isAiGenerated; }
        public void setAiGenerated(boolean aiGenerated) { isAiGenerated = aiGenerated; }
    }

    public static class ResolveRequest {
        private String acceptedReplyId;
        private String resolvedByUserId;

        public ResolveRequest() {}

        public String getAcceptedReplyId() { return acceptedReplyId; }
        public void setAcceptedReplyId(String acceptedReplyId) { this.acceptedReplyId = acceptedReplyId; }
        public String getResolvedByUserId() { return resolvedByUserId; }
        public void setResolvedByUserId(String resolvedByUserId) { this.resolvedByUserId = resolvedByUserId; }
    }

    public static class ThreadSummaryDto {
        private String id;
        private String courseId;
        private String courseTitle;
        private String lessonId;
        private String lessonTitle;
        private String authorId;
        private String authorName;
        private String authorAvatar;
        private String authorRole;
        private String title;
        private String contentSnippet;
        private List<String> tags = new ArrayList<>();
        private int upvotes;
        private int replyCount;
        private boolean isResolved;
        private boolean hasAiAnswer;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean upvotedByCurrentUser;

        public ThreadSummaryDto() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        public String getCourseTitle() { return courseTitle; }
        public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
        public String getLessonId() { return lessonId; }
        public void setLessonId(String lessonId) { this.lessonId = lessonId; }
        public String getLessonTitle() { return lessonTitle; }
        public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getAuthorAvatar() { return authorAvatar; }
        public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
        public String getAuthorRole() { return authorRole; }
        public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContentSnippet() { return contentSnippet; }
        public void setContentSnippet(String contentSnippet) { this.contentSnippet = contentSnippet; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public int getUpvotes() { return upvotes; }
        public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
        public int getReplyCount() { return replyCount; }
        public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
        public boolean isResolved() { return isResolved; }
        public void setResolved(boolean resolved) { isResolved = resolved; }
        public boolean isHasAiAnswer() { return hasAiAnswer; }
        public void setHasAiAnswer(boolean hasAiAnswer) { this.hasAiAnswer = hasAiAnswer; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public boolean isUpvotedByCurrentUser() { return upvotedByCurrentUser; }
        public void setUpvotedByCurrentUser(boolean upvotedByCurrentUser) { this.upvotedByCurrentUser = upvotedByCurrentUser; }
    }

    public static class ThreadDetailDto {
        private String id;
        private String courseId;
        private String courseTitle;
        private String lessonId;
        private String lessonTitle;
        private String authorId;
        private String authorName;
        private String authorAvatar;
        private String authorRole;
        private String title;
        private String content;
        private List<String> tags = new ArrayList<>();
        private int upvotes;
        private int replyCount;
        private boolean isResolved;
        private String acceptedReplyId;
        private boolean hasAiAnswer;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private boolean upvotedByCurrentUser;
        private List<ReplyDto> replies = new ArrayList<>();

        public ThreadDetailDto() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getCourseId() { return courseId; }
        public void setCourseId(String courseId) { this.courseId = courseId; }
        public String getCourseTitle() { return courseTitle; }
        public void setCourseTitle(String courseTitle) { this.courseTitle = courseTitle; }
        public String getLessonId() { return lessonId; }
        public void setLessonId(String lessonId) { this.lessonId = lessonId; }
        public String getLessonTitle() { return lessonTitle; }
        public void setLessonTitle(String lessonTitle) { this.lessonTitle = lessonTitle; }
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getAuthorAvatar() { return authorAvatar; }
        public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
        public String getAuthorRole() { return authorRole; }
        public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public List<String> getTags() { return tags; }
        public void setTags(List<String> tags) { this.tags = tags; }
        public int getUpvotes() { return upvotes; }
        public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
        public int getReplyCount() { return replyCount; }
        public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
        public boolean isResolved() { return isResolved; }
        public void setResolved(boolean resolved) { isResolved = resolved; }
        public String getAcceptedReplyId() { return acceptedReplyId; }
        public void setAcceptedReplyId(String acceptedReplyId) { this.acceptedReplyId = acceptedReplyId; }
        public boolean isHasAiAnswer() { return hasAiAnswer; }
        public void setHasAiAnswer(boolean hasAiAnswer) { this.hasAiAnswer = hasAiAnswer; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
        public boolean isUpvotedByCurrentUser() { return upvotedByCurrentUser; }
        public void setUpvotedByCurrentUser(boolean upvotedByCurrentUser) { this.upvotedByCurrentUser = upvotedByCurrentUser; }
        public List<ReplyDto> getReplies() { return replies; }
        public void setReplies(List<ReplyDto> replies) { this.replies = replies; }
    }

    public static class ReplyDto {
        private String id;
        private String threadId;
        private String authorId;
        private String authorName;
        private String authorAvatar;
        private String authorRole;
        private String content;
        private boolean isAiGenerated;
        private boolean isAcceptedSolution;
        private int upvotes;
        private LocalDateTime createdAt;
        private boolean upvotedByCurrentUser;

        public ReplyDto() {}

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public String getThreadId() { return threadId; }
        public void setThreadId(String threadId) { this.threadId = threadId; }
        public String getAuthorId() { return authorId; }
        public void setAuthorId(String authorId) { this.authorId = authorId; }
        public String getAuthorName() { return authorName; }
        public void setAuthorName(String authorName) { this.authorName = authorName; }
        public String getAuthorAvatar() { return authorAvatar; }
        public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
        public String getAuthorRole() { return authorRole; }
        public void setAuthorRole(String authorRole) { this.authorRole = authorRole; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public boolean isAiGenerated() { return isAiGenerated; }
        public void setAiGenerated(boolean aiGenerated) { isAiGenerated = aiGenerated; }
        public boolean isAcceptedSolution() { return isAcceptedSolution; }
        public void setAcceptedSolution(boolean acceptedSolution) { isAcceptedSolution = acceptedSolution; }
        public int getUpvotes() { return upvotes; }
        public void setUpvotes(int upvotes) { this.upvotes = upvotes; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public boolean isUpvotedByCurrentUser() { return upvotedByCurrentUser; }
        public void setUpvotedByCurrentUser(boolean upvotedByCurrentUser) { this.upvotedByCurrentUser = upvotedByCurrentUser; }
    }
}
