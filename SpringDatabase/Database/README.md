# FIISmart – Spring Boot Backend

Spring Boot migration of the legacy `Database/DatabaseDAO` layer. Exposes a full REST API backed by MongoDB using Spring Data.

---

## Requirements

- Java 21
- Maven 3.9+
- MongoDB running on `localhost:27017`


If you do not have MongoDB installed locally, you can use Docker to quickly spin up the database.

STEPS TO START THE DATABASE WITH DOCKER 🐋:
----------------------------------

Run the following command in the directory containing the 
`docker-compose.yaml` file to start the container:
```bash
docker compose up --build -d
```
And to stop and remove the container:
```bash
docker compose down -v
```
* Note: The `-v` flag is used to remove volumes (use this if you want to wipe the database data).
---

## Build & Run

```bash
cd SpringDatabase/Database

# Run (starts MongoDB connection + DataInitializer CRUD suite)
./mvnw spring-boot:run

# Build JAR
./mvnw clean package

# Run tests
./mvnw test
```

---

## Configuration

`src/main/resources/application.properties`:

```properties
spring.data.mongodb.uri=mongodb://localhost:27017/FIISmart
```

The database name is `FIISmart` — the same as the legacy DAO module.

---

## Architecture

```
com.app
├── FiiSmartApplication.java       Entry point (@SpringBootApplication)
├── model/                         @Document entities (MongoDB collections)
├── dto/
│   ├── request/                   Inbound DTOs with Jakarta Validation
│   └── response/                  Outbound DTOs (returned to client)
├── repository/                    MongoRepository interfaces
├── service/                       Business logic + DTO↔Entity mapping
├── controller/                    REST controllers (demo/reference)
├── exception/                     Custom exceptions + GlobalExceptionHandler
└── config/                        DataInitializer (CommandLineRunner)
```

### Design Decisions

| Decision | Rationale |
|---|---|
| `MongoRepository` for queries | Derives queries from method names; no boilerplate |
| `MongoTemplate` for updates | Enables atomic `$set`, `$push`, `$pull`, `$inc`, `$addToSet` matching the original DAO semantics |
| Constructor injection (`@RequiredArgsConstructor`) | Avoids field injection; makes dependencies explicit and testable |
| `String` IDs | Spring Data auto-converts `String` ↔ `ObjectId` for `@Id` fields |
| `@Field("isHidden")` / `@Field("isDeleted")` | Preserves legacy MongoDB field names for soft-delete and visibility flags |
| Embedded types (Lecture, QuizQuestion, LectureProgress, Answer, ModerationFlag, Session) | Replaces raw `Document` objects with typed, Lombok-annotated classes |

---

## Collections & Models

| Collection | Model | Embedded types |
|---|---|---|
| `Users` | `User` | `Session` |
| `Courses` | `Course` | `Lecture` |
| `Quiz` | `Quiz` | `QuizQuestion` |
| `Enrollments` | `Enrollment` | `LectureProgress` |
| `Review` | `Review` | — |
| `QuizAttempt` | `QuizAttempt` | `Answer` |
| `Comments` | `Comment` | `ModerationFlag` |

---

## REST API Reference

All endpoints are prefixed with `/api`.

### Users — `/api/users`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create user (hashes password with BCrypt) |
| GET | `/{id}` | Find by ID |
| GET | `/email/{email}` | Find by email |
| GET | `/role/{role}` | Find all by role |
| GET | `/teachers` | All teachers |
| GET | `/students` | All students |
| GET | `/banned` | All banned users |
| GET | `/enrolled-in/{courseId}` | Students enrolled in a course |
| PATCH | `/{id}/display-name` | Update display name |
| PATCH | `/{id}/ban` | Ban user `{ bannedBy, reason }` |
| PATCH | `/{id}/unban` | Unban user |
| PATCH | `/{teacherId}/owned-courses/{courseId}` | Add owned course |
| DELETE | `/{teacherId}/owned-courses/{courseId}` | Remove owned course |
| PATCH | `/{studentId}/enrolled-courses/{courseId}` | Add enrolled course |
| DELETE | `/{studentId}/enrolled-courses/{courseId}` | Remove enrolled course |
| DELETE | `/{id}` | Delete user |
| GET | `/exists/email/{email}` | Email existence check |
| GET | `/count/role/{role}` | Count by role |

### Courses — `/api/courses`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create course |
| GET | `/{id}` | Find by ID |
| GET | `/` | Find all |
| GET | `/teacher/{teacherId}` | By teacher |
| GET | `/published` | Published & visible |
| GET | `/tag/{tag}` | By tag |
| GET | `/min-rating/{minRating}` | By minimum rating |
| GET | `/{courseId}/lectures` | Get lectures |
| PATCH | `/{id}/title` | Update title |
| PATCH | `/{id}/status` | Update status |
| PATCH | `/{id}/hidden` | Set hidden `{ hidden: true/false }` |
| PATCH | `/{id}/quiz/{quizId}` | Assign quiz |
| POST | `/{courseId}/lectures` | Add lecture |
| DELETE | `/{courseId}/lectures/{lectureId}` | Remove lecture |
| DELETE | `/{id}` | Delete course |
| GET | `/count/teacher/{teacherId}` | Count by teacher |

