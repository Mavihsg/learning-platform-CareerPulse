package com.learning.platform.dto;

import java.time.LocalDateTime;

public class EmailNotificationAudit {
    private String id;
    private String recipient;
    private String type;
    private String subject;
    private String htmlContent;
    private LocalDateTime dispatchedAt;
    private String status;
    private String resendMessageId;

    public EmailNotificationAudit() {
    }

    public EmailNotificationAudit(String id, String recipient, String type, String subject,
                                  String htmlContent, LocalDateTime dispatchedAt, String status, String resendMessageId) {
        this.id = id;
        this.recipient = recipient;
        this.type = type;
        this.subject = subject;
        this.htmlContent = htmlContent;
        this.dispatchedAt = dispatchedAt;
        this.status = status;
        this.resendMessageId = resendMessageId;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRecipient() {
        return recipient;
    }

    public void setRecipient(String recipient) {
        this.recipient = recipient;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getHtmlContent() {
        return htmlContent;
    }

    public void setHtmlContent(String htmlContent) {
        this.htmlContent = htmlContent;
    }

    public LocalDateTime getDispatchedAt() {
        return dispatchedAt;
    }

    public void setDispatchedAt(LocalDateTime dispatchedAt) {
        this.dispatchedAt = dispatchedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getResendMessageId() {
        return resendMessageId;
    }

    public void setResendMessageId(String resendMessageId) {
        this.resendMessageId = resendMessageId;
    }
}
