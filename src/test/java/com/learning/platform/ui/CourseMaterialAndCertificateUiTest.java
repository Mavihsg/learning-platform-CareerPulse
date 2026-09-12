package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import com.microsoft.playwright.options.BoundingBox;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("UI-E2E: Material Components, Markdown Formatting & Automated Certificate Email")
class CourseMaterialAndCertificateUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("Issue 1: Verify 'NOT ENROLLED' badge and difficulty pill remain on single line without wrapping")
    void testNotEnrolledBadgeLayout() {
        navigateHome();

        // Switch to Pulkit Jain (user_3) who is not enrolled in Course By Isha or Spring Cloud
        page.evaluate("() => App.quickLogin('user_3')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Catalog
        page.evaluate("() => App.navigate('catalog')");
        page.waitForSelector("#catalog-course-grid .catalog-card", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Locate cards with NOT ENROLLED status
        Locator notEnrolledBadges = page.locator(".badge-status-not-enrolled");
        assertTrue(notEnrolledBadges.count() > 0, "Catalog should have at least one NOT ENROLLED card");

        // Find the card containing 'Course By Isha'
        Locator ishaCard = page.locator(".catalog-card").filter(new Locator.FilterOptions().setHasText("Course By Isha"));
        assertTrue(ishaCard.isVisible(), "Course By Isha card must be visible");

        Locator ishaBadge = ishaCard.locator(".badge-status-not-enrolled");
        assertTrue(ishaBadge.isVisible(), "NOT ENROLLED badge must be visible");
        assertEquals("NOT ENROLLED", ishaBadge.innerText().trim());

        Locator ishaDiff = ishaCard.locator(".badge-pct-pill");
        assertTrue(ishaDiff.isVisible(), "Difficulty pill must be visible");
        assertEquals("INTERMEDIATE", ishaDiff.innerText().trim());

        // Assert BoundingBox vertical alignment: both badges must be on the EXACT same vertical line
        BoundingBox badgeBox = ishaBadge.boundingBox();
        BoundingBox diffBox = ishaDiff.boundingBox();
        assertNotNull(badgeBox, "Badge bounding box must not be null");
        assertNotNull(diffBox, "Difficulty pill bounding box must not be null");

        double yDiff = Math.abs(badgeBox.y - diffBox.y);
        assertTrue(yDiff < 8.0, "NOT ENROLLED badge and INTERMEDIATE pill must stay horizontally aligned on a single row (yDiff was " + yDiff + "px)");

        captureScreenshot("catalog_not_enrolled_alignment_fixed");
    }

    @Test
    @DisplayName("Issue 2: VIDEO lesson displays video player and hides reading material")
    void testVideoLessonShowsVideoOnly() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_3')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Plan Overview for Applied Data Engineering
        page.evaluate("() => App.openPlan('PLAN_ADE_01')");
        page.waitForSelector("#modules-tree-container .lesson-row", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Open Lesson 1: Data modeling primer (which is VIDEO)
        page.evaluate("() => App.openLessonModal('PLAN_ADE_01', 'LES_ADE_01')");
        page.waitForSelector("#lesson-player-modal", new Page.WaitForSelectorOptions().setTimeout(5000));

        // 1. Video Container must be VISIBLE
        Locator videoContainer = page.locator("#modal-video-container");
        assertTrue(videoContainer.isVisible(), "Video container must be visible for VIDEO component");

        // 2. Video Iframe must have a valid YouTube embed
        String iframeSrc = page.locator("#modal-video-iframe").getAttribute("src");
        assertNotNull(iframeSrc, "Video iframe must have a src URL");
        assertTrue(iframeSrc.contains("youtube"), "Video iframe should embed YouTube");

        // 3. Reading content must be HIDDEN for VIDEO lesson
        Locator readingContent = page.locator("#modal-reading-content");
        assertFalse(readingContent.isVisible(), "Reading material container must be hidden for VIDEO component");

        // 4. Video metadata brief must be VISIBLE
        Locator videoMeta = page.locator("#modal-video-meta");
        assertTrue(videoMeta.isVisible(), "Video metadata card must be visible for VIDEO component");
        assertTrue(videoMeta.innerText().contains("Data modeling primer"), "Video meta should display the lesson title");

        captureScreenshot("video_lesson_shows_video_only");

        // Close modal
        page.locator("#lesson-player-modal .btn-modal-close").click();
        page.waitForTimeout(300);
    }

    @Test
    @DisplayName("Issue 3: READING lesson displays formatted markdown with clean headings, code blocks, and no stray hashes")
    void testReadingLessonFormatting() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_3')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Navigate to Plan Overview for Applied Data Engineering
        page.evaluate("() => App.openPlan('PLAN_ADE_01')");
        page.waitForSelector("#modules-tree-container .lesson-row", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Open Lesson 2: Normalization in practice (which is READING)
        page.evaluate("() => App.openLessonModal('PLAN_ADE_01', 'LES_ADE_02')");
        page.waitForSelector("#lesson-player-modal", new Page.WaitForSelectorOptions().setTimeout(5000));

        // 1. Video container must be HIDDEN for READING lesson
        Locator videoContainer = page.locator("#modal-video-container");
        assertFalse(videoContainer.isVisible(), "Video container must be hidden for READING component");

        // 2. Reading content container must be VISIBLE
        Locator readingContent = page.locator("#modal-reading-content");
        assertTrue(readingContent.isVisible(), "Reading content must be visible for READING component");

        // 3. Headings must be parsed properly without rogue standalone '#'
        String rawHtml = (String) page.evaluate("() => document.getElementById('modal-reading-content').innerHTML");
        assertFalse(rawHtml.contains("<p>#</p>"), "HTML should not contain isolated floating '#' paragraph tags");
        assertFalse(rawHtml.contains("<h4>#"), "Headings should not have raw leading '#' remaining");

        // 4. Ordered and bullet lists must be parsed into proper semantic tags
        Locator listItems = readingContent.locator("li");
        assertTrue(listItems.count() >= 3, "Reading material should render semantic list items");

        // 5. Code blocks must be styled inside code-snippet-box
        Locator codeSnippets = readingContent.locator(".code-snippet-box");
        assertTrue(codeSnippets.count() > 0, "Code blocks should be wrapped in .code-snippet-box");

        captureScreenshot("reading_lesson_formatted_clean");

        // Close modal
        page.locator("#lesson-player-modal .btn-modal-close").click();
        page.waitForTimeout(300);
    }

    @Test
    @DisplayName("Issue 4: Automated Course Completion Certificate is emailed and viewable in Resend Email Center")
    void testAutomatedCertificateEmailOnCompletion() {
        navigateHome();
        page.evaluate("() => App.quickLogin('user_1')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Trigger Certificate send for Technical Writing (PLAN_TW_01 which user_1 has 100% completed)
        Object sendResult = page.evaluate("() => API.post('/courses/PLAN_TW_01/certificate/send?userId=user_1')");
        assertNotNull(sendResult, "Certificate send API must return success");

        // Open Resend Email Center
        page.evaluate("() => App.openEmailModal()");
        page.waitForSelector("#email-inbox-modal", new Page.WaitForSelectorOptions().setTimeout(5000));
        assertTrue(page.locator("#email-inbox-modal").isVisible(), "Email modal must open");

        // Check dispatched email list
        Locator dispatchedEmails = page.locator("#email-audit-list .card");
        assertTrue(dispatchedEmails.count() > 0, "At least one email must be in the audit list");

        // Verify Certificate email is present
        String emailListText = page.locator("#email-audit-list").innerText();
        assertTrue(emailListText.contains("Certificate of Completion") || emailListText.contains("CERTIFICATE"),
                "Dispatched emails must include Certificate of Completion");

        // Click Preview HTML on the certificate email
        Locator previewBtn = page.locator("#email-audit-list button").filter(new Locator.FilterOptions().setHasText("Preview HTML")).first();
        assertTrue(previewBtn.isVisible(), "Preview HTML button must exist");
        previewBtn.click();
        page.waitForTimeout(400);

        // Verify Template Preview tab is active
        assertTrue(page.locator("#tab-email-preview").getAttribute("class").contains("active"),
                "Preview tab should be active after clicking preview");

        captureScreenshot("certificate_email_preview_verified");
    }
}
