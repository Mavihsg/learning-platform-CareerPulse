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

## 🧪 Automated Testing

Run the automated test suite locally:
```bash
mvn test
```
All 15 unit and integration tests run in an isolated in-memory environment with full coverage of auth, user profiles, courses, and data ingestion.

---

## 📄 License
This project is open source and available under the [MIT License](LICENSE).
