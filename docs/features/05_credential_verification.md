# Feature Guide 05: Public Credential Verification Portal

CareerPulse provides a zero-auth public verification registry enabling prospective employers, recruiters, and auditors to verify credential authenticity instantly.

---

## 🌟 Capabilities & User Value

* **Zero-Auth Verification**: No login or enterprise account required to verify a certificate.
* **Direct Deep-Link Verification**: Visiting `/?verify=CP-CERT-2026-XXXXX` opens the public verification modal immediately.
* **Top Header Lookup Button**: Global **🛡️ Verify Credential** button in the top navigation allows manual ID lookup at any time.
* **Verified Skills Breakdown**: Displays the specific verified competencies and skills earned during the curriculum.
* **Tamper-Evident Badge**: Visual green badge confirming registry authenticity against the backend database.

---

## 🖼️ Visual Evidence

![Public Credential Verification](./../screenshots/public-credential-verification-success.png)
*Figure 1: Public Credential Verification modal displaying learner name, course title, verified badge, and skills breakdown.*

---

## 🛠️ How It Works (Step-by-Step Instructions)

### Method 1: Using a Public Verification Link
1. Click any public link shared by a learner (e.g., from LinkedIn or an email):
   `https://careerpulse-lms.onrender.com/?verify=CP-CERT-2026-73235`
2. CareerPulse loads directly into the **Public Verification Portal**.
3. Review verified learner details, course name, issue date, and competencies.

### Method 2: Manual Search from Top Header
1. Click **🛡️ Verify Credential** on the top header navigation bar.
2. Enter the credential ID (e.g., `CP-CERT-2026-73235`) and click **Verify Certificate**.
3. If authentic, the complete credential ledger is displayed with LinkedIn and copy-link options.

---

## 🔌 Technical Architecture & APIs

* **Verification Endpoint**: `GET /api/credentials/verify/{credentialId}`
* **Response Payload**:
  ```json
  {
    "credentialId": "CP-CERT-2026-73235",
    "learnerName": "Shivam Gupta",
    "courseId": "PLAN_ADE_01",
    "courseTitle": "Production Spring Cloud & Microservices",
    "issueDate": "2026-09-16",
    "verified": true,
    "skills": ["Spring Boot", "Spring Cloud", "Resilience4j", "Docker"]
  }
  ```
* **Frontend Controller**: `App.openPublicVerification(credentialId)` in `app.js`.
* **Playwright Verification**: Tested by [`CredentialVerificationUiTest.java`](../../src/test/java/com/learning/platform/ui/CredentialVerificationUiTest.java).
