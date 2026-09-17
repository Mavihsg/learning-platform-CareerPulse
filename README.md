# CareerPulse - Gamified Learning & Career Benchmark Platform

[![Build Status](https://github.com/Mavihsg/learning-platform-CareerPulse/actions/workflows/ci-cd.yml/badge.svg)](https://github.com/Mavihsg/learning-platform-CareerPulse/actions/workflows/ci-cd.yml)
[![Java 21](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot 3.3.3](https://img.shields.io/badge/Spring%20Boot-3.3.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Database](https://img.shields.io/badge/Database-Neon%20PostgreSQL-blue.svg)](https://neon.tech)
[![AI Integration](https://img.shields.io/badge/AI-Google%20Gemini-8E24AA.svg)](https://ai.google.dev/)
[![IaC](https://img.shields.io/badge/IaC-AWS%20CloudFormation-FF9900.svg)](./cloudformation/)

**CareerPulse** is an enterprise-grade, gamified learning management and role-benchmark platform. It empowers engineers to advance their careers through role benchmark tracking, interactive lesson completion, dynamic AI-powered daily quizzes, and an AI Mentor-assisted peer discussion forum.

---

## 🌟 Key Features

- 🎯 **Role Gap Analysis & Career Benchmarks**: Benchmark current skills against target industry roles (Cloud Solutions Architect, Senior Backend Engineer, Data Architect).
- 🧠 **AI-Powered Quiz Generator**: Dynamically generate quizzes from completed courses or custom technical topics using Google Gemini AI, with daily streak multiplier mechanics.
- 💬 **AI Mentor & Discussion Forum**: Community discussion boards where learners can ask technical questions and get instant, structured architecture explanations from the CareerPulse AI Mentor.
- 🏆 **Gamified XP & Badges**: XP leveling system, streak shields, milestone badges, and live team leaderboards.
- ⚡ **Sub-Millisecond Response In-Memory Caching**: Spring cache pre-warmed on boot for high-throughput concurrency.
- 🐘 **Cloud PostgreSQL Integration**: Seamless support for Neon serverless PostgreSQL with PgBouncer connection pooling and cold-start tolerance.
- 🏗️ **Infrastructure as Code (IaC)**: Production-ready AWS CloudFormation templates in [`cloudformation/`](./cloudformation/) for AWS App Runner and Amazon ECS Fargate.
- 🔄 **Automated CI/CD Pipeline**: GitHub Actions workflow that compiles, tests (100% pass rate), packages, and builds Docker containers on every commit.

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Backend** | Java 21, Spring Boot 3.3.3, Spring Data JPA, Hibernate 6, HikariCP |
| **Frontend** | Modern Vanilla JavaScript (ES6+), HTML5, CSS3 Custom Properties, Responsive Glassmorphism Design |
| **Database** | Neon.tech Serverless PostgreSQL (PgBouncer pooler), In-Memory H2 fallback |
| **AI / LLM** | Google Gemini Flash via REST APIs |
| **Containerization** | Docker multi-stage build (Maven 3.9 + Eclipse Temurin 21 JRE Alpine) |
| **Infrastructure** | AWS CloudFormation (App Runner, ECS Fargate, ECR, ALB, IAM) |
| **CI/CD** | GitHub Actions (`.github/workflows/ci-cd.yml`) |

---

## 🚀 Quick Start (Local Development)

### Prerequisites
- **JDK 21** or later
- **Maven 3.8+**
- (Optional) Git & Docker

### 1. Clone the Repository
```bash
git clone https://github.com/Mavihsg/learning-platform-CareerPulse.git
cd learning-platform-CareerPulse
```

### 2. Configure Environment Variables
Copy the `.env.example` template:
```bash
cp .env.example .env
```
*(If left unset, the application automatically falls back to an in-memory H2 database with curated mock data).*

### 3. Run the Application
```bash
mvn spring-boot:run
```
Once started, open your browser and navigate to:
👉 **[http://localhost:8080](http://localhost:8080)**

---

## 🐳 Running with Docker

Build and run using the optimized multi-stage Dockerfile:

```bash
# Build Docker image
docker build -t careerpulse-lms:latest .

# Run container
docker run -p 8080:8080 careerpulse-lms:latest
```

---

## ☁️ Cloud Deployment & Infrastructure as Code (IaC)

Infrastructure provisioning is fully codified using **AWS CloudFormation** in the [`cloudformation/`](./cloudformation/) folder:

- **[`apprunner-stack.yaml`](./cloudformation/apprunner-stack.yaml)**: AWS App Runner + ECR + Neon PostgreSQL (Lowest cost, auto-SSL, pauses on idle).
- **[`ecs-fargate-stack.yaml`](./cloudformation/ecs-fargate-stack.yaml)**: Amazon ECS Fargate + Application Load Balancer + VPC (Enterprise isolated networking).

See the [**CloudFormation README**](./cloudformation/README.md) for full deployment instructions.

---

## 📖 Documentation & User Guides

A dedicated enterprise documentation suite is available in the [`docs/`](./docs/) directory:

- **[Documentation Hub & Feature Index](./docs/README.md)**: Master architecture map and table of contents.
- **[End-to-End User Guide](./docs/USER_GUIDE.md)**: Comprehensive manual for Learners, Authors, and Credential Verifiers.
- **[Automated Regression Test Report](./docs/REGRESSION_TEST_REPORT.md)**: Live regression test matrix and screenshot evidence generated dynamically by the Playwright test suite.
- **[Feature Deep Dives](./docs/features/)**:
  1. [Dashboard & Gamification](./docs/features/01_dashboard_and_gamification.md)
  2. [Catalog & Course Enrollment](./docs/features/02_catalog_and_enrollment.md)
  3. [My Learning & Video Tracking](./docs/features/03_my_learning_and_video_tracking.md)
  4. [Course Completion & Certification](./docs/features/04_course_completion_and_certification.md)
  5. [Public Credential Verification](./docs/features/05_credential_verification.md)
  6. [AI Quiz Arena](./docs/features/06_ai_quiz_arena.md)
  7. [Discussions Forum & AI Mentor](./docs/features/07_discussions_forum_and_ai_mentor.md)
  8. [Leaderboard & Learner Passport](./docs/features/08_leaderboard_and_learner_passport.md)
  9. [Author Mode & Plan Builder](./docs/features/09_author_mode_and_plan_builder.md)
  10. [Cross-Platform Desktop & Mobile](./docs/features/10_cross_platform_desktop_and_mobile.md)

---

## 🧪 Automated Testing & Release Verification

Run the automated regression test suite locally:
```bash
# Run all unit, integration, and UI Playwright regression tests
mvn test

# Run UI E2E tests specifically and auto-update docs/REGRESSION_TEST_REPORT.md
mvn test -Dtest="*UiTest"
```
Every Playwright test execution automatically captures viewport screenshots into `docs/screenshots/` and synchronizes the live test results into **[`docs/REGRESSION_TEST_REPORT.md`](./docs/REGRESSION_TEST_REPORT.md)** for release auditability.

---

## 📄 License
This project is open source and available under the [MIT License](LICENSE).

