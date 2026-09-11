package com.learning.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.learning.platform.dto.EmailNotificationAudit;
import com.learning.platform.model.Course;
import com.learning.platform.model.Enrollment;
import com.learning.platform.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Resend Email Service Unit Tests")
class ResendEmailServiceTest {

    private ResendEmailService resendEmailService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        resendEmailService = new ResendEmailService(objectMapper);
    }

    @Test
    @DisplayName("Should format enrollment email with course metadata and call-to-action")
    void testBuildEnrollmentHtml() {
        User user = new User("user_1", "Shivam Gupta", "shivam.gupta@enterprise.io", "Junior Data Engineer", "ROLE_CLOUD_ARCHITECT", "Cloud Native Solutions Architect");
        Course course = new Course();
        course.setId("PLAN_ADE_01");
        course.setTitle("Applied Data Engineering");
        course.setDescription("Master end-to-end data pipeline development");
        course.setEstimatedHours(10);
        course.setXpReward(650);
        course.setTrack("Data");

        String html = resendEmailService.buildEnrollmentHtml(user, course);

        assertNotNull(html);
        assertTrue(html.contains("Shivam Gupta"), "HTML should include user's name");
        assertTrue(html.contains("Applied Data Engineering"), "HTML should include course title");
        assertTrue(html.contains("+650 XP"), "HTML should include XP reward");
        assertTrue(html.contains("10 Hours"), "HTML should include estimated duration");
        assertTrue(html.contains("https://resend.com"), "HTML should credit Resend in footer");
    }

    @Test
    @DisplayName("Should format course completion certificate email with XP and status")
    void testBuildCourseCompletionHtml() {
        User user = new User("user_1", "Shivam Gupta", "shivam.gupta@enterprise.io", "Junior Data Engineer", "ROLE_CLOUD_ARCHITECT", "Cloud Native Solutions Architect");
        Course course = new Course();
        course.setId("PLAN_ADE_01");
        course.setTitle("Applied Data Engineering");
        course.setXpReward(650);

        Enrollment enrollment = new Enrollment("user_1", "PLAN_ADE_01");
        enrollment.setTotalLessons(13);
        enrollment.setCompletedLessonsCount(13);
        enrollment.setProgressPercentage(100);

        String html = resendEmailService.buildCourseCompletionHtml(user, course, enrollment);

        assertNotNull(html);
        assertTrue(html.contains("Shivam Gupta"));
        assertTrue(html.contains("Applied Data Engineering"));
        assertTrue(html.contains("100%"));
        assertTrue(html.contains("+650 XP"));
        assertTrue(html.contains("13 comprehensive lessons"));
    }

    @Test
    @DisplayName("Should format milestone level up and badge emails")
    void testBuildMilestoneHtml() {
        User user = new User("user_2", "Isha Agarwal", "isha.agarwal@enterprise.io", "Mid-Level Backend Developer", "ROLE_SR_BACKEND_ENG", "Senior Backend Engineer");
        user.setCurrentLevel(3);
        user.setLevelTitle("Skill Specialist");

        String html = resendEmailService.buildMilestoneHtml(user, "LEVEL UP", "Promoted to Level 3 (Skill Specialist)", "Technical mastery is expanding.", 300);

        assertNotNull(html);
        assertTrue(html.contains("Level 3"));
        assertTrue(html.contains("Skill Specialist"));
        assertTrue(html.contains("+300 XP"));
        assertTrue(html.contains("LEVEL UP"));
    }

    @Test
    @DisplayName("Should dispatch email in simulation mode and record in audit deque")
    void testSendCustomEmailSimulation() {
        EmailNotificationAudit audit = resendEmailService.sendCustomEmail(
                "delivered@resend.dev",
                "Unit Test Notification",
                "<h1>Hello World</h1>",
                "TEST"
        ).join();

        assertNotNull(audit);
        assertEquals("delivered@resend.dev", audit.getRecipient());
        assertEquals("Unit Test Notification", audit.getSubject());
        assertEquals("SIMULATED_SUCCESS", audit.getStatus());
        assertTrue(audit.getResendMessageId().startsWith("sim_"));

        List<EmailNotificationAudit> history = resendEmailService.getRecentDispatchedEmails();
        assertFalse(history.isEmpty());
        assertEquals(audit.getId(), history.get(0).getId());
    }
}
