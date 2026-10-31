# SecureBank

SecureBank is an educational three-tier banking application for demonstrating a React frontend, a Spring Boot API, and PostgreSQL persistence. It uses fictional demo money only. It is **not** a real banking system, payment service, or compliance-certified product, and must not be used with real customer or financial data.

## What the application provides

- User registration with a unique fictional `SB...` customer account number
- Email verification with a six-digit OTP
- Login, server-side session cookies, logout, and profile information
- Customer-facing dashboard with account balance and activity
- INR-denominated demo accounts and transactions
- Beneficiary management
- Step-by-step simulated transfers with review, confirmation, receipt, and idempotency protection
- Searchable transaction history and statements
- Account number masking and hide-balance controls
- Customer QR code for the account identifier
- Responsive mobile navigation and hamburger menu
- Light and dark themes
- Admin dashboard for the seeded administrator account
- Flyway database migrations and a PostgreSQL health check

## Architecture

```text
Browser / mobile browser
        |
        v
Frontend: React + TypeScript + Vite, served by Nginx
        |
        | /api/* reverse proxy
        v
Backend: Spring Boot REST API
        |
        v
Database: PostgreSQL + Flyway migrations
```

The browser calls `/api/...` on the same origin. Nginx proxies those requests to the backend container, so local and mobile users do not need to call port `8080` directly.

### Main project folders

| Path | Purpose |
|---|---|
| `frontend/` | React/Vite user interface, responsive styles, Nginx configuration |
| `backend/` | Spring Boot API, authentication, banking rules, mail integration |
| `backend/src/main/resources/db/migration/` | Flyway schema migrations |
| `docker-compose.yml` | Local three-container deployment |
| `.env.example` | Environment variable template |

## Requirements

### Docker deployment (recommended for local use)

- Docker Desktop with the Linux/WSL2 engine
- Docker Compose v2
- Git

### Running without Docker

- Java 21
- Maven
- Node.js 22 or later
- npm
- PostgreSQL 16 or a compatible PostgreSQL server

## Run locally with Docker Compose

Open PowerShell in the project directory:

```powershell
Set-Location "D:\3 Tier"
Copy-Item .env.example .env
docker compose up -d --build
```

Check the containers:

```powershell
docker compose ps
```

The expected services are:

| Service | Local address | Purpose |
|---|---|---|
| Frontend | <http://localhost:5173> | Web application |
| Backend | <http://localhost:8080> | REST API and health endpoint |
| PostgreSQL | `localhost:5432` | Persistent database |

Open <http://localhost:5173> in a browser. The frontend is the normal entry point.

Check backend health:

```powershell
curl.exe -i http://localhost:8080/actuator/health
```

Expected response:

```json
{"status":"UP"}
```

View logs:

```powershell
docker compose logs -f backend
docker compose logs -f frontend
```

Stop the application without deleting data:

```powershell
docker compose down
```

Reset the local database and all demo users (destructive):

```powershell
docker compose down -v
```

Do not use `down -v` unless a complete database reset is intentional. The normal named volume preserves users, accounts, and transactions between restarts.

## First-use workflow

1. Open <http://localhost:5173>.
2. Select **Create an account**.
3. Enter a full name, email, and a password of at least 12 characters.
4. Submit the registration form.
5. Verify the email using the six-digit OTP.
6. Without an SMTP server, the local development response displays the OTP as `Local code: ######`; the backend also logs it.
7. After verification, the application signs the user in.
8. The new user receives a unique fictional account identifier beginning with `SB`.
9. Register a second user to test transfers. Transfers require an existing active destination account.

New accounts and transactions use Indian rupees (`INR`). Entering `10` means `₹10.00`; the application does not automatically convert the amount.

### Seeded administrator

The development initializer creates the administrator from these environment variables:

```text
ADMIN_EMAIL=admin@securebank.local
ADMIN_PASSWORD=ChangeMe-Dev-Only-123!
```

Change both values before any shared or deployed environment. The administrator is for administration and is not seeded with a normal customer account for transfers.

## Email and OTP configuration

SMTP is optional for local development. Without SMTP, use the OTP shown in the registration response and backend logs:

```powershell
docker compose logs -f backend
```

For actual email delivery, set these values in `.env` before rebuilding/restarting:

```dotenv
SMTP_HOST=smtp.example.com
SMTP_PORT=587
SMTP_USERNAME=your-smtp-user
SMTP_PASSWORD=your-smtp-password
SMTP_AUTH=true
SMTP_STARTTLS=true
MAIL_FROM=no-reply@example.com
```

Never commit `.env` or SMTP credentials. In AWS, use Amazon SES or another managed email provider and store credentials in AWS Secrets Manager rather than in source code or a committed file.

## Test on a phone

The phone and computer must use the same Wi-Fi network. Find the computer's Wi-Fi IPv4 address:

