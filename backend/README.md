# WPW PIM Backend

Product Information Management (PIM) system for WPW — REST API backend for managing a product catalog of industrial tools and machinery.

## Stack

- **Java 17**, **Spring Boot 3.4.3**, **Maven**
- **PostgreSQL** + Flyway migrations (13 versions)
- **Spring Security** — JWT (admin) + API Key (dealers)
- **Spring Data JPA** + Caffeine cache
- **Apache POI** (Excel import), **OpenCSV** (CSV export)
- **SpringDoc / Swagger UI**

## Features

- Multi-level product catalog: Sections → Categories → Product Groups → Products
- Multi-language product translations with PostgreSQL full-text search
- Bulk import from Excel with pre-import validation and detailed reports
- Export to CSV / XLSX / XML
- Dealer/partner portal: custom SKU mapping, tiered price lists, API key auth
- Role-based access control (Admin / Dealer) with privilege system
- Content audit log
- JSON-LD structured data for SEO

## Getting Started

**Prerequisites:** Java 17, PostgreSQL running on `localhost:5432`

```bash
# Build
./mvnw clean package

# Run
./mvnw spring-boot:run
```

**Docker:**

```bash
docker build -t wpw-pim .
docker run -p 8080:8080 \
  -e SPRING_DATASOURCE_URL=jdbc:postgresql://postgres:5432/pim_db \
  -e SPRING_DATASOURCE_USERNAME=pim_user \
  -e SPRING_DATASOURCE_PASSWORD=<password> \
  wpw-pim
```

## Configuration

Key settings in `src/main/resources/application.properties`:

| Property                     | Default                                   |
| ---------------------------- | ----------------------------------------- |
| `spring.datasource.url`      | `jdbc:postgresql://localhost:5432/pim_db` |
| `spring.datasource.username` | `pim_user`                                |
| `jwt.secret`                 | change in production                      |
| `jwt.expiration-ms`          | `86400000` (24h)                          |
| `pim.media.base-path`        | `/media/products`                         |

## API Overview

| Prefix                    | Access           | Description                      |
| ------------------------- | ---------------- | -------------------------------- |
| `POST /api/v1/auth/login` | Public           | Login, returns JWT               |
| `GET /api/v1/products`    | Public           | Product listing with filters     |
| `GET /api/v1/search`      | Public           | Full-text search                 |
| `GET /api/v1/export`      | Public           | Export (CSV / XLSX / XML)        |
| `/api/v1/admin/**`        | JWT (Admin)      | Catalog, import, user management |
| `/api/v1/dealer/**`       | API Key (Dealer) | SKU mapping, price lists         |

Interactive docs: `http://localhost:8080/swagger-ui.html`

## Project Structure

```
src/main/java/com/wpw/pim/
├── auth/          # JWT auth, users, roles, privileges
├── config/        # Security, cache, OpenAPI config
├── domain/        # JPA entities (product, catalog, dealer, pricing, media, audit)
├── repository/    # Spring Data repositories
├── security/      # API key authentication for dealers
├── service/       # Business logic (product, catalog, import, export, search, dealer)
└── web/           # REST controllers + DTOs
src/main/resources/db/migration/   # Flyway SQL migrations (V1–V13)
```
