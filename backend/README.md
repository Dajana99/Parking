# SmartPark – backend

Java 21, Spring Boot 3.5, Maven, PostgreSQL + Flyway, Spring Security + JWT.

## Pokretanje lokalno

1. Napravi `.env` od primera i upiši vrednosti (lozinku baze i JWT tajnu):

       cp .env.example .env
       openssl rand -base64 48      # rezultat nalepi u JWT_SECRET

2. Podigni bazu (potreban Docker Desktop):

       docker compose up -d

3. Pokreni backend:

       mvn spring-boot:run

Aplikacija se podiže na http://localhost:8080, a Swagger UI je na
http://localhost:8080/swagger-ui.html. Flyway sam pravi tabele pri prvom pokretanju.

## Testovi

    mvn test

Integracioni testovi pokreću PostgreSQL preko Testcontainers-a, pa Docker mora da radi.

## Autentifikacija (FR-01, FR-02)

| Metoda | Putanja | Telo | Odgovor |
|---|---|---|---|
| POST | `/api/auth/register` | `{"name","email","password"}` | 201 + token; 400; 409 ako e-mail postoji |
| POST | `/api/auth/login` | `{"email","password"}` | 200 + token; 400; 401 |
| POST | `/api/auth/logout` | – | 204 (klijent briše token) |
| GET | `/api/me` | – | 200 podaci o prijavljenom korisniku; 401 bez tokena |

Odgovor na registraciju i prijavu:

```json
{
  "token": "eyJhbGciOi...",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "user": { "id": 1, "name": "Ana Anić", "email": "ana@example.com", "role": "VOZAC", "createdAt": "..." }
}
```

Svaki zaštićeni zahtev šalje zaglavlje `Authorization: Bearer <token>`.
Greške imaju oblik `{"status", "error", "message", "path", "fieldErrors"?}`.

Uloge: `VOZAC`, `OPERATER`, `ADMINISTRATOR`. Registracija uvek pravi vozača; ostale uloge dodeljuje administrator (FR-05).
