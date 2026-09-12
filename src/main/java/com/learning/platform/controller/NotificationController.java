package com.learning.platform.controller;

import com.learning.platform.dto.ApiResponse;
import com.learning.platform.dto.EmailNotificationAudit;
import com.learning.platform.service.ResendEmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@CrossOrigin(origins = "*")
public class NotificationController {

    private final ResendEmailService resendEmailService;

    @Value("${resend.api.key:}")
    private String apiKey;

    @Value("${resend.from.email:CareerPulse <onboarding@resend.dev>}")
    private String fromEmail;

    public NotificationController(ResendEmailService resendEmailService) {
        this.resendEmailService = resendEmailService;
    }

    @GetMapping("/recent-emails")
    public ResponseEntity<ApiResponse<List<EmailNotificationAudit>>> getRecentDispatchedEmails() {
        return ResponseEntity.ok(ApiResponse.ok(resendEmailService.getRecentDispatchedEmails()));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        boolean isLive = resendEmailService.isLiveMode();
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("provider", "Resend (https://resend.com)");
        data.put("liveMode", isLive);
        data.put("fromEmail", fromEmail);
        data.put("status", isLive ? "LIVE_READY" : "SIMULATION_AUDIT_MODE");
        data.put("maskedApiKey", resendEmailService.getMaskedApiKey());
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @PostMapping("/configure-key")
    public ResponseEntity<ApiResponse<Map<String, Object>>> configureApiKey(@RequestBody Map<String, String> payload) {
        String key = payload != null ? payload.get("apiKey") : null;
        resendEmailService.setApiKey(key);
        boolean isLive = resendEmailService.isLiveMode();
        Map<String, Object> result = new java.util.HashMap<>();
        result.put("liveMode", isLive);
        result.put("status", isLive ? "LIVE_READY" : "SIMULATION_AUDIT_MODE");
        result.put("maskedApiKey", resendEmailService.getMaskedApiKey());
        String msg = isLive 
                ? "Resend API key connected successfully! Live email delivery to inboxes is now active."
                : "Switched to Simulation & Audit Mode.";
        return ResponseEntity.ok(ApiResponse.ok(msg, result));
    }

    @PostMapping("/send-test")
    public ResponseEntity<ApiResponse<EmailNotificationAudit>> sendTestEmail(
            @RequestParam(defaultValue = "delivered@resend.dev") String to,
            @RequestParam(defaultValue = "Test Notification from CareerPulse") String subject) {
        
        String testHtml = """
                <!DOCTYPE html>
                <html>
                <head><style>body { font-family: sans-serif; background: #0b0f19; color: #fff; padding: 20px; }</style></head>
                <body>
                  <div style="max-width: 500px; margin: auto; background: #1f2937; padding: 24px; border-radius: 12px; border: 1px solid #374151;">
                    <h2 style="color: #818cf8; margin-top: 0;">🚀 CareerPulse & Resend Connection Verified!</h2>
                    <p style="color: #d1d5db;">This is a test notification verifying that transactional email dispatch via <strong>Resend</strong> is functioning properly.</p>
                    <div style="background: #111827; padding: 12px; border-radius: 8px; font-family: monospace; font-size: 13px; color: #34d399;">
                      ✓ Resend API Integration Active
                    </div>
                  </div>
                </body>
                </html>
                """;

        EmailNotificationAudit audit = resendEmailService.sendCustomEmail(to, subject, testHtml, "TEST").join();
        boolean isLive = resendEmailService.isLiveMode();
        String message;
        if (!isLive) {
            message = "Processed in Simulation Mode. (To receive emails directly in your inbox, enter your Resend API key in the connection box above)";
        } else if (audit.getStatus() != null && audit.getStatus().startsWith("FAILED")) {
            message = "Resend API Error: " + audit.getStatus();
        } else {
            message = "Live email successfully dispatched via Resend to " + audit.getRecipient();
        }
        return ResponseEntity.ok(ApiResponse.ok(message, audit));
    }
}
