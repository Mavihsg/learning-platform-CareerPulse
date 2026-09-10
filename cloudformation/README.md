# AWS CloudFormation Infrastructure as Code (IaC)

This directory contains production-ready **AWS CloudFormation (CF)** templates to provision the **CareerPulse Gamified Learning Platform** infrastructure on AWS.

---

## 📁 Available CloudFormation Templates

| Template | Architecture | Best For | Cost Profile |
| :--- | :--- | :--- | :--- |
| **[`apprunner-stack.yaml`](./apprunner-stack.yaml)** | **AWS App Runner + ECR** | Fast deployment, zero maintenance, auto-scaling, auto-SSL | **Lowest Cost** (Pauses compute when idle, $0 idle cost) |
| **[`ecs-fargate-stack.yaml`](./ecs-fargate-stack.yaml)** | **Amazon ECS Fargate + ALB + VPC** | Enterprise compliance, isolated VPC, custom subnets & load balancer | Standard AWS enterprise pricing |

---

## 🚀 Option 1: Deploy with AWS App Runner (Recommended)

### Method A: Via AWS Management Console
1. Log into your [AWS Management Console](https://console.aws.amazon.com/cloudformation).
2. Go to **CloudFormation** → Click **Create stack** (With new resources).
3. Select **Upload a template file** → Choose `cloudformation/apprunner-stack.yaml`.
4. Click **Next** and fill in your parameters:
   - **ServiceName**: `careerpulse-lms`
   - **NeonDatabaseUrl**: `jdbc:postgresql://<your-neon-host>-pooler.c-2.us-east-2.aws.neon.tech/neondb?sslmode=require&reWriteBatchedInserts=true`
   - **NeonDatabaseUsername**: `neondb_owner`
   - **NeonDatabasePassword**: `<your-neon-password>`
   - **GeminiApiKey**: `<your-gemini-api-key>`
5. Click **Next** → Check *"I acknowledge that AWS CloudFormation might create IAM resources with custom names"* → Click **Submit**.

### Method B: Via AWS CLI
```bash
aws cloudformation create-stack \
  --stack-name careerpulse-apprunner \
  --template-body file://cloudformation/apprunner-stack.yaml \
  --capabilities CAPABILITY_NAMED_IAM \
  --parameters \
      ParameterKey=ServiceName,ParameterValue=careerpulse-lms \
      ParameterKey=NeonDatabaseUrl,ParameterValue="jdbc:postgresql://<neon-host>-pooler.c-2.us-east-2.aws.neon.tech/neondb?sslmode=require" \
      ParameterKey=NeonDatabaseUsername,ParameterValue=neondb_owner \
      ParameterKey=NeonDatabasePassword,ParameterValue="<your-neon-password>" \
      ParameterKey=GeminiApiKey,ParameterValue="<your-gemini-key>"
```

---

## 🐳 Pushing Your Docker Container to AWS ECR

Once the CloudFormation stack creates your ECR repository:

1. Authenticate Docker with your AWS ECR registry:
   ```bash
   aws ecr get-login-password --region us-east-2 | docker login --username AWS --password-stdin <YOUR_AWS_ACCOUNT_ID>.dkr.ecr.us-east-2.amazonaws.com
   ```
2. Build your local Docker image:
   ```bash
   docker build -t careerpulse-lms:latest .
   ```
3. Tag the image for ECR:
   ```bash
   docker tag careerpulse-lms:latest <YOUR_AWS_ACCOUNT_ID>.dkr.ecr.us-east-2.amazonaws.com/careerpulse-lms-repo:latest
   ```
4. Push to ECR:
   ```bash
   docker push <YOUR_AWS_ACCOUNT_ID>.dkr.ecr.us-east-2.amazonaws.com/careerpulse-lms-repo:latest
   ```

*(AWS App Runner automatically detects the new push and triggers an automated deployment!)*

---

## 🌐 Outputs
When stack creation finishes, visit the **Outputs** tab in CloudFormation to find your live HTTPS endpoint:
- **`ServiceUrl`**: `https://<random-id>.us-east-2.awsapprunner.com`
