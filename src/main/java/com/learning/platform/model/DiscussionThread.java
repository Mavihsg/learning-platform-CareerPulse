package com.learning.platform.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discussion_threads", indexes = {
        @Index(name = "idx_thread_course_id", columnList = "course_id"),
        @Index(name = "idx_thread_author_id", columnList = "author_id"),
        @Index(name = "idx_thread_created_at", columnList = "created_at"),
        @Index(name = "idx_thread_is_resolved", columnList = "is_resolved")
})
public class DiscussionThread {

    @Id
    private String id;

    @Column(name = "course_id", nullable = false)
    private String courseId;

    @Column(name = "course_title", nullable = false)
    private String courseTitle;

    @Column(name = "lesson_id")
    private String lessonId;

    @Column(name = "lesson_title")
    private String lessonTitle;

    @Column(name = "author_id", nullable = false)
    private String authorId;

    @Column(name = "author_name", nullable = false)
    private String authorName;

    private String authorAvatar;

    private String authorRole = "Learner";

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "discussion_thread_tags", joinColumns = @JoinColumn(name = "thread_id"))
    @BatchSize(size = 25)
    private List<String> tags = new ArrayList<>();

    private int upvotes = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "discussion_thread_upvotes", joinColumns = @JoinColumn(name = "thread_id"))
    @BatchSize(size = 25)
    private List<String> upvotedUserIds = new ArrayList<>();

    private int replyCount = 0;

    @Column(name = "is_resolved")
    private boolean isResolved = false;

    private String acceptedReplyId;

    private boolean hasAiAnswer = false;

    @OneToMany(mappedBy = "thread", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("createdAt ASC")
    @JsonManagedReference
    @BatchSize(size = 25)
    private List<DiscussionReply> replies = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public DiscussionThread() {}

    public DiscussionThread(String id, String courseId, String courseTitle, String authorId,
                            String authorName, String authorAvatar, String authorRole, String title, String content) {
        this.id = id;
        this.courseId = courseId;
        this.courseTitle = courseTitle;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.authorRole = authorRole;
        this.title = title;
        this.content = content;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public void addReply(DiscussionReply reply) {
        replies.add(reply);
        reply.setThread(this);
        this.replyCount = replies.size();
        if (reply.isAiGenerated()) {
            this.hasAiAnswer = true;
        }
        this.updatedAt = LocalDateTime.now();
    }

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

    public List<String> getUpvotedUserIds() { return upvotedUserIds; }
    public void setUpvotedUserIds(List<String> upvotedUserIds) { this.upvotedUserIds = upvotedUserIds; }

    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }

    public boolean isResolved() { return isResolved; }
    public void setResolved(boolean resolved) { isResolved = resolved; }

    public String getAcceptedReplyId() { return acceptedReplyId; }
    public void setAcceptedReplyId(String acceptedReplyId) { this.acceptedReplyId = acceptedReplyId; }

    public boolean isHasAiAnswer() { return hasAiAnswer; }
    public void setHasAiAnswer(boolean hasAiAnswer) { this.hasAiAnswer = hasAiAnswer; }

    public List<DiscussionReply> getReplies() { return replies; }
    public void setReplies(List<DiscussionReply> replies) {
        this.replies = replies;
        this.replyCount = replies != null ? replies.size() : 0;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