```powershell
Get-NetIPAddress -AddressFamily IPv4
```

For example, if the address is `192.168.1.24`, open this URL on the phone:

```text
http://192.168.1.24:5173
```

Do not use `localhost` on the phone; on a phone, `localhost` means the phone itself.

If Windows Firewall blocks access, allow the frontend port in an elevated PowerShell:

```powershell
New-NetFirewallRule `
  -DisplayName "SecureBank Frontend 5173" `
  -Direction Inbound `
  -Protocol TCP `
  -LocalPort 5173 `
  -Action Allow
```

Test registration, OTP verification, login after logout, the hamburger menu, dark mode, account masking, QR code, transfers, receipts, and logout. Also test narrow widths such as 320px and 375px in browser device emulation.

## Run without Docker

Start PostgreSQL separately and create a database named `securebank`. Then configure the backend:

```powershell
$env:DB_URL="jdbc:postgresql://localhost:5432/securebank"
$env:DB_USERNAME="securebank"
$env:DB_PASSWORD="your-local-password"
$env:COOKIE_SECURE="false"
$env:CORS_ORIGIN="http://localhost:5173"
Set-Location backend
.\mvnw spring-boot:run
```

In another PowerShell window:

```powershell
Set-Location frontend
npm install
npm run dev
```

Open the Vite URL shown in the terminal, normally <http://localhost:5173>.

## API contract

Protected requests use the HttpOnly `JSESSIONID` server-side session cookie. The frontend sends credentials with same-origin requests.

| Method | Endpoint | Purpose |
|---|---|---|
| `POST` | `/api/auth/register` | Register a user and create verification challenge |
| `POST` | `/api/auth/verify-email` | Verify the six-digit registration OTP |
| `POST` | `/api/auth/login` | Start an authenticated session |
| `POST` | `/api/auth/logout` | End the session and delete the cookie |
| `GET` | `/api/auth/me` | Return the current profile |
| `GET` | `/api/accounts` | Return the current user's accounts |
| `GET` | `/api/beneficiaries` | Return the current user's beneficiaries |
| `POST`/`DELETE` | `/api/beneficiaries[/{id}]` | Manage owned beneficiaries |
| `POST` | `/api/transfers` | Execute a simulated atomic transfer |
| `GET` | `/api/transactions?search=` | Return transaction history |
| `GET` | `/actuator/health` | Container/load-balancer health check |

Transfers require an authenticated customer session, an active owned source account, an existing active destination account, matching currency, a positive amount with at most two decimal places, sufficient balance, and an `X-Idempotency-Key` header.

## Testing and validation

Frontend:

```powershell
Set-Location frontend
npm install
npm run build
npm test
```

Backend:

```powershell
Set-Location backend
mvn test
```

Docker deployment smoke check:

```powershell
Set-Location "D:\3 Tier"
docker compose up -d --build
docker compose ps
curl.exe -i http://localhost:8080/actuator/health
```

## AWS three-tier deployment plan

The project is containerized and is a suitable starting point for AWS deployment, but the AWS infrastructure is not created by this repository. Do not expose the development Docker Compose defaults directly to the internet.

### Recommended production topology

```text
Users
  |
  v
Route 53 + HTTPS certificate (ACM)
  |
  v
