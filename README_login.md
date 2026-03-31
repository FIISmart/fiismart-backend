# FIISmart Backend

Spring Boot web layer

---

## Requirements

- Java 21+
- Maven 3.8+
- MongoDB running locally on port 27017

## Run MongoDB (if not already running)

```bash
# If you have MongoDB installed:
mongod

# Or with Docker:
docker run -d -p 27017:27017 --name fiismart-mongo mongo:7
```

## Run the backend

```bash
mvn spring-boot:run
```

Server starts on **http://localhost:8080**.

---

## How this project is structured

```
src/main/java/
├── ro/fiismart/                  ← NEW (Spring Boot web layer)
│   ├── FiismartApplication.java  ← entry point
│   ├── config/CorsConfig.java    ← allows frontend on :5173
│   ├── dto/                      ← request/response shapes for the frontend
│   │   ├── LoginRequest.java
│   │   ├── RegisterRequest.java
│   │   ├── UserDto.java
│   │   ├── AuthResponse.java
│   │   ├── ApiResponse.java
│   │   ├── ForgotPasswordRequest.java
│   │   └── ResetPasswordRequest.java
│   ├── service/AuthService.java  ← the bridge (see below)
│   └── controller/AuthController.java ← HTTP endpoints
│
└── database/                     ← UNCHANGED from your colleagues' code
    ├── dao/                      ← MongoConnectionPool, UserDAO, CourseDAO...
    └── model/                    ← User, Course, Enrollment...
```

---

## The bridge (most important concept)

The DB model and the frontend's expected shapes don't match perfectly.
`AuthService.java` bridges the differences. Here's the mapping:

| DB stores          | Frontend expects     | How we bridge it              |
|--------------------|----------------------|-------------------------------|
| `displayName`      | `firstName+lastName` | split on first space          |
| `"student"`        | `"STUDENT"`          | `.toUpperCase()`              |
| `ObjectId`         | `String`             | `.toHexString()`              |
| `Date`             | ISO string           | `.toInstant().toString()`     |
| (no field)         | `emailVerified`      | hardcoded `false` for now     |

---

## Connecting the frontend

In the frontend project, create/edit `.env`:

```
VITE_API_BASE_URL=http://localhost:8080
```


---

## Test accounts

The database starts empty. Either:
1. Run the seeder from the DBDAO project (`Main.java`) to insert test data
2. Register via the frontend — it will create real MongoDB documents

---

## What's left to do

- [ ] Add `emailVerified` field to the `User` model (backend team's job)
- [ ] Replace in-memory token store with real JWTs (when ready)
- [ ] Add email sending for password reset / verification
- [ ] Add more controllers (CourseController, EnrollmentController, etc.)
