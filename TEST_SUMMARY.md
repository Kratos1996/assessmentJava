# Java Developer Test - Quick Summary

## What You Need to Do

### Required Tasks (2–3 hours)

1. **Add User Creation** – `POST /api/users` (in Java backend)
   - Validate input (`name`, `email`, `role`)
   - Generate unique ID
   - Return 201 with created user

2. **Add Task Creation** – `POST /api/tasks`
   - Validate input (`title`, `status`, `userId`)
   - Validate status enum and userId exists
   - Return 201 with created task

3. **Add Task Update** – `PUT /api/tasks/{id}`
   - Support partial updates
   - Validate status and userId if provided
   - Return 404 if not found

4. **Add Request Logging**
   - Log method, path, status, duration for all Java backend requests
   - Use structured logging (e.g., SLF4J + Logback)

### Optional Tasks (2–3 hours)

- Data persistence (e.g., JSON/file or database)
- Caching layer
- Request validation middleware/filter
- Enhanced health checks

### Code Quality (Ongoing)

- Write unit and integration tests
- Follow Java & Spring Boot best practices
- Add documentation
- Handle errors properly

## Evaluation

- **60%** – Technical correctness and code quality
- **25%** – Java/Spring best practices and idioms
- **15%** – Problem solving and architecture

## Full Details

See [TEST_REQUIREMENTS.md](./TEST_REQUIREMENTS.md) for complete requirements.