CloudFront/WAF or public Application Load Balancer
  |                         |
  | frontend                | /api/*
  v                         v
Frontend service        Backend service
S3/CloudFront or        ECS Fargate private service
ECS/Nginx               behind an internal/private path
                              |
                              v
                       Amazon RDS PostgreSQL
                       private subnets, backups

Amazon SES -> verification emails
Secrets Manager -> database, admin, SMTP secrets
CloudWatch -> logs, metrics, alarms
```

There are two practical deployment choices:

1. **Fastest demonstration:** run the existing three containers on one private EC2 host, publish only the frontend through an HTTPS Application Load Balancer, and keep PostgreSQL on Amazon RDS. This is easy to understand but not highly available.
2. **Recommended production-like setup:** push the backend and frontend images to Amazon ECR, run them as separate ECS Fargate services, use an ALB for routing and health checks, and use RDS PostgreSQL in private subnets. Serve the frontend from CloudFront/S3 or from the frontend container according to the routing design.

### AWS deployment steps

#### 1. Prepare the AWS account

- Choose a region.
- Create a VPC with public subnets for the load balancer and private subnets for ECS tasks and RDS.
- Create separate security groups for the ALB, frontend/backend tasks, and RDS.
- Enable CloudTrail, CloudWatch logging, and budget alerts.

#### 2. Create the database

- Create an Amazon RDS PostgreSQL instance.
- Keep it in private subnets and disable public access.
- Create a dedicated database, username, and strong password.
- Configure automated backups, encryption at rest, deletion protection, and a maintenance window.
- Allow inbound PostgreSQL traffic only from the backend task security group.

Flyway runs automatically when the backend starts and applies `V1`, `V2`, and `V3` migrations. Confirm the migration logs and take a database snapshot before production migrations.

#### 3. Build and push images to ECR

Create repositories for `securebank-backend` and `securebank-frontend`, authenticate Docker to ECR, then build and push from the project root. Replace the placeholder values:

```powershell
$REGION="ap-south-1"
$ACCOUNT_ID="123456789012"
$REGISTRY="$ACCOUNT_ID.dkr.ecr.$REGION.amazonaws.com"

aws ecr get-login-password --region $REGION |
  docker login --username AWS --password-stdin $REGISTRY

docker build -t securebank-backend ./backend
docker tag securebank-backend:latest "$REGISTRY/securebank-backend:latest"
docker push "$REGISTRY/securebank-backend:latest"

docker build -t securebank-frontend ./frontend
docker tag securebank-frontend:latest "$REGISTRY/securebank-frontend:latest"
docker push "$REGISTRY/securebank-frontend:latest"
```

Create the ECR repositories first if they do not already exist:

```powershell
aws ecr create-repository --repository-name securebank-backend --region $REGION
aws ecr create-repository --repository-name securebank-frontend --region $REGION
```

#### 4. Configure ECS tasks and secrets

Run the backend as an ECS service with:

```text
DB_URL=jdbc:postgresql://<rds-endpoint>:5432/securebank
DB_USERNAME=<secret>
DB_PASSWORD=<secret>
ADMIN_EMAIL=<secret>
ADMIN_PASSWORD=<secret>
COOKIE_SECURE=true
CORS_ORIGIN=https://<public-frontend-domain>
SMTP_HOST=<SES-or-provider-host>
SMTP_PORT=587
SMTP_USERNAME=<secret>
SMTP_PASSWORD=<secret>
SMTP_AUTH=true
SMTP_STARTTLS=true
MAIL_FROM=<verified-sender>
```

Store secrets in AWS Secrets Manager and inject them into ECS. Do not put passwords in task-definition JSON committed to the repository.

The backend health check is:

```text
GET /actuator/health
```

The frontend Nginx configuration currently proxies `/api/` to the Docker Compose service name `backend`. If the frontend runs as a standalone ECS task or on S3/CloudFront, update the deployment routing so `/api/*` reaches the ALB/backend service. Do not leave a browser-side API URL pointing to `localhost`.

#### 5. Configure HTTPS and routing

- Request an ACM certificate for the application domain.
- Put the ALB or CloudFront distribution behind HTTPS.
- Redirect HTTP to HTTPS.
- Route `/api/*` to the backend target group.
- Route the remaining paths to the frontend.
- Set the production `CORS_ORIGIN` to the exact HTTPS frontend origin.
- Set `COOKIE_SECURE=true` only when HTTPS is active.

Because authentication uses an HttpOnly session cookie, keep frontend and API routing on the same site where possible. Verify cookie behavior, login, logout, and session expiry after HTTPS is enabled.

#### 6. Configure SES

- Verify the sender identity in Amazon SES.
- Request production access if sending outside the SES sandbox.
- Allow the backend task to send email through SES.
- Put SMTP/SES configuration in Secrets Manager.
- Test registration and OTP delivery from the deployed domain.

#### 7. Observability and operations

- Send ECS application logs to CloudWatch.
- Add alarms for ALB 5xx responses, unhealthy targets, ECS task restarts, RDS storage, CPU, and connections.
- Enable RDS backups and test restore procedures.
- Deploy immutable image tags rather than relying only on `latest`.
- Use a CI/CD pipeline to run frontend build/tests and backend tests before pushing images.
- Use a migration backup and rollback plan for every database schema change.

## Production checklist

- [ ] Replace all development passwords and default admin credentials
- [ ] Use AWS Secrets Manager for secrets
- [ ] Use RDS PostgreSQL in private subnets
- [ ] Use HTTPS with ACM and set `COOKIE_SECURE=true`
- [ ] Set an exact production `CORS_ORIGIN`
- [ ] Configure SES and verify the sender domain
- [ ] Restrict security groups to required traffic only
- [ ] Do not expose PostgreSQL to the public internet
- [ ] Configure CloudWatch logs, alarms, backups, and restore testing
- [ ] Pin image versions and scan ECR images
- [ ] Review CSRF/session strategy, rate limiting, authorization, audit logging, and dependency vulnerabilities
- [ ] Obtain an independent security review before handling anything sensitive
- [ ] Keep this application limited to fictional demo money

## Security limitations

This is a learning project. It includes BCrypt password hashing, server-side sessions, ownership checks, validation, database constraints, atomic transfer transactions, and idempotency controls. It still requires substantial hardening before production use, including managed secrets, HTTPS, rate limiting, monitoring, dependency scanning, security testing, stronger operational controls, and a reviewed CSRF/session design. Never use real financial data or connect it to real payment rails.
