package com.learning.platform.model;

import com.fasterxml.jackson.annotation.JsonBackReference;
import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "discussion_replies", indexes = {
        @Index(name = "idx_reply_thread_id", columnList = "thread_id"),
        @Index(name = "idx_reply_created_at", columnList = "created_at")
})
public class DiscussionReply {

    @Id
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thread_id", nullable = false)
    @JsonBackReference
    private DiscussionThread thread;

    @Column(nullable = false)
    private String authorId;

    @Column(nullable = false)
    private String authorName;

    private String authorAvatar;

    private String authorRole = "Learner";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    private boolean isAiGenerated = false;

    private boolean isAcceptedSolution = false;

    private int upvotes = 0;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "discussion_reply_upvotes", joinColumns = @JoinColumn(name = "reply_id"))
    @BatchSize(size = 25)
    private List<String> upvotedUserIds = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public DiscussionReply() {}

    public DiscussionReply(String id, DiscussionThread thread, String authorId, String authorName,
                           String authorAvatar, String authorRole, String content, boolean isAiGenerated) {
        this.id = id;
        this.thread = thread;
        this.authorId = authorId;
        this.authorName = authorName;
        this.authorAvatar = authorAvatar;
        this.authorRole = authorRole;
        this.content = content;
        this.isAiGenerated = isAiGenerated;
        this.createdAt = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public DiscussionThread getThread() { return thread; }
    public void setThread(DiscussionThread thread) { this.thread = thread; }

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

    public List<String> getUpvotedUserIds() { return upvotedUserIds; }
    public void setUpvotedUserIds(List<String> upvotedUserIds) { this.upvotedUserIds = upvotedUserIds; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
