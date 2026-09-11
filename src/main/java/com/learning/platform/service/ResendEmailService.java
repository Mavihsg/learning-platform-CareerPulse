package com.learning.platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.platform.dto.EmailNotificationAudit;
import com.learning.platform.model.Course;
import com.learning.platform.model.Enrollment;
import com.learning.platform.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedDeque;

@Service
public class ResendEmailService {

    private static final Logger log = LoggerFactory.getLogger(ResendEmailService.class);

    @Value("${resend.api.key:}")
    private String apiKey;

    @Value("${resend.from.email:CareerPulse <onboarding@resend.dev>}")
    private String fromEmail;

    @Value("${resend.override.to.email:}")
    private String overrideToEmail;

    @Value("${resend.api.url:https://api.resend.com/emails}")
    private String apiUrl;

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final Deque<EmailNotificationAudit> recentDispatches = new ConcurrentLinkedDeque<>();
    private static final int MAX_AUDIT_HISTORY = 50;

    public ResendEmailService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.restClient = RestClient.builder().build();
    }

    /**
     * Send enrollment confirmation email via Resend
     */
    public CompletableFuture<EmailNotificationAudit> sendEnrollmentEmail(User user, Course course) {
        String recipient = resolveRecipient(user != null ? user.getEmail() : null);
        String subject = "🎓 You enrolled in: " + (course != null ? course.getTitle() : "New Career Plan");
        String html = buildEnrollmentHtml(user, course);
        return sendCustomEmail(recipient, subject, html, "ENROLLMENT");
    }

    /**
     * Send 100% course completion & certification email via Resend
     */
    public CompletableFuture<EmailNotificationAudit> sendCourseCompletionEmail(User user, Course course, Enrollment enrollment) {
        String recipient = resolveRecipient(user != null ? user.getEmail() : null);
        String subject = "🏆 Certificate of Completion: " + (course != null ? course.getTitle() : "Track Completed!");
        String html = buildCourseCompletionHtml(user, course, enrollment);
        return sendCustomEmail(recipient, subject, html, "COURSE_COMPLETION");
    }

    /**
     * Send milestone achievement email (Level up or Badge unlocked) via Resend
     */
    public CompletableFuture<EmailNotificationAudit> sendMilestoneEmail(User user, String milestoneType, String milestoneTitle, String milestoneDescription, int xpEarned) {
        String recipient = resolveRecipient(user != null ? user.getEmail() : null);
        String subject = "⭐ Achievement Unlocked: " + milestoneTitle + " (" + milestoneType + ")";
        String html = buildMilestoneHtml(user, milestoneType, milestoneTitle, milestoneDescription, xpEarned);
        return sendCustomEmail(recipient, subject, html, "MILESTONE");
    }

    /**
     * Core transactional email dispatcher using Resend REST API (https://resend.com)
     */
    public CompletableFuture<EmailNotificationAudit> sendCustomEmail(String toEmail, String subject, String htmlContent, String type) {
        return CompletableFuture.supplyAsync(() -> {
            String auditId = UUID.randomUUID().toString();
            String recipient = resolveRecipient(toEmail);
            LocalDateTime now = LocalDateTime.now();

            if (apiKey != null && !apiKey.trim().isBlank()) {
                try {
                    log.info("Dispatching live Resend email to {} (Subject: {}) via {}", recipient, subject, apiUrl);

                    Map<String, Object> payload = new HashMap<>();
                    payload.put("from", fromEmail);
                    payload.put("to", List.of(recipient));
                    payload.put("subject", subject);
                    payload.put("html", htmlContent);

                    String responseBody = restClient.post()
                            .uri(apiUrl)
                            .header("Authorization", "Bearer " + apiKey.trim())
                            .contentType(MediaType.APPLICATION_JSON)
                            .body(payload)
                            .retrieve()
                            .body(String.class);

                    String resendId = "resend_" + UUID.randomUUID().toString().substring(0, 8);
                    if (responseBody != null) {
                        try {
                            JsonNode json = objectMapper.readTree(responseBody);
                            if (json.has("id")) {
                                resendId = json.get("id").asText();
                            }
                        } catch (Exception ignored) {
                        }
                    }

                    log.info("✓ Resend API dispatch successful! Message ID: {}", resendId);
                    EmailNotificationAudit audit = new EmailNotificationAudit(auditId, recipient, type, subject, htmlContent, now, "DELIVERED_LIVE", resendId);
                    recordAudit(audit);
                    return audit;

                } catch (Exception e) {
                    log.warn("Resend API dispatch error: {}. Falling back to simulation audit mode.", e.getMessage());
                    String simId = "sim_" + UUID.randomUUID().toString().substring(0, 8);
                    EmailNotificationAudit fallback = new EmailNotificationAudit(auditId, recipient, type, subject, htmlContent, now, "FALLBACK_LOGGED (" + e.getMessage() + ")", simId);
                    recordAudit(fallback);
                    return fallback;
                }
            } else {
                // Simulation & Audit Mode when RESEND_API_KEY is not configured
                String simId = "sim_" + UUID.randomUUID().toString().substring(0, 8);
                log.info("\n========================================================================\n" +
                        "[RESEND SIMULATION MODE] (Configure RESEND_API_KEY for live delivery)\n" +
                        "To: {}\nFrom: {}\nSubject: {}\nType: {}\nSimulated ID: {}\n" +
                        "========================================================================",
                        recipient, fromEmail, subject, type, simId);

                EmailNotificationAudit simulated = new EmailNotificationAudit(auditId, recipient, type, subject, htmlContent, now, "SIMULATED_SUCCESS", simId);
                recordAudit(simulated);
                return simulated;
            }
        });
    }

    private synchronized void recordAudit(EmailNotificationAudit audit) {
        recentDispatches.addFirst(audit);
        while (recentDispatches.size() > MAX_AUDIT_HISTORY) {
            recentDispatches.removeLast();
        }
    }

    public List<EmailNotificationAudit> getRecentDispatchedEmails() {
        return new ArrayList<>(recentDispatches);
    }

    private String resolveRecipient(String targetEmail) {
        if (overrideToEmail != null && !overrideToEmail.trim().isBlank()) {
            return overrideToEmail.trim();
        }
        if (targetEmail != null && !targetEmail.trim().isBlank()) {
            return targetEmail.trim();
        }
        return "delivered@resend.dev";
    }

    // =========================================================================
    // RESPONSIVE HTML EMAIL TEMPLATES (Modern Glassmorphism Design System)
    // =========================================================================

    public String buildEnrollmentHtml(User user, Course course) {
        String userName = (user != null && user.getName() != null) ? user.getName() : "Learner";
        String courseTitle = (course != null && course.getTitle() != null) ? course.getTitle() : "Curriculum Track";
        String courseDesc = (course != null && course.getDescription() != null) ? course.getDescription() : "High-impact professional skills curriculum.";
        int estHours = (course != null) ? course.getEstimatedHours() : 10;
        int xpReward = (course != null) ? course.getXpReward() : 250;
        String track = (course != null && course.getTrack() != null) ? course.getTrack() : "Engineering";

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Enrollment Confirmed</title>
          <style>
            body { margin: 0; padding: 0; background-color: #0b0f19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f3f4f6; }
            .container { max-width: 600px; margin: 40px auto; background: #111827; border: 1px solid #1f2937; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5); }
            .header { background: linear-gradient(135deg, #6366f1 0%%, #8b5cf6 50%%, #ec4899 100%%); padding: 36px 32px; text-align: center; }
            .header h1 { margin: 0; font-size: 26px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px; }
            .badge { display: inline-block; background: rgba(255, 255, 255, 0.2); padding: 4px 12px; border-radius: 9999px; font-size: 12px; font-weight: 700; color: #ffffff; text-transform: uppercase; margin-bottom: 12px; }
            .content { padding: 36px 32px; }
            .greeting { font-size: 18px; font-weight: 600; color: #f9fafb; margin-bottom: 12px; }
            .desc { font-size: 15px; line-height: 1.6; color: #9ca3af; margin-bottom: 24px; }
            .card { background: #1f2937; border: 1px solid #374151; border-radius: 12px; padding: 20px; margin-bottom: 28px; }
            .course-name { font-size: 20px; font-weight: 700; color: #ffffff; margin-bottom: 8px; }
            .meta-grid { display: table; width: 100%%; margin-top: 16px; border-top: 1px solid #374151; padding-top: 14px; }
            .meta-item { display: table-cell; text-align: center; }
            .meta-val { font-size: 16px; font-weight: 700; color: #a5b4fc; }
            .meta-label { font-size: 11px; text-transform: uppercase; color: #9ca3af; margin-top: 2px; }
            .cta-btn { display: block; width: 220px; margin: 0 auto; text-align: center; background: #6366f1; color: #ffffff !important; text-decoration: none; padding: 14px 24px; border-radius: 8px; font-weight: 700; font-size: 15px; transition: background 0.2s; }
            .footer { padding: 24px 32px; background: #0b0f19; border-top: 1px solid #1f2937; text-align: center; font-size: 12px; color: #6b7280; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="badge">🚀 Track Enrollment Active</div>
              <h1>Welcome to Your New Learning Path</h1>
            </div>
            <div class="content">
              <div class="greeting">Hello %s,</div>
              <p class="desc">
                You have successfully enrolled in <strong>%s</strong>! Your curriculum is calibrated with real-world architectural scenarios, hands-on video modules, and verified skill milestones.
              </p>
              <div class="card">
                <div class="course-name">%s</div>
                <p style="margin: 0; font-size: 14px; color: #d1d5db; line-height: 1.5;">%s</p>
                <div class="meta-grid">
                  <div class="meta-item">
                    <div class="meta-val">%d Hours</div>
                    <div class="meta-label">Est. Time</div>
                  </div>
                  <div class="meta-item">
                    <div class="meta-val">+%d XP</div>
                    <div class="meta-label">Completion Reward</div>
                  </div>
                  <div class="meta-item">
                    <div class="meta-val">%s</div>
                    <div class="meta-label">Domain Track</div>
                  </div>
                </div>
              </div>
              <a href="http://localhost:8080" class="cta-btn">Start Learning Now →</a>
            </div>
            <div class="footer">
              <p>Delivered via <a href="https://resend.com" style="color: #818cf8; text-decoration: none;">Resend</a> · CareerPulse Gamified Learning Platform</p>
              <p>© 2026 CareerPulse Inc. All rights reserved.</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(userName, courseTitle, courseTitle, courseDesc, estHours, xpReward, track);
    }

    public String buildCourseCompletionHtml(User user, Course course, Enrollment enrollment) {
        String userName = (user != null && user.getName() != null) ? user.getName() : "Champion";
        String courseTitle = (course != null && course.getTitle() != null) ? course.getTitle() : "Mastery Curriculum";
        int xpEarned = (course != null && course.getXpReward() > 0) ? course.getXpReward() : 650;
        int lessonsCompleted = (enrollment != null && enrollment.getTotalLessons() > 0) ? enrollment.getTotalLessons() : 13;

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Course Completed</title>
          <style>
            body { margin: 0; padding: 0; background-color: #0b0f19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f3f4f6; }
            .container { max-width: 600px; margin: 40px auto; background: #111827; border: 1px solid #1f2937; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5); }
            .header { background: linear-gradient(135deg, #10b981 0%%, #059669 50%%, #047857 100%%); padding: 40px 32px; text-align: center; }
            .header h1 { margin: 0; font-size: 28px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px; }
            .badge { display: inline-block; background: rgba(255, 255, 255, 0.25); padding: 4px 14px; border-radius: 9999px; font-size: 13px; font-weight: 800; color: #ffffff; margin-bottom: 12px; }
            .content { padding: 36px 32px; text-align: center; }
            .trophy { font-size: 54px; margin-bottom: 16px; }
            .congrats { font-size: 20px; font-weight: 700; color: #ffffff; margin-bottom: 10px; }
            .cert-box { background: #1f2937; border: 2px dashed #10b981; border-radius: 12px; padding: 24px; margin: 24px 0; text-align: left; }
            .cert-title { font-size: 22px; font-weight: 800; color: #34d399; margin-bottom: 8px; }
            .cert-sub { font-size: 14px; color: #9ca3af; line-height: 1.5; }
            .stat-badge { display: inline-block; background: #064e3b; border: 1px solid #059669; color: #6ee7b7; padding: 6px 14px; border-radius: 8px; font-size: 13px; font-weight: 700; margin-right: 8px; margin-top: 12px; }
            .cta-btn { display: inline-block; background: #10b981; color: #ffffff !important; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: 700; font-size: 15px; margin-top: 12px; }
            .footer { padding: 24px 32px; background: #0b0f19; border-top: 1px solid #1f2937; text-align: center; font-size: 12px; color: #6b7280; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="badge">🏆 CERTIFICATION ACHIEVED</div>
              <h1>Curriculum 100%% Completed</h1>
            </div>
            <div class="content">
              <div class="trophy">🏅</div>
              <div class="congrats">Outstanding Execution, %s!</div>
              <p style="color: #9ca3af; font-size: 15px; line-height: 1.6; margin: 0 0 20px 0;">
                You've successfully completed every single module, verified reading exercise, and video requirement in <strong>%s</strong>!
              </p>
              <div class="cert-box">
                <div class="cert-title">Verified Skill Certification</div>
                <div class="cert-sub">Issued to %s for completing %d comprehensive lessons and architectural assessments.</div>
                <div>
                  <span class="stat-badge">+%d XP Awarded</span>
                  <span class="stat-badge">100%% Progress</span>
                  <span class="stat-badge">Core Verified</span>
                </div>
              </div>
              <a href="http://localhost:8080" class="cta-btn">View My Dashboard & Badges →</a>
            </div>
            <div class="footer">
              <p>Powered by <a href="https://resend.com" style="color: #34d399; text-decoration: none;">Resend</a> · CareerPulse Professional Certification</p>
              <p>© 2026 CareerPulse Inc. All rights reserved.</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(userName, courseTitle, userName, lessonsCompleted, xpEarned);
    }

    public String buildMilestoneHtml(User user, String milestoneType, String milestoneTitle, String milestoneDescription, int xpEarned) {
        String userName = (user != null && user.getName() != null) ? user.getName() : "Learner";
        int currentLevel = (user != null) ? user.getCurrentLevel() : 2;
        String levelTitle = (user != null && user.getLevelTitle() != null) ? user.getLevelTitle() : "Architect Apprentice";

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Milestone Unlocked</title>
          <style>
            body { margin: 0; padding: 0; background-color: #0b0f19; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f3f4f6; }
            .container { max-width: 600px; margin: 40px auto; background: #111827; border: 1px solid #1f2937; border-radius: 16px; overflow: hidden; box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.5); }
            .header { background: linear-gradient(135deg, #f59e0b 0%%, #d97706 50%%, #b45309 100%%); padding: 36px 32px; text-align: center; }
            .header h1 { margin: 0; font-size: 26px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px; }
            .badge { display: inline-block; background: rgba(255, 255, 255, 0.25); padding: 4px 14px; border-radius: 9999px; font-size: 13px; font-weight: 800; color: #ffffff; margin-bottom: 12px; }
            .content { padding: 36px 32px; text-align: center; }
            .icon-circle { width: 72px; height: 72px; line-height: 72px; font-size: 38px; background: rgba(245, 158, 11, 0.15); border: 2px solid #f59e0b; border-radius: 50%%; margin: 0 auto 20px; }
            .title { font-size: 22px; font-weight: 800; color: #ffffff; margin-bottom: 8px; }
            .desc { font-size: 15px; color: #9ca3af; line-height: 1.6; margin-bottom: 24px; }
            .stats-card { background: #1f2937; border: 1px solid #374151; border-radius: 12px; padding: 18px; margin-bottom: 26px; display: table; width: 100%%; }
            .stat-cell { display: table-cell; text-align: center; }
            .stat-val { font-size: 18px; font-weight: 800; color: #fbbf24; }
            .stat-lbl { font-size: 11px; text-transform: uppercase; color: #9ca3af; margin-top: 2px; }
            .cta-btn { display: inline-block; background: #f59e0b; color: #ffffff !important; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: 700; font-size: 15px; }
            .footer { padding: 24px 32px; background: #0b0f19; border-top: 1px solid #1f2937; text-align: center; font-size: 12px; color: #6b7280; }
          </style>
        </head>
        <body>
          <div class="container">
            <div class="header">
              <div class="badge">⭐ NEW MILESTONE REACHED</div>
              <h1>%s</h1>
            </div>
            <div class="content">
              <div class="icon-circle">🎖️</div>
              <div class="title">%s</div>
              <p class="desc">%s</p>
              <div class="stats-card">
                <div class="stat-cell">
                  <div class="stat-val">Level %d</div>
                  <div class="stat-lbl">%s</div>
                </div>
                <div class="stat-cell">
                  <div class="stat-val">+%d XP</div>
                  <div class="stat-lbl">Points Credited</div>
                </div>
                <div class="stat-cell">
                  <div class="stat-val">%s</div>
                  <div class="stat-lbl">Category</div>
                </div>
              </div>
              <a href="http://localhost:8080" class="cta-btn">Check Leaderboard & Stats →</a>
            </div>
            <div class="footer">
              <p>Sent with ❤️ via <a href="https://resend.com" style="color: #fbbf24; text-decoration: none;">Resend</a> · CareerPulse</p>
              <p>© 2026 CareerPulse Inc. All rights reserved.</p>
            </div>
          </div>
        </body>
        </html>
        """.formatted(milestoneType, milestoneTitle, milestoneDescription, currentLevel, levelTitle, xpEarned, milestoneType);
    }
}
