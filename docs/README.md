# CareerPulse Documentation Hub

Welcome to the **CareerPulse Learning & Advancement Platform** documentation suite. This hub provides comprehensive user manuals, feature-specific architecture guides, visual verification evidence, and release governance reports.

---

## 📚 Documentation Index

### 1. User & Release Guides
* **[📖 End-to-End User Guide](./USER_GUIDE.md)**: Complete operational walkthrough for Learners, Authors, and Public Credential Verifiers.
* **[📊 Automated Regression Test Report](./REGRESSION_TEST_REPORT.md)**: Live regression test matrix and screenshot evidence generated dynamically by the Playwright test suite.

---

### 2. Feature-by-Feature Deep Dives
Detailed functional breakdowns, workflows, API contracts, and Playwright screenshot evidence:

| # | Feature Guide | Key Capabilities | Visual Evidence |
| :-: | :--- | :--- | :--- |
| **01** | **[Dashboard & Gamification](./features/01_dashboard_and_gamification.md)** | Daily study streak, XP multipliers, streak shields, benchmark targets, level progression | `dashboard_svg_badges.png` |
| **02** | **[Catalog & Course Enrollment](./features/02_catalog_and_enrollment.md)** | Role tracks, category filtering, prerequisites, real-time enrollment actions | `catalog_with_enrollment_buttons.png` |
| **03** | **[My Learning & Video Tracking](./features/03_my_learning_and_video_tracking.md)** | Video lesson player, real-time 80% watch tracking, auto-unlocking module quizzes | `activity_video_tracking_unlocked.png` |
| **04** | **[Course Completion & Certification](./features/04_course_completion_and_certification.md)** | Confetti celebration, Credential ID generation, Resend email delivery, LinkedIn 1-click share | `course_completion_modal_celebration_success.png` |
| **05** | **[Public Credential Verification](./features/05_credential_verification.md)** | Instant verification URL (`?verify=...`), top-header modal lookup, badge authenticity | `public-credential-verification-success.png` |
| **06** | **[AI Quiz Arena (Google Gemini)](./features/06_ai_quiz_arena.md)** | Dynamic AI question synthesis, timer scoring, streak multipliers, rationale explanations | `quiz_interactive_results.png` |
| **07** | **[Discussions Forum & AI Mentor](./features/07_discussions_forum_and_ai_mentor.md)** | Threaded discussion topics, community Q&A, instant AI Mentor architectural solutions | `resend_email_center_verified.png` |
| **08** | **[Leaderboard & Learner Passport](./features/08_leaderboard_and_learner_passport.md)** | Global & department leaderboards, champion badges, skill competencies passport | `db_leaderboard_champion_verified.png` |
| **09** | **[Author Mode & Plan Builder](./features/09_author_mode_and_plan_builder.md)** | AI curriculum builder, custom course authoring, module & lesson structured editor | `plan_builder_view.png` |
| **10** | **[Cross-Platform Desktop & Mobile](./features/10_cross_platform_desktop_and_mobile.md)** | Electron desktop client (menus, system tray, offline fallback, safe OS browser routing), mobile PWA | `desktop_catalog_viewport.png`, `mobile_phone_catalog_viewport.png` |

---

## 🖼️ Visual Verification Gallery

All 40 high-resolution viewport captures from automated Playwright regression runs are permanently cataloged in **[`docs/screenshots/`](./screenshots/)**.

---

## 🚀 Quality Assurance & Test Automation

CareerPulse enforces 100% automated regression test coverage across all layers:
```bash
# Run all unit and integration tests
mvn test

# Run UI E2E Playwright tests and update docs/REGRESSION_TEST_REPORT.md
mvn test -Dtest="*UiTest"
```
On every Playwright run, `RegressionReportGenerator` updates **[`docs/REGRESSION_TEST_REPORT.md`](./REGRESSION_TEST_REPORT.md)** with the latest scenario pass statuses and screenshot links.
