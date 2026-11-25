# SmartPark

Pametni parking – rezervacija mesta, naplata po vremenu, pretplate, kazne i IoT (MQTT).

| Folder      | Sadržaj                              |
|-------------|--------------------------------------|
| `frontend/` | Flutter klijent                      |
| `backend/`  | Java 21 + Spring Boot REST API       |
| `docker-compose.yml` | MySQL baza (kasnije i backend, MQTT broker, simulator) |

## Baza

    cp .env.example .env      # pa upisati lozinke
    docker compose up -d db

Šema baze se ne komituje kao fajl baze, već kao Flyway migracije u
`backend/src/main/resources/db/migration` (V1__init.sql, V2__..., ...).
