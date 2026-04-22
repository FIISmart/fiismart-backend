# Student API

API-ul pentru partea de student din platforma **FIISmart** — parcurgere cursuri, lecturi, progres, quiz-uri și comentarii.

---

## Structură

```
backend/
├── controller/student/
│   ├── StudentCourseController.java
│   ├── StudentLectureController.java
│   ├── StudentQuizController.java
│   └── StudentCommentController.java
├── service/student/
│   ├── StudentCourseService.java
│   ├── StudentLectureService.java
│   ├── StudentQuizService.java
│   └── StudentCommentService.java
└── dto/student/
    ├── StudentCourseHeaderDTO.java
    ├── StudentModuleDTO.java
    ├── StudentLectureDTO.java
    ├── StudentLectureDetailDTO.java
    ├── StudentLectureProgressRequest.java
    ├── StudentLectureProgressResponse.java
    ├── StudentQuizStatusDTO.java
    ├── StudentCommentDTO.java
    └── CommentCreateRequest.java
```

> Față de dashboard-ul profesorului, pachetele `controller`, `service` și `dto` au acum sub-pachete `student/` și `teacher/` pentru a separa clar cele două zone ale aplicației.

---

## Autentificare

Spre deosebire de teacher dashboard (care folosea header-ul `X-Dev-UserId`), endpoint-urile de student primesc `studentId` direct ca **path variable** (`Mongo ObjectId`).

Exemplu: `/api/students/651ab...e3/courses/651ab...f7`

---

## Endpoint-uri

Toate endpoint-urile pornesc de la prefixul `/api/students/{studentId}`.

### Curs

| Metodă | Endpoint | Descriere |
|--------|----------|-----------|
| GET | `/courses/{courseId}` | Header-ul cursului (titlu, descriere, profesor, rating, progres global, status înrolare) |

### Lecturi & module

| Metodă | Endpoint | Descriere |
|--------|----------|-----------|
| GET | `/courses/{courseId}/modules` | Toate modulele cursului, fiecare cu lecturile lui și starea quiz-ului aferent |
| GET | `/courses/{courseId}/lectures/{lectureId}` | Detaliile unei lecturi (video, imagini, durată, progresul studentului) |
| PUT | `/courses/{courseId}/lectures/{lectureId}/progress` | Salvează progresul la o lectură și recalculează progresul cursului |

### Quiz

| Metodă | Endpoint | Descriere |
|--------|----------|-----------|
| GET | `/courses/{courseId}/quiz/status` | Statusul quiz-ului cursului pentru studentul curent (ex: `not_started`, `passed`, cu ultimul scor) |

### Comentarii

| Metodă | Endpoint | Descriere |
|--------|----------|-----------|
| GET | `/courses/{courseId}/lectures/{lectureId}/comments` | Comentariile unei lecturi, organizate ca thread (comentarii + reply-uri) |
| POST | `/courses/{courseId}/lectures/{lectureId}/comments` | Adaugă un comentariu nou pe o lectură |
| POST | `/comments/{commentId}/replies` | Răspunde la un comentariu existent |
| POST | `/comments/{commentId}/like` | Toggle like pe un comentariu (dacă e pus, se șterge; dacă nu, se adaugă) |

---

## Body-uri pentru request-uri

**`PUT /lectures/{lectureId}/progress`** — `StudentLectureProgressRequest`
```json
{
  "watchedPercent": 0,
  "positionSecs": 0,
  "completed": false
}
```
Returnează `StudentLectureProgressResponse`, care include și progresul recalculat al cursului (`overallProgress`, `enrollmentStatus`, `courseCompleted`).

**`POST .../comments`** și **`POST /comments/{commentId}/replies`** — `CommentCreateRequest`
```json
{
  "body": "textul comentariului"
}
```

---

## Observații

- Endpoint-urile de student **nu** au parametri de paginare/filtrare (`limit`, `offset`, `status`) — totul vine în blocuri complete (ex: toate modulele unui curs, tot thread-ul de comentarii al unei lecturi).
- Partea de student are și **endpoint-uri care scriu** (PUT progres, POST comentarii/replies/like), nu doar GET-uri ca teacher dashboard-ul.
- `StudentModuleDTO` vine cu lecturile încorporate (`List<StudentLectureDTO>`) și cu starea quiz-ului modulului, deci un singur GET pe `/modules` e suficient ca să umpli tot ecranul cu structura cursului.

---

## Cum funcționează

- **Controller** — primește request-ul HTTP, extrage `studentId`/`courseId`/`lectureId` din URL și trimite mai departe către service
- **Service** — logica propriu-zisă: interoghează baza de date prin DAO-uri, recalculează progresul, construiește thread-ul de comentarii, validează dacă studentul e înrolat etc.
- **DTO** — definește structura exactă a datelor returnate către frontend (sau a celor primite, în cazul *Request-urilor*)
