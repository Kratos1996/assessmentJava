# What’s New

## Latest Changes

- Added a full end-to-end sample project with three services:
  - `java-backend/` — Spring Boot backend serving users, tasks, stats, and health endpoints
  - `node-backend/` — Express gateway proxying requests to the Java backend
  - `react-frontend/` — Vite-powered React UI for users, tasks, and statistics

- Added documentation and guides for the repository:
  - `README.md` — project overview, architecture, and startup instructions
  - `GETTING_STARTED.md` — setup guide for Java, Node, and React services
  - `TEST_REQUIREMENTS.md` — detailed Java test requirements and evaluation criteria
  - `TEST_SUMMARY.md` — quick summary of required tasks
  - `CANDIDATE_CHECKLIST.md` — progress tracking checklist

- Added multi-service startup support:
  - `start-all.ps1` — starts the Java backend, Node backend, React frontend, and verifies health endpoints
  - `start-mysql.ps1` — starts a local MySQL container for persistence or database testing
  - `docker-compose.yml` — service definitions for local containerized execution

- Implemented architecture flow and service separation:
  - React frontend communicates with Node.js backend on port `3000`
  - Node.js backend communicates with Java backend on port `8080`
  - Java backend serves as the data source and API implementation

- Documented the core Java backend requirements:
  - user creation via `POST /api/users`
  - task creation via `POST /api/tasks`
  - task update via `PUT /api/tasks/{id}`
  - request logging and error handling in the Java backend

## Notes

- This file captures the current “What’s New” state for the repository.
- If you want the file to reflect a specific feature set or a completed code change, I can update this with exact release notes.
