# Gemini AI Setup

The course builder's "Generate from PDF" feature calls `gemini-2.5-flash-lite` via the Google Generative Language REST API.

## Required env vars

| Variable | Required | Default | Notes |
|---|---|---|---|
| `GEMINI_API_KEY` | yes | (empty) | Get one from <https://aistudio.google.com/app/apikey>. Without it, `/ai/pdf/generate` returns 502. |
| `GEMINI_MODEL` | no | `gemini-2.5-flash-lite` | Override only if you have a reason. |
| `GEMINI_BASE_URL` | no | `https://generativelanguage.googleapis.com` | For testing against a mock or proxy. |
| `GEMINI_TIMEOUT_SECS` | no | `60` | Read timeout on the upstream call. |

## Local dev

`src/main/resources/application.properties` is gitignored. If you don't have one, copy `application.example.properties` next to it and fill in your local values. Spring's relaxed binding will also pick up the env vars directly with no properties-file entry — `gemini.api.key` resolves from `GEMINI_API_KEY` automatically.

```bash
export GEMINI_API_KEY=...   # from Google AI Studio
./mvnw spring-boot:run
```

## Endpoint

```
POST /api/v1/ai/pdf/generate         Authorization: Bearer <professor-token>
Content-Type: multipart/form-data

file=<PDF, application/pdf, ≤ 15 MB>
questionCount=<integer, 3..10, default 5>
language=<"ro" | "en", default "ro">
```

Returns `{ summary, quiz }` matching the same shape used by `ModuleQuiz` (without persisting anything server-side — the professor reviews and saves explicitly).

Errors:
- `400` — bad input (wrong content-type, oversize, count out of range)
- `413` — `PDF_TOO_LARGE` (max upload size exceeded)
- `502` — `AI_UPSTREAM_ERROR` (Gemini unavailable, safety block, schema violation)

## Limits

- 15 MB PDF size cap. Enforced at the multipart parser (`AiConfig#multipartConfigElement`) so oversize files get `413 PDF_TOO_LARGE` consistently. `PdfAiService` re-checks as defense in depth.
- 16 MB multipart request cap (leaves headroom for the form fields).
- Question count: 3–10.

## What is NOT logged

PDF bytes, base64 payload, Gemini prompt, Gemini response body. Logs include only: user id, filename, file size, finish reason, HTTP status.
