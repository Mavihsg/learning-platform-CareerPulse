# Career Pulse | Enterprise Learning & Advancement Platform
## Executive Project Brief & Technical Architecture Documentation
**Date**: September 11, 2026  
**Repository**: [Mavihsg/learning-platform-CareerPulse](https://github.com/Mavihsg/learning-platform-CareerPulse)  
**Branch**: `main`

---

## 1. Executive Summary: What We Accomplished Today

Today’s engineering efforts focused on **real-data synchronization**, **visual UI polish**, **cloud deployment hardening**, **end-to-end browser test automation with Microsoft Playwright**, and **live database-to-frontend integrity verification** across both local H2 and cloud **Neon.tech PostgreSQL**.

### Key Milestones Delivered:

1. **Dashboard & Profile Real-Data Alignment**:
   - Eliminated all static/mockup placeholders across learner dashboards and profiles.
   - Synchronized Isha Agarwal's profile state so her real database metrics (**2,100 XP**, **Level 3 - "Skill Specialist"**) display consistently across the Dashboard hero card, header HUD level pill, and Profile page.

2. **Visual Polish & SVG Achievements**:
   - Replaced broken plain-text/font icon strings with high-contrast, scalable **inline SVGs**.
   - Built styled themed color containers (`badge-theme-amber`, `badge-theme-rose`, `badge-theme-orange`, etc.) for all achievement and milestone cards.

3. **AI Badge Navigation Alignment**:
   - Standardized navigation chips by adding matching right-aligned purple **`AI`** badges to both **Plan Builder** and **Quiz Arena** in the sidebar.
   - Removed the redundant inline AI badge from the daily quiz challenge tile on the dashboard.

4. **Cloud Deployment Feature Flag**:
   - Introduced the `app.features.demo-users-enabled` configuration property, overridable in any environment via `APP_ENABLE_DEMO_USERS`.
   - Added the `/api/config` REST endpoint to expose runtime configuration to the client.
   - Implemented automatic client-side hiding (`display: none`) and runtime guarding on `App.quickLogin` so demo accounts never appear in production/cloud deployments.

5. **Microsoft Playwright Automated UI Testing**:
   - Added native Java Playwright (`com.microsoft.playwright:playwright:1.48.0`) in Maven `test` scope—**zero Node.js dependency**.
   - Implemented a pre-installed browser channel strategy (`msedge` / `chrome`) with `PLAYWRIGHT_SKIP_BROWSER_DOWNLOAD=1` to bypass corporate network download constraints.
   - Built a custom headed demo mode with a smooth 500ms pacing delay (`slowMo`) for live visual demonstrations on screen.
   - Automated full PNG screenshot capture saved to `target/playwright-screenshots/`.

6. **Exhaustive Automated Test Coverage (8 E2E Suites)**:
   - **`AuthAndFeatureFlagUiTest`**: Netflix intro, auth modal dimensions, local demo user chips, and quick sign-in.
   - **`DashboardUiTest`**: Real profile XP, Level 3 title, SVG badge icons, and AI navigation chips.
   - **`QuizArenaUiTest`**: Navigation and challenge view switching.
   - **`QuizArenaInteractiveUiTest`**: Complete 3-question daily quiz sprint (answering, feedback reveal, submit, and score evaluation card).
   - **`PlanBuilderUiTest`**: Plan builder authoring interface, title input, and editable module cards.
   - **`CloudFeatureFlagNegativeUiTest`**: Verifies demo user chips are hidden and quick login is rejected when the feature flag is disabled.
   - **`DatabaseToFrontendIntegrityUiTest`**: Queries H2 JPA repositories (`UserRepository`, `CourseRepository`, `BadgeRepository`, `EnrollmentRepository`) and asserts every table row matches the DOM elements.
     - *Bug Caught & Fixed*: Uncovered and fixed a hardcoded `<strong id="profile-badges">2</strong>` HTML tag, binding it dynamically to `user.unlockedBadgeIds.length` (`3`).
   - **`NeonDatabaseToFrontendIntegrityUiTest`**: Connects directly to the live cloud **Neon.tech PostgreSQL** instance over PgBouncer (`ep-mute-dream-aee1esdv-pooler.c-2.us-east-2.aws.neon.tech/neondb`) and verifies cloud database rows against the frontend DOM with 100% precision.

7. **Zero Test Regressions**:
   - **33 out of 33 tests passing with `BUILD SUCCESS`** (16 backend unit/integration tests + 17 Playwright E2E tests).
   - All code committed and pushed to GitHub [`main`](https://github.com/Mavihsg/learning-platform-CareerPulse).

---

## 2. Database-to-Frontend Verification Matrix

| Domain & View | Database Source | Frontend Target | Validation Result |
| :--- | :--- | :--- | :--- |
| **Learner Profile** *(Sidebar & Profile)* | `User` entity (`name`, `role`, `email`, `level`, `levelTitle`, `XP`, `streak`) | `#sidebar-user-name`, `#profile-name`, `#profile-email`, `#profile-level`, `#profile-xp`, etc. | **100% MATCH** across Isha Agarwal & Shivam Gupta |
| **Course Catalog** *(Catalog Grid)* | `CourseRepository.findAll()` (`courses` table) | `#catalog-course-grid .catalog-card` | **100% MATCH** across all 6 courses with titles, categories, and difficulty |
| **My Learning** *(Enrolled Plans)* | `EnrollmentRepository.findByUserId(userId)` | `#my-learning-grid .my-learning-card` | **100% MATCH** with enrolled courses and progress |
| **Badges & Milestones** *(Dashboard & Profile)* | `BadgeRepository` & `user_unlocked_badges` | `#dash-badges-grid`, `#profile-badges` | **100% MATCH** with unlocked badges and dynamic count |
| **Leaderboard** *(Olympic Podium)* | `UserRepository.findAll()` sorted by `currentXp DESC` | `#leaderboard-podium-section .podium-rank-1`, `#leaderboard-total-count` | **100% MATCH** with Rank 1, 2, 3 and learner count |
| **Live Cloud Neon DB** *(Cloud Postgres)* | `ep-mute-dream-aee1esdv-pooler...` (`neondb`) | All frontend views via live cloud connection | **100% MATCH** with live cloud PostgreSQL data |

---

## 3. Technology Stack & Platform Architecture

### Architectural Overview Diagram

```
                                  CAREER PULSE ARCHITECTURE
                                  
   +-----------------------------------------------------------------------------------+
   |                                 FRONTEND CLIENT                                   |
   |   - Semantic HTML5 & Vanilla JavaScript ES6+ (Zero-Framework, Instant Load)       |
   |   - Vanilla CSS Design System with CSS Variables (Dark/Light Modes, Glassmorphism)|
   |   - Netflix-Style Cinematic Intro (Canvas/SVG Animations + Laser Streaks)         |
   |   - Canvas Confetti Particle Engine (Gamified Celebration FX)                     |
   |   - DiceBear Bottts SVG Avatar API + Google Fonts (Inter & JetBrains Mono)        |
   +------------------------------------------+----------------------------------------+
                                              | REST JSON APIs (/api/*)
                                              v
   +-----------------------------------------------------------------------------------+
   |                             BACKEND (SPRING BOOT 3)                               |
   |   - Java 21 LTS + Spring Boot 3.3.3                                               |
   |   - Spring MVC (REST Controllers: Auth, Analytics, Courses, Quizzes, Discussions)  |
   |   - Spring Data JPA + Hibernate ORM 6.5                                           |
   |   - Spring In-Memory Cache (@Cacheable) + CacheWarmupRunner (Sub-ms Latency)      |
   |   - HikariCP Connection Pool (Max 10-15 connections, keepalive tuned)             |
   |   - DatabaseIndexRunner (Automated secondary index creation & deduplication)      |
   +--------------------+--------------------------------------+-----------------------+
                        |                                      |
         +--------------+--------------+        +--------------+--------------+
         |      DATABASE LAYER         |        |    EXTERNAL INTEGRATIONS    |
         | - Neon.tech Serverless      |        | - Google Gemini AI API       |
         |   PostgreSQL (PgBouncer)    |        |   (Fallback Curated Engine)  |
         | - In-Memory H2 Database     |        | - Google Identity Services   |
         |   (Zero-config local/test)  |        | - YouTube Embed API          |
         +-----------------------------+        +-----------------------------+
                        |
                        v
   +-----------------------------------------------------------------------------------+
   |                            TESTING & AUTOMATION                                   |
   |   - Microsoft Playwright Java 1.48.0 (Headless / Headed Edge & Chrome Automation)  |
   |   - JUnit 5 & Spring Boot Test (@SpringBootTest on Random Ports)                  |
   |   - Database-to-Frontend Entity Verification & Automated Screenshot Capture       |
   +-----------------------------------------------------------------------------------+
   |                            DEVOPS & CLOUD DEPLOYMENT                              |
   |   - Apache Maven (Dependency management & multi-profile build lifecycle)          |
   |   - Docker (Multi-stage containerization)                                         |
   |   - AWS CloudFormation (App Runner & ECS Fargate infrastructure-as-code)          |
   |   - GitHub Actions CI/CD Pipeline + Git Version Control                           |
   +-----------------------------------------------------------------------------------+
```

### Detailed Component Inventory

| Category | Technology | Purpose & Implementation |
| :--- | :--- | :--- |
| **Backend Runtime** | **Java 21 LTS** | Core programming language leveraging modern features, virtual threads readiness, and enhanced performance. |
| **Application Framework** | **Spring Boot 3.3.3** | Full-stack enterprise framework managing REST controllers, security boundaries, and service beans. |
| **Persistence / ORM** | **Spring Data JPA & Hibernate 6.5** | Relational entity mappings for Users, Courses, Modules, Lessons, Enrollments, Badges, and Discussions. |
| **Cloud Database** | **Neon.tech Serverless PostgreSQL** | Production PostgreSQL hosted on AWS us-east-2 with PgBouncer connection pooling and cold-start tolerance. |
| **In-Memory Database** | **H2 Database (learningdb)** | Local development and automated test database providing zero-configuration relational storage. |
| **Connection Pool** | **HikariCP** | High-performance JDBC connection pool configured with keepalive pings and connection timeout guards. |
| **Caching Subsystem** | **Spring In-Memory Cache** | Sub-millisecond API response caching across hot endpoints, pre-warmed on boot by `CacheWarmupRunner`. |
| **Frontend Structure** | **Semantic HTML5** | Ultra-lightweight markup spanning 8 full-featured application views with zero single-page-application overhead. |
| **Frontend Styling** | **Vanilla CSS (Design Tokens)** | Custom CSS variable design system featuring dark/light modes, glassmorphism, responsive grids, and micro-interactions. |
| **Frontend Logic** | **Vanilla JavaScript (ES6+)** | `app.js` (UI state engine) and `api.js` (REST client with in-flight deduplication and TTL-based caching). |
| **Generative AI** | **Google Gemini AI API** | Generates dynamic quizzes and course syllabi on demand, backed by a robust offline curated fallback engine. |
| **Identity & Avatars** | **Google Identity & DiceBear API** | One-tap Google Sign-In support and procedural SVG avatar generation. |
| **Browser Automation** | **Microsoft Playwright Java 1.48.0** | Full browser automation, cross-browser validation, headed demo mode, and automated screenshot capture. |
| **Testing Engine** | **JUnit 5 (Jupiter)** | Modern test lifecycle with ephemeral random-port Spring Boot testing. |
| **DevOps & IaC** | **Docker & AWS CloudFormation** | Multi-stage Docker containerization and automated cloud deployment templates for AWS App Runner and ECS Fargate. |
| **Build & CI/CD** | **Apache Maven & GitHub Actions** | Automated build compilation, dependency management, and continuous integration pipeline. |

---

## 4. How to Run Demonstrations & Verify Locally

### Watch Playwright in Live Headed Mode (Browser Automates On Your Screen):
```powershell
# Watch the full Database-to-Frontend Integrity test:
mvn test "-Dtest=DatabaseToFrontendIntegrityUiTest" "-Dplaywright.headless=false"

# Watch the cloud Neon PostgreSQL verification:
mvn test "-Dtest=NeonDatabaseToFrontendIntegrityUiTest" "-Dplaywright.headless=false"

# Watch the Interactive Daily Quiz Sprint:
mvn test "-Dtest=QuizArenaInteractiveUiTest" "-Dplaywright.headless=false"
```

### Explore Manually in Your Browser:
1. Ensure the server is running (`mvn spring-boot:run` or background task).
2. Open **[http://localhost:8080](http://localhost:8080)** in Microsoft Edge or Google Chrome.
3. Click **Isha Agarwal** under *"OR QUICK SIGN-IN AS"* to inspect real 2,100 XP, Level 3 stats, badges, and the Quiz Arena.

---
*Document generated on September 11, 2026.*
