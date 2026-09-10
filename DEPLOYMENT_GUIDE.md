# Cloud Deployment & Neon PostgreSQL Setup Guide

This guide walks you through connecting your **CareerPulse** LMS to **Neon.tech** PostgreSQL and deploying it live on the web for free using **Railway** or **Render**.

---

## Part 1: Setting Up Neon.tech (Your Current Screen)

You are currently on the **"Welcome to Neon"** creation screen! Follow these steps:

### Step 1: Create the Project in Neon
1. On your current screen, keep the settings as shown in your screenshot:
   - **Project name**: `CareerPulse`
   - **Region**: `AWS US East 2 (Ohio)` (or choose whichever is closest to you)
   - **Postgres database**: `neondb` (toggled ON)
   - **Postgres version**: `18` (or default)
2. Click the white **"Create project"** button in the bottom right.

### Step 2: Copy Your Connection Details
After clicking "Create project", Neon will immediately display your database credentials:
1. Look for the **"Connect to your database"** section or the **"Connection Details"** modal.
2. Select the **Java** or **JDBC** tab (or copy the `Connection String`).
3. Neon will show something like:
   - **URL / Host**: `jdbc:postgresql://ep-cool-pine-123456.us-east-2.aws.neon.tech/neondb?sslmode=require`
   - **Database**: `neondb`
   - **User**: `careerpulse_owner` (or similar)
   - **Password**: *(A generated password like `npg_abc123...`)*

> [!IMPORTANT]
> **Copy and save your password!** Neon only shows it once when generated (though you can reset it anytime in the Dashboard).

---

## Part 2: Testing Your Neon Connection Locally (Optional)

You can run the application locally while pointing to Neon:

```powershell
# Set your Neon credentials in your PowerShell terminal:
$env:SPRING_PROFILES_ACTIVE="prod"
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://<YOUR-NEON-HOST>/neondb?sslmode=require"
$env:SPRING_DATASOURCE_USERNAME="<YOUR-NEON-USERNAME>"
$env:SPRING_DATASOURCE_PASSWORD="<YOUR-NEON-PASSWORD>"

# Run Spring Boot
mvn spring-boot:run
```

On first startup, Hibernate will automatically connect to Neon, create all tables (`users`, `courses`, `lessons`, `enrollments`, etc.), and seed your course catalog!

---

## Part 3: Deploying Live to the Web

### Option A: Deploy on Railway (Recommended)

1. **Push your code to GitHub**:
   ```bash
   git add .
   git commit -m "Configure Neon Postgres and Dockerfile for cloud deployment"
   git push origin main
   ```
2. **Go to [Railway.app](https://railway.app)** and log in with GitHub.
3. Click **"New Project"** → **"Deploy from GitHub repo"** → select your repository.
4. Railway will automatically detect your [`Dockerfile`](file:///c:/Users/shigupta44/.gemini/antigravity-ide/scratch/gamified-learning-platform/Dockerfile) and [`railway.json`](file:///c:/Users/shigupta44/.gemini/antigravity-ide/scratch/gamified-learning-platform/railway.json).
5. Before deploying, go to the **Variables** tab in Railway and add:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<YOUR-NEON-HOST>/neondb?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` = `<YOUR-NEON-USERNAME>`
   - `SPRING_DATASOURCE_PASSWORD` = `<YOUR-NEON-PASSWORD>`
   - `GEMINI_API_KEY` = `your_gemini_api_key`
6. Click **Deploy**. Railway will build the container and provide a live public HTTPS URL (e.g. `https://careerpulse.up.railway.app`)!

---

### Option B: Deploy on Render (100% Free Tier Alternative)

If your Railway free trial has ended, **Render.com** offers 100% free web service hosting:

1. Go to **[Render.com](https://render.com)** and sign in with GitHub.
2. Click **"New +"** → **"Web Service"** → connect your GitHub repository.
3. Choose **Docker** as the runtime (Render will automatically use the root `Dockerfile`).
4. Select the **Free** instance type.
5. In **Environment Variables**, add:
   - `SPRING_PROFILES_ACTIVE` = `prod`
   - `SPRING_DATASOURCE_URL` = `jdbc:postgresql://<YOUR-NEON-HOST>/neondb?sslmode=require`
   - `SPRING_DATASOURCE_USERNAME` = `<YOUR-NEON-USERNAME>`
   - `SPRING_DATASOURCE_PASSWORD` = `<YOUR-NEON-PASSWORD>`
   - `GEMINI_API_KEY` = `your_gemini_api_key`
6. Click **"Create Web Service"**. Render will deploy your website with a free HTTPS domain!
