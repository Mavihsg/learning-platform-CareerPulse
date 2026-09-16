package com.learning.platform.ui;

import com.microsoft.playwright.Locator;
import com.microsoft.playwright.Page;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CredentialVerificationUiTest extends BasePlaywrightUiTest {

    @Test
    @DisplayName("Public verification: Direct URL ?verify=ID automatically opens authentic verification modal with zero auth required")
    void testDirectVerifiableUrlOpensModalWithoutAuth() {
        // Recruiter / Interviewer visits public verification URL
        page.navigate(getBaseUrl() + "/?verify=CP-CERT-2026-10001");
        dismissNetflixIntro();

        // Modal should open automatically on page load
        page.waitForSelector("#credential-verification-modal", new Page.WaitForSelectorOptions().setTimeout(6000));
        Locator modal = page.locator("#credential-verification-modal");
        assertTrue(modal.isVisible(), "Credential verification modal should open on URL parameter load");

        // Wait for registry lookup success
        page.waitForSelector("#verification-success-state", new Page.WaitForSelectorOptions().setTimeout(6000));
        Locator successState = page.locator("#verification-success-state");
        assertTrue(successState.isVisible(), "Verification success state should be displayed");

        // Validate authenticity data
        Locator learnerName = page.locator("#verify-learner-name");
        assertTrue(learnerName.isVisible() && !learnerName.innerText().isBlank(), "Learner name should be displayed");

        Locator courseTitle = page.locator("#verify-course-title");
        assertTrue(courseTitle.isVisible() && !courseTitle.innerText().isBlank(), "Course title should be displayed");

        Locator credId = page.locator("#verify-credential-id");
        assertTrue(credId.innerText().contains("CP-CERT-2026-"), "Registry credential ID should be displayed");

        // Validate action buttons
        Locator copyBtn = page.locator("#btn-copy-verification-url");
        assertTrue(copyBtn.isVisible(), "Copy verifiable link button should be available");

        Locator linkedinBtn = page.locator("#btn-linkedin-share");
        assertTrue(linkedinBtn.isVisible(), "Add to LinkedIn button should be available");
        assertTrue(linkedinBtn.getAttribute("href").contains("linkedin.com/profile/add"), "LinkedIn link should target certification add");

        captureScreenshot("public-credential-verification-success");
    }

    @Test
    @DisplayName("Header action: Top bar 'Verify Credential' button opens modal in ID lookup mode")
    void testTopHeaderVerifyButtonOpensModal() {
        navigateHome();

        // Dismiss auth modal so top header is unobscured
        page.evaluate("() => { const a = document.getElementById('auth-modal'); if (a) a.style.display = 'none'; }");

        Locator headerVerifyBtn = page.locator("#btn-verify-header");
        assertTrue(headerVerifyBtn.isVisible(), "Verify Credential button should be visible in top header");

        headerVerifyBtn.click();
        page.waitForSelector("#credential-verification-modal", new Page.WaitForSelectorOptions().setTimeout(5000));

        Locator modal = page.locator("#credential-verification-modal");
        assertTrue(modal.isVisible(), "Modal should open upon clicking top header verify button");

        Locator manualInput = page.locator("#verify-manual-input");
        assertTrue(manualInput.isVisible(), "Manual credential ID input should be visible for quick lookup");
    }

    @Test
    @DisplayName("Course completion popup includes verified Credential ID, Copy Link, and LinkedIn share")
    void testCourseCompletionPopupContainsCredentialAndVerifiableLink() {
        navigateHome();

        // Login as user_2 who has completed coursework
        page.evaluate("() => App.quickLogin('user_2')");
        page.waitForSelector("#sidebar-user-name", new Page.WaitForSelectorOptions().setTimeout(6000));

        // Open completion modal for PLAN_ADE_01
        page.evaluate("() => App.showCompletionModalForCourse('PLAN_ADE_01')");
        page.waitForSelector("#course-completion-modal", new Page.WaitForSelectorOptions().setTimeout(5000));

        Locator completionModal = page.locator("#course-completion-modal");
        assertTrue(completionModal.isVisible(), "Course completion celebration popup should be visible");

        Locator credIdEl = page.locator("#completion-modal-credential-id");
        assertTrue(credIdEl.innerText().contains("CP-CERT-2026-"), "Credential ID should be displayed in completion modal");

        Locator copyLinkBtn = page.locator("#btn-completion-copy-link");
        assertTrue(copyLinkBtn.isVisible(), "Copy verification link button should be present");

        Locator linkedinBtn = page.locator("#btn-completion-linkedin");
        assertTrue(linkedinBtn.isVisible(), "Add to LinkedIn button should be present");

        captureScreenshot("completion-modal-with-verifiable-credential");
    }
}