### Quizzes — `/api/quizzes`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create quiz |
| GET | `/{id}` | Find by ID |
| GET | `/course/{courseId}` | Find by course |
| GET | `/{quizId}/questions` | Get questions |
| PATCH | `/{id}/title` | Update title |
| PATCH | `/{id}/passing-score` | Update passing score |
| PATCH | `/{id}/time-limit` | Update time limit |
| POST | `/{quizId}/questions` | Add question |
| DELETE | `/{quizId}/questions/{questionId}` | Remove question |
| DELETE | `/{id}` | Delete quiz |
| DELETE | `/course/{courseId}` | Delete quiz by course |

### Enrollments — `/api/enrollments`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create enrollment |
| GET | `/{id}` | Find by ID |
| GET | `/student/{studentId}/course/{courseId}` | Find by student + course |
| GET | `/student/{studentId}` | All by student |
| GET | `/course/{courseId}` | All by course |
| GET | `/student/{studentId}/completed` | Completed by student |
| PATCH | `/{id}/status` | Update status |
| PATCH | `/{id}/progress` | Update overall progress |
| PATCH | `/{id}/complete` | Mark as completed (sets 100%) |
| POST | `/{id}/lecture-progress` | Add lecture progress entry |
| DELETE | `/{id}` | Delete enrollment |
| DELETE | `/student/{studentId}/course/{courseId}` | Delete by student + course |
| GET | `/exists/student/{studentId}/course/{courseId}` | Enrollment check |
| GET | `/count/course/{courseId}` | Count by course |

### Reviews — `/api/reviews`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create review |
| GET | `/{id}` | Find by ID |
| GET | `/student/{studentId}/course/{courseId}` | By student + course |
| GET | `/course/{courseId}` | All active reviews by course |
| GET | `/student/{studentId}` | All by student |
| GET | `/course/{courseId}/stars/{stars}` | By course + star rating |
| GET | `/course/{courseId}/avg-rating` | Compute average rating |
| PATCH | `/{id}` | Update `{ stars, body }` |
| PATCH | `/{id}/soft-delete` | Soft delete `{ deletedBy }` |
| DELETE | `/{id}` | Hard delete |
| GET | `/count/course/{courseId}` | Count active reviews |

### Quiz Attempts — `/api/quiz-attempts`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Submit attempt |
| GET | `/{id}` | Find by ID |
| GET | `/student/{studentId}` | All by student |
| GET | `/quiz/{quizId}` | All by quiz |
| GET | `/student/{studentId}/quiz/{quizId}` | By student + quiz |
| GET | `/student/{studentId}/quiz/{quizId}/latest` | Latest attempt |
| GET | `/quiz/{quizId}/passed` | Passed attempts |
| GET | `/quiz/{quizId}/avg-score` | Average score |
| GET | `/student/{studentId}/quiz/{quizId}/passed` | Has student passed? |
| GET | `/count/quiz/{quizId}/passed` | Count passed |
| DELETE | `/{id}` | Delete attempt |

### Comments — `/api/comments`

| Method | Path | Description |
|---|---|---|
| POST | `/` | Create comment or reply (set `parentCommentId` for replies) |
| GET | `/{id}` | Find by ID |
| GET | `/lecture/{lectureId}` | All active comments for lecture |
| GET | `/lecture/{lectureId}/top-level` | Top-level only |
| GET | `/{parentId}/replies` | Replies to a comment |
| GET | `/author/{authorId}` | By author |
| GET | `/flagged/{minFlags}` | Flagged comments |
| PATCH | `/{id}/body` | Update body text |
| PATCH | `/{id}/soft-delete` | Soft delete |
| PATCH | `/{id}/like/{userId}` | Add like |
| DELETE | `/{id}/like/{userId}` | Remove like |
| POST | `/{id}/flags` | Add moderation flag |
| DELETE | `/{id}/flags` | Clear all flags |
| DELETE | `/{id}` | Hard delete |
| GET | `/count/lecture/{lectureId}` | Count active comments |
| GET | `/{id}/liked-by/{userId}` | Has user liked? |

---

## DataInitializer

On startup, `DataInitializer` (implements `CommandLineRunner`) runs a complete CRUD verification cycle covering all 7 entities:

1. Creates entities with valid request DTOs
2. Performs all update operations (field updates, atomic array operations)
3. Verifies business logic (ban/unban, soft delete, average computation, etc.)
4. Cleans up all created test data

A successful run prints `=== DataInitializer: all verifications passed ===`.

---

## Error Handling

`GlobalExceptionHandler` handles:

| Exception | HTTP Status |
|---|---|
| `ResourceNotFoundException` | 404 Not Found |
| `MethodArgumentNotValidException` | 400 Bad Request (field-level errors) |
| `Exception` (catch-all) | 500 Internal Server Error |

All error responses follow:
```json
{
  "timestamp": "2026-03-30T...",
  "status": 404,
  "message": "User not found with id: abc123"
}
```

Validation errors include a field-level `errors` map.

---

## Migration Notes (Legacy DAO vs Spring Boot)

| Legacy DAO | Spring Boot |
|---|---|
| Manual `Document` serialization (`toDocument` / `fromDocument`) | Automatic via Spring Data MongoDB mapping |
| `ObjectId` for all IDs | `String` for `@Id` (auto-converted); embedded doc IDs are `String` |
| `MongoConnectionPool` singleton | Spring-managed `MongoClient` bean (configured via `application.properties`) |
| Raw `Document` for embedded arrays (sessions, lectureProgress, answers, flags) | Typed embedded classes with Lombok |
| `UpdateResult` / `DeleteResult` returned from DAO | `void` methods; HTTP status communicates outcome |
| No BCrypt in DAO (hashing in seeder) | `BCrypt.hashpw` called in `UserService.create` |
