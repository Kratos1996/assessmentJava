# Getting Started - Java Developer Test

Welcome! This guide will help you get started with the **Java** test project.

## Step 1: Read the Requirements

1. **Start here**: Read [TEST_REQUIREMENTS.md](./TEST_REQUIREMENTS.md) for complete Java test requirements
2. **Quick reference**: Check [TEST_SUMMARY.md](./TEST_SUMMARY.md) for a quick overview
3. **Track progress**: Use [CANDIDATE_CHECKLIST.md](./CANDIDATE_CHECKLIST.md) to track your work

## Step 2: Set Up Your Environment

### Prerequisites

- **Java 11 or higher**
  ```bash
  java -version
  ```

- **Maven 3.6+**
  ```bash
  mvn -version
  ```

- **Node.js 16 or higher**
  ```bash
  node --version
  npm --version
  ```

### Install Dependencies

From the `java-test/` folder:

1. **Java Backend**
   ```bash
   cd java-backend
   mvn clean package
   ```

2. **Node.js Backend**
   ```bash
   cd ../node-backend
   npm install
   ```

3. **React Frontend**
   ```bash
   cd ../react-frontend
   npm install
   ```

## Step 3: Start the Services

**Important**: Start services in this order (inside `java-test/`):

### Terminal 1: Java Backend
```bash
cd java-backend
mvn spring-boot:run
```
You should see: `Started Application in ... seconds` and the server on `http://localhost:8080`.

### Terminal 2: Node.js Backend
```bash
cd node-backend
npm start
```
You should see: `Node.js backend server running on http://localhost:3000`.

### Terminal 3: React Frontend
```bash
cd react-frontend
npm run dev
```
You should see: `Local: http://localhost:5173`.

## Step 4: Verify Everything Works

1. **Open the frontend**: http://localhost:5173
   - You should see users and tasks displayed
   - Health status should show "ok"

2. **Test Java backend directly**:
   ```bash
   curl http://localhost:8080/health
   curl http://localhost:8080/api/users
   curl http://localhost:8080/api/tasks
   ```

3. **Test Node.js backend**:
   ```bash
   curl http://localhost:3000/health
   curl http://localhost:3000/api/users
   ```

## Step 5: Understand the Codebase

### Java Backend Structure

```
java-backend/
├── src/main/java/com/developer/test/
│   ├── Application.java        # Entry point, starts server
│   ├── controller/             # HTTP controllers
│   ├── dto/                    # Response DTOs
│   ├── model/                  # Entities (User, Task)
│   └── service/                # DataStore and business logic
└── src/main/resources/
    └── application.properties
```

**Key Files to Review**:
- `service/DataStore.java` – how data is stored and accessed
- `controller/*.java` – how HTTP endpoints are handled
- `Application.java` – how the server is started

### Current Endpoints

**GET endpoints** (already implemented):
- `GET /health` – Health check
- `GET /api/users` – Get all users
- `GET /api/users/{id}` – Get user by ID
- `GET /api/tasks` – Get all tasks (supports `?status=` and `?userId=` query params)
- `GET /api/stats` – Get statistics

**POST/PUT endpoints** (you need to implement in Java):
- `POST /api/users` – Create new user
- `POST /api/tasks` – Create new task
- `PUT /api/tasks/{id}` – Update existing task

## Step 6: Start Implementing

### Recommended Order

1. **Start with User Creation** (`POST /api/users`)
2. **Then Task Creation** (`POST /api/tasks`)
3. **Then Task Update** (`PUT /api/tasks/{id}`)
4. **Finally Logging** (request logging in Java backend)

### Tips

- **Read the existing code first** – Understand patterns before adding new code
- **Test as you go** – Use `curl` or Postman to test endpoints
- **Write tests early** – Don't wait until the end
- **Check the checklist** – Use `CANDIDATE_CHECKLIST.md` to track progress

## Step 7: Testing Your Code

You can add JUnit tests later if you have time; focus first on getting the core endpoints and logging working.

## Getting Help

- **Java & Spring Boot Docs**
- **Review existing code** – The codebase has examples of patterns to follow
- **Ask questions** – If requirements are unclear, ask for clarification

**Good luck!** 🚀
