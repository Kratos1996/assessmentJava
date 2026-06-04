# Java Backend

This is the Java backend for the developer test project. It's built with Spring Boot and provides REST API endpoints for users and tasks.

## Requirements

- Java 11 or higher
- Maven 3.6+
- Optional: MySQL 8+ if you want to run the database-backed mode

### Installing Maven

**Arch Linux / Manjaro:**
```bash
sudo pacman -S maven
```

**Ubuntu / Debian:**
```bash
sudo apt-get update
sudo apt-get install maven
```

**macOS (with Homebrew):**
```bash
brew install maven
```

**Or download manually:**
- Download from https://maven.apache.org/download.cgi
- Extract and add to PATH

## Project Structure

```
java-backend/
├── src/
│   ├── main/
│   │   ├── java/com/developer/test/
│   │   │   ├── Application.java          # Spring Boot main class
│   │   │   ├── controller/               # REST controllers
│   │   │   │   ├── HealthController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── TaskController.java
│   │   │   │   └── StatsController.java
│   │   │   ├── model/                    # Entity models
│   │   │   │   ├── User.java
│   │   │   │   └── Task.java
│   │   │   ├── dto/                      # Data transfer objects
│   │   │   │   ├── UsersResponse.java
│   │   │   │   ├── TasksResponse.java
│   │   │   │   ├── StatsResponse.java
│   │   │   │   └── HealthResponse.java
│   │   │   └── service/                  # Business logic
│   │   │       └── DataStore.java
│   │   └── resources/
│   │       └── application.properties
│   └── test/                             # Test files (to be added)
└── pom.xml                               # Maven configuration
```

## Running the Application

### Using Maven

```bash
cd java-backend
mvn spring-boot:run
```

### Using Java directly

```bash
cd java-backend
mvn clean package
java -jar target/java-backend-1.0.0.jar
```

The server will start on `http://localhost:8080`

## API Endpoints

### Health Check
- `GET /health` - Returns health status

### Users
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID
- `POST /api/users` - Create a new user

### Tasks
- `GET /api/tasks` - Get all tasks
- `GET /api/tasks?status={status}` - Filter tasks by status
- `GET /api/tasks?userId={userId}` - Filter tasks by user ID
- `GET /api/tasks?status={status}&userId={userId}` - Filter by both
- `POST /api/tasks` - Create a new task
- `PUT /api/tasks/{id}` - Update an existing task

### Statistics
- `GET /api/stats` - Get statistics about users and tasks

### Security
- Write requests require `Authorization: Bearer <token>`
- Use `POST /api/auth/login` to obtain a JWT

### MySQL Mode
- Set `app.datastore.use-file-storage=true` to force the JSON snapshot store
- Leave `app.datastore.use-file-storage=false` and configure `app.mysql.url`, `app.mysql.username`, and `app.mysql.password` to use MySQL
- Use `.\start-mysql.ps1` or `docker compose up -d mysql` from the repo root to start the database container

## Current Implementation

The backend currently implements:
- ✅ Health check endpoint
- ✅ GET endpoints for users and tasks
- ✅ Statistics endpoint
- ✅ Thread-safe in-memory data storage
- ✅ CORS enabled for frontend integration

## To Be Implemented (Phase 2)

- ⏳ `POST /api/users` - Create new user
- ⏳ `POST /api/tasks` - Create new task
- ⏳ `PUT /api/tasks/{id}` - Update existing task
- ⏳ Request logging middleware

## Testing

Run tests with:
```bash
mvn test
```

## Dependencies

- Spring Boot 3.2.0
- Spring Web (REST API)
- Spring Validation
- Jackson (JSON serialization)
