# Java Developer Test Project

This project is designed to test **Java developers'** skills in building a backend service that powers a React + Node.js application.

## 📚 Documentation

- **[Getting Started](./GETTING_STARTED.md)** – Java test setup guide
- **[Test Requirements](./TEST_REQUIREMENTS.md)** – Complete Java test requirements and evaluation criteria
- **[Test Summary](./TEST_SUMMARY.md)** – Quick overview of required tasks
- **[Phase 2 Guide](./PHASE2_GUIDE.md)** – What was built in Phase 2 and how it works
- **[Phase 3 Guide](./PHASE3_GUIDE.md)** – Optional advanced features and how they work
- **[Phase 4 Guide](./PHASE4_GUIDE.md)** – Code quality, tests, and review notes
- **[Phase 5 Guide](./PHASE5_GUIDE.md)** – API key auth, rate limiting, and metrics
- **[Swagger Guide](./SWAGGER_GUIDE.md)** – OpenAPI setup and the docs URLs
- **[Candidate Checklist](./CANDIDATE_CHECKLIST.md)** – Track your progress

## Project Structure

```
java-test/
├── java-backend/     # Java Spring Boot HTTP server (data source)
├── node-backend/     # Node.js Express API server (calls Java backend)
└── react-frontend/   # React frontend (calls Node.js backend)
```

## Architecture Flow

```
React Frontend (port 5173)
    ↓
Node.js Backend (port 3000)
    ↓
Java Backend (port 8080)
```

## Quick Start

### MySQL Database

From the repository root, start the database first:

```powershell
.\start-mysql.ps1
```

Or run Docker directly:

```powershell
docker compose up -d mysql
```

### Option 1: Start everything with one script (Windows PowerShell)

From the repository root, run:

```powershell
.\start-all.ps1
```

This script will:
- verify Java, Maven, Node.js, and npm versions
- install dependencies in `node-backend` and `react-frontend`
- start the Java backend on port `8080`
- start the Node.js backend on port `3000`
- start the React frontend on port `5173`
- verify the health endpoints for each service

### Option 2: Start services manually

**Important:** Start services in this order (inside `java-test/`):

### 1. Start Java Backend (Data Source)

```bash
cd java-backend
mvn spring-boot:run
```

The Java backend will run on `http://localhost:8080` and serves as the data source.
Swagger UI will be available at `http://localhost:8080/swagger-ui/index.html`, and the raw OpenAPI JSON will be at `http://localhost:8080/v3/api-docs`.

### 2. Start Node.js Backend (API Gateway)

In a new terminal:

```bash
cd node-backend
npm install
npm start
```

The Node.js backend will run on `http://localhost:3000` and proxies requests to the Java backend.

### 3. Start React Frontend

In a new terminal:

```bash
cd react-frontend
npm install
npm run dev
```

The React frontend will run on `http://localhost:5173` and calls the Node.js backend.

## Project Overview

### Java Backend (Data Source)
A Spring Boot HTTP server that:
- Serves as the primary data source
- Stores users and tasks in-memory
- Exposes REST endpoints (`/api/users`, `/api/tasks`, `/api/stats`)
- Handles JSON requests/responses
- Implements thread-safe data access

### Node.js Backend (API Gateway)
A RESTful API server built with Express that:
- Acts as a proxy/gateway between React and Java
- Calls the Java backend for all data operations
- Provides the same API interface to the frontend
- Handles error propagation and status codes

### React Frontend
A modern React application that:
- Consumes the Node.js backend API
- Displays users and tasks
- Provides filtering and search capabilities
- Shows real-time statistics
- Features a modern, responsive UI

## Test Requirements

**📋 See [TEST_REQUIREMENTS.md](./TEST_REQUIREMENTS.md) for detailed Java test requirements and evaluation criteria.**

The test includes:
- **Phase 1**: Setup and understanding (30 min)
- **Phase 2**: Core requirements – Add POST/PUT endpoints, logging in Java backend (2–3 hours)
- **Phase 3**: Advanced features – Persistence, caching, middleware (2–3 hours)
- **Phase 4**: Code quality - Testing, documentation, best practices
- **Phase 5**: Bonus tasks - JWT authentication, rate limiting, metrics, Hibernate/MySQL persistence, file-toggle fallback

## Requirements

- Java 11+ and Maven
- Node.js 16+ and npm
