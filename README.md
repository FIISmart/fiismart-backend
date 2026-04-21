# Teacher Dashboard API

API-ul pentru dashboard-ul profesorului din platforma **FIISmart**.

---

## Structură

```
teacher_dashboard/
├── controller/
│   ├── TeacherOverviewController.java
│   ├── TeacherStatsController.java
│   ├── TeacherCoursesController.java
│   ├── TeacherQuizController.java
│   └── TeacherCommentsController.java
├── service/
│   ├── TeacherOverviewService.java
│   ├── TeacherStatsService.java
│   ├── TeacherCoursesService.java
│   ├── TeacherQuizzesService.java
│   └── TeacherCommentsService.java
└── dto/
    ├── TeacherOverviewDTO.java
    ├── TeacherStatsDTO.java
    ├── TeacherCoursesDTO.java
    ├── TeacherQuizPreviewDTO.java
    └── TeacherCommentPreviewDTO.java
```

---

## Endpoint-uri

Toate endpoint-urile folosesc prefixul `/api/teacher-dashboard`.

Autentificare temporară (mod development): header `X-Dev-UserId: <Mongo ObjectId>`.

| Metodă | Endpoint | Descriere |
|--------|----------|-----------|
| GET | `/me/overview` | Toate datele pentru pagina principală (stats + cursuri + quizuri + comentarii) |
| GET | `/me/stats` | Cardurile de statistici (studenți, cursuri active, quizuri, rată completare) |
| GET | `/me/courses` | Lista cursurilor profesorului, cu filtrare și paginare |
| GET | `/me/quizzes` | Lista quizurilor asociate cursurilor profesorului |
| GET | `/me/comments` | Comentariile relevante de pe lecturile profesorului |

---

## Parametri disponibili

**`/me/overview`**
- `coursesLimit` (default: 3)
- `quizzesLimit` (default: 5)
- `commentsLimit` (default: 3)

**`/me/courses`**
- `status` — `published` / `draft` / `all` (default: `all`)
- `limit` (default: 3)
- `offset` (default: 0)

**`/me/quizzes`**
- `limit` (default: 3)
- `offset` (default: 0)

**`/me/comments`**
- `limit` (default: 30)
- `offset` (default: 0)

---

## Cum funcționează

- **Controller** — primește request-ul HTTP și îl trimite mai departe către service
- **Service** — conține logica: interoghează baza de date prin DAO-uri și construiește răspunsul
- **DTO** — definește structura exactă a datelor returnate către frontend

---
