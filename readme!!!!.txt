Documentație APIs Landing Page (FIISmart)

Am implementat un server REST utilizând Javalin pentru a expune datele din baza de date MongoDB către interfața de utilizator (Landing Page). Toate datele sunt extrase dinamic folosind arhitectura DAO existentă, fără a modifica logica originală.

Detalii Tehnice

Framework: Javalin (Micro-framework Java)
Port: 7070
Protocol: HTTP (JSON)
CORS: Activat pentru toate originile (permite integrarea directă cu orice framework de Frontend).

Endpoint-uri Disponibile

	1. Statistici Generale
URL: GET /api/statistics

Descriere: Returnează cifrele de impact afișate în secțiunea principală a site-ului.

Date returnate:

activeStudents: Numărul total de utilizatori cu rolul "student".

totalTeachers: Numărul total de profesori.

freeCourses: Numărul de cursuri publicate și vizibile.

satisfactionRate: Rata de satisfacție (hardcoded la 98% momentan).

	2. Categorii și Filtre
URL: GET /api/categories

Descriere: Returnează lista categoriilor disponibile și numărul de cursuri din fiecare.

Logica: Extrage automat etichetele din câmpurile category, subject sau tags. Dacă o valoare este o listă (Array), o extrage pe prima pentru a defini categoria principală.

Bonus: Include cheia "Toate" care reprezintă suma totală a cursurilor.

	3. Cursuri Populare (Carduri)
URL: GET /api/courses/popular

Descriere: Returnează un array cu primele 4 cursuri publicate pentru a fi afișate sub formă de carduri.

Date incluse: Titlu, descriere, thumbnailUrl, rating mediu și numărul de înscrieri (enrollmentCount).