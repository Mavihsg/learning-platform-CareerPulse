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
    private volatile String apiKey;

    @jakarta.annotation.PostConstruct
    public void initApiKey() {
        if (this.apiKey == null || this.apiKey.trim().isBlank()) {
            String envKey = System.getenv("RESEND_API_KEY");
            if (envKey != null && !envKey.trim().isBlank()) {
                this.apiKey = envKey.trim();
                log.info("Initialized Resend API key from system environment.");
                return;
            }
            java.io.File envFile = new java.io.File(".env");
            if (envFile.exists()) {
                try {
                    java.util.List<String> lines = java.nio.file.Files.readAllLines(envFile.toPath());
                    for (String line : lines) {
                        if (line.trim().startsWith("RESEND_API_KEY=")) {
                            this.apiKey = line.trim().substring("RESEND_API_KEY=".length()).trim();
                            log.info("Initialized Resend API key from local .env file.");
                            break;
                        }
                    }
                } catch (Exception ignored) {}
            }
        }
    }

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

    public void setApiKey(String key) {
        this.apiKey = key != null ? key.trim() : null;
        log.info("Resend API key updated. Live mode: {}", isLiveMode());
    }

    public String getApiKey() {
        return this.apiKey;
    }

    public boolean isLiveMode() {
        return this.apiKey != null && !this.apiKey.trim().isBlank();
    }

    public String getMaskedApiKey() {
        if (!isLiveMode()) return "";
        if (apiKey.length() <= 7) return "re_***";
        return apiKey.substring(0, 5) + "••••••••" + apiKey.substring(apiKey.length() - 3);
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
     * Send 100% course completion & official certification email via Resend
     */
    public CompletableFuture<EmailNotificationAudit> sendCourseCompletionEmail(User user, Course course, Enrollment enrollment) {
        return sendCourseCertificateEmail(user, course, enrollment);
    }

    /**
     * Send official verifiable Course Completion Certificate email to user via Resend
     */
    public CompletableFuture<EmailNotificationAudit> sendCourseCertificateEmail(User user, Course course, Enrollment enrollment) {
        String recipient = resolveRecipient(user != null ? user.getEmail() : null);
        String courseTitle = (course != null && course.getTitle() != null) ? course.getTitle() : "Mastery Curriculum";
        String subject = "🎉 Congratulations! Official Certificate of Completion: " + courseTitle;
        String html = buildCourseCertificateHtml(user, course, enrollment);
        return sendCustomEmail(recipient, subject, html, "CERTIFICATE");
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

                } catch (org.springframework.web.client.RestClientResponseException e) {
                    String errBody = e.getResponseBodyAsString();
                    log.warn("Resend API HTTP {} error: {}", e.getStatusCode(), errBody);
                    String errMsg = errBody;
                    try {
                        JsonNode j = objectMapper.readTree(errBody);
                        if (j.has("message")) {
                            errMsg = j.get("message").asText();
                        }
                    } catch (Exception ignored) {}
                    if (e.getStatusCode().value() == 403 && !recipient.equalsIgnoreCase("shivamgupta16p80@gmail.com")) {
                        log.info("Trial domain restriction detected for {}. Delivering live email to registered account owner: shivamgupta16p80@gmail.com", recipient);
                        return sendCustomEmail("shivamgupta16p80@gmail.com", subject, htmlContent, type).join();
                    }
                    String errId = "err_" + UUID.randomUUID().toString().substring(0, 8);
                    EmailNotificationAudit failed = new EmailNotificationAudit(auditId, recipient, type, subject, htmlContent, now, "FAILED (" + errMsg + ")", errId);
                    recordAudit(failed);
                    return failed;
                } catch (Exception e) {
                    log.warn("Resend API dispatch error: {}", e.getMessage());
                    String errId = "err_" + UUID.randomUUID().toString().substring(0, 8);
                    EmailNotificationAudit fallback = new EmailNotificationAudit(auditId, recipient, type, subject, htmlContent, now, "FAILED (" + e.getMessage() + ")", errId);
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
        return "shivamgupta16p80@gmail.com";
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
        return buildCourseCertificateHtml(user, course, enrollment);
    }

    public String buildCourseCertificateHtml(User user, Course course, Enrollment enrollment) {
        String userName = (user != null && user.getName() != null) ? user.getName() : "Shivam Gupta";
        String userEmail = (user != null && user.getEmail() != null) ? user.getEmail() : "learner@careerpulse.io";
        String courseTitle = (course != null && course.getTitle() != null) ? course.getTitle() : "Applied Data Engineering";
        String track = (course != null && course.getTrack() != null) ? course.getTrack() : "Engineering";
        int xpEarned = (course != null && course.getXpReward() > 0) ? course.getXpReward() : 650;
        int totalLessons = (enrollment != null && enrollment.getTotalLessons() > 0) ? enrollment.getTotalLessons() : 13;
        String certId = (enrollment != null && enrollment.getCredentialId() != null) 
                ? enrollment.getCredentialId() 
                : "CP-CERT-2026-" + Math.abs(((course != null ? course.getId() : "c") + "_" + (user != null ? user.getId() : "u")).hashCode() % 90000 + 10000);
        String issueDate = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MMMM d, yyyy"));
        String verifyUrl = "https://careerpulse-lms.onrender.com/?verify=" + certId;
        String linkedInUrl = "https://www.linkedin.com/profile/add?startTask=CERTIFICATION_NAME&name=" 
                + java.net.URLEncoder.encode(courseTitle, java.nio.charset.StandardCharsets.UTF_8)
                + "&organizationName=CareerPulse&issueYear=2026&certUrl=" 
                + java.net.URLEncoder.encode(verifyUrl, java.nio.charset.StandardCharsets.UTF_8)
                + "&certId=" + certId;

        return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="utf-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
          <title>Certificate of Completion - %s</title>
          <style>
            body { margin: 0; padding: 0; background-color: #050811; font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #f8fafc; }
            .wrapper { max-width: 680px; margin: 30px auto; background: #0c1222; border: 8px double #10b981; border-radius: 18px; padding: 8px; box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.7); }
            .inner-frame { border: 1px solid rgba(16, 185, 129, 0.4); border-radius: 10px; padding: 36px 32px; background: radial-gradient(ellipse at top, #131d38 0%%, #0c1222 100%%); text-align: center; }
            .cert-badge { display: inline-block; background: rgba(16, 185, 129, 0.15); border: 1px solid rgba(16, 185, 129, 0.4); color: #34d399; font-size: 11px; font-weight: 800; letter-spacing: 0.15em; padding: 4px 16px; border-radius: 999px; margin-bottom: 16px; text-transform: uppercase; }
            .cert-headline { font-size: 13px; letter-spacing: 0.2em; text-transform: uppercase; color: #94a3b8; margin: 0 0 6px 0; font-weight: 600; }
            .cert-title { font-size: 26px; font-weight: 800; color: #ffffff; letter-spacing: -0.5px; margin: 0 0 20px 0; }
            .cert-sub { font-size: 13px; color: #64748b; font-style: italic; margin-bottom: 8px; }
            .recipient-name { font-size: 32px; font-weight: 800; color: #38bdf8; margin: 0 0 16px 0; letter-spacing: -0.5px; }
            .cert-description { font-size: 14px; line-height: 1.6; color: #94a3b8; max-width: 520px; margin: 0 auto 24px auto; }
            .course-name-box { background: rgba(16, 185, 129, 0.08); border: 1px solid rgba(16, 185, 129, 0.3); border-radius: 10px; padding: 16px 20px; margin: 0 auto 28px auto; max-width: 500px; }
            .course-name { font-size: 20px; font-weight: 700; color: #34d399; margin: 0 0 4px 0; }
            .course-track { font-size: 12px; color: #64748b; font-family: monospace; letter-spacing: 0.05em; }
            .cert-meta-grid { display: table; width: 100%%; max-width: 500px; margin: 0 auto 28px auto; border-top: 1px solid #1e293b; border-bottom: 1px solid #1e293b; padding: 14px 0; }
            .meta-item { display: table-cell; width: 33.33%%; text-align: center; }
            .meta-label { font-size: 11px; text-transform: uppercase; color: #64748b; letter-spacing: 0.08em; margin-bottom: 4px; }
            .meta-val { font-size: 13px; font-weight: 700; color: #f1f5f9; font-family: monospace; }
            .signatures-row { display: table; width: 100%%; max-width: 520px; margin: 0 auto 24px auto; }
            .sig-col { display: table-cell; width: 50%%; text-align: center; padding: 0 16px; }
            .sig-line { border-bottom: 1px solid #334155; margin-bottom: 6px; padding-bottom: 4px; font-family: 'Brush Script MT', cursive, sans-serif; font-size: 20px; color: #cbd5e1; }
            .sig-title { font-size: 11px; color: #64748b; text-transform: uppercase; letter-spacing: 0.05em; }
            .cta-btn { display: inline-block; background: #10b981; color: #ffffff !important; text-decoration: none; padding: 12px 28px; border-radius: 8px; font-weight: 700; font-size: 14px; margin-top: 6px; box-shadow: 0 4px 14px rgba(16, 185, 129, 0.4); }
            .footer { padding: 20px 24px; text-align: center; font-size: 11px; color: #475569; }
            .footer a { color: #10b981; text-decoration: none; }
          </style>
        </head>
        <body>
          <div class="wrapper">
            <div class="inner-frame">
              <div class="cert-badge">🎉 CONGRATULATIONS ON COMPLETING YOUR COURSE!</div>
              <div class="cert-headline">CAREERPULSE ACADEMY OF ADVANCED SOFTWARE & DATA</div>
              <h1 class="cert-title">Official Certificate of Technical Mastery</h1>
              <div class="cert-sub">Outstanding achievement! This certifies that</div>
              <div class="recipient-name">%s</div>
              <div class="cert-description">
                has successfully fulfilled all required %d comprehensive lessons, hands-on modules, verified video instruction checkpoints, and architectural assessments in
              </div>
              <div class="course-name-box">
                <div class="course-name">%s</div>
                <div class="course-track">TRACK: %s · 100%%%% CURRICULUM MASTERY</div>
              </div>
              <div class="cert-meta-grid">
                <div class="meta-item">
                  <div class="meta-label">Credential ID</div>
                  <div class="meta-val">%s</div>
                </div>
                <div class="meta-item">
                  <div class="meta-label">Issued On</div>
                  <div class="meta-val">%s</div>
                </div>
                <div class="meta-item">
                  <div class="meta-label">Recognition</div>
                  <div class="meta-val">+%d XP Awarded</div>
                </div>
              </div>
              <div class="signatures-row">
                <div class="sig-col">
                  <div class="sig-line">Dr. Alex Thorne</div>
                  <div class="sig-title">Head of Curriculum & Engineering</div>
                </div>
                <div class="sig-col">
                  <div class="sig-line">CareerPulse Council</div>
                  <div class="sig-title">Credential Verification Registry</div>
                </div>
              </div>
              <div style="margin: 24px 0 12px 0; text-align: center;">
                <a href="%s" class="cta-btn" style="display: inline-block; margin-right: 8px; background: #10b981;">🔗 Verify Credential Online</a>
                <a href="%s" class="cta-btn" style="display: inline-block; background: #0a66c2; box-shadow: 0 4px 14px rgba(10, 102, 194, 0.4);">Add to LinkedIn →</a>
              </div>
            </div>
          </div>
          <div class="footer">
            <p>Dispatched via <a href="https://resend.com">Resend</a> to <strong>%s</strong> · Verifiable credential ID: <code>%s</code></p>
            <p>Official Public Verification URL: <a href="%s" style="color: #38bdf8;">%s</a></p>
            <p>© 2026 CareerPulse Inc. All rights reserved.</p>
          </div>
        </body>
        </html>
        """.formatted(courseTitle, userName, totalLessons, courseTitle, track, certId, issueDate, xpEarned, verifyUrl, linkedInUrl, userEmail, certId, verifyUrl, verifyUrl);
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
