# URL Shortener

A small URL shortener service built with Java 17, Spring Boot, MySQL, Spring Data JPA, validation, and an in-memory Caffeine cache for redirect lookups.

## Features

- `POST /shorten` creates a short URL.
- Repeated generated shorten requests for the same normalized URL return the same short code.
- Optional custom aliases are supported.
- Duplicate aliases return `409 Conflict`.
- Invalid URLs return `400 Bad Request`.
- `GET /{code}` redirects to the original URL with `301 Moved Permanently`.
- Missing short codes return `404 Not Found`.

## Architecture

The project uses a standard layered Spring Boot structure:

- `UrlShortenerController` handles HTTP request and response mapping.
- `UrlShortenerService` owns business rules: URL normalization, idempotency, alias validation, short-code generation, and redirects.
- `UrlMappingRepository` uses Spring Data JPA for persistence.
- `UrlMapping` maps the database table.
- DTOs keep request and response payloads separate from persistence.
- Utility classes isolate Base62 encoding, SHA-256 hashing, and URL normalization.
- Global exception handling returns consistent JSON errors.

## Supported APIs And Methods

### REST APIs

| Method | Endpoint | Purpose | Success Response | Error Responses |
| --- | --- | --- | --- | --- |
| `POST` | `/shorten` | Creates a generated short URL or a custom alias. | `201 Created` with `shortCode`, `shortUrl`, and `originalUrl`. | `400 Bad Request` for invalid URL or alias, `409 Conflict` for duplicate alias. |
| `GET` | `/{code}` | Redirects a short code or alias to the original URL. | `301 Moved Permanently` with `Location` header. | `404 Not Found` when the code does not exist. |

`POST /shorten` accepts this request body:

```json
{
  "url": "https://example.com/articles?id=10",
  "customAlias": "optional-alias"
}
```

`customAlias` is optional. When it is omitted, the service creates or returns the existing generated short URL for the normalized original URL.

### Core Application Methods

| Component | Method | Purpose |
| --- | --- | --- |
| `UrlShortenerController` | `shorten(ShortenRequest request)` | Handles `POST /shorten` and returns the created short URL response. |
| `UrlShortenerController` | `redirect(String code)` | Handles `GET /{code}` and returns a `301` redirect to the original URL. |
| `UrlShortenerService` | `shorten(ShortenRequest request)` | Coordinates URL normalization, idempotency, alias handling, and response creation. |
| `UrlShortenerService` | `getOriginalUrl(String code)` | Looks up the original URL for a short code and caches successful lookups. |
| `UrlMappingRepository` | `findByShortCode(String shortCode)` | Retrieves a mapping by generated code or custom alias. |
| `UrlMappingRepository` | `findByIdempotencyKey(String idempotencyKey)` | Retrieves the existing generated mapping for a normalized URL hash. |
| `UrlNormalizer` | `normalize(String input)` | Validates and canonicalizes incoming HTTP/HTTPS URLs. |
| `UrlHasher` | `sha256Hex(String value)` | Hashes normalized URLs for efficient idempotency checks. |
| `Base62Encoder` | `encode(long value)` | Converts database ids into compact generated code suffixes. |

## Data Model

The service stores URL mappings in one table:

```sql
CREATE TABLE IF NOT EXISTS url_mappings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    original_url VARCHAR(2048) NOT NULL,
    original_url_hash CHAR(64) NOT NULL,
    idempotency_key CHAR(64) NULL,
    short_code VARCHAR(64) NOT NULL,
    custom_alias BIT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_url_mappings_short_code UNIQUE (short_code),
    CONSTRAINT uk_url_mappings_idempotency_key UNIQUE (idempotency_key),
    INDEX idx_url_mappings_original_url_hash (original_url_hash)
);
```

Generated URLs use `idempotency_key`, which is the SHA-256 hash of the normalized URL. Custom aliases leave `idempotency_key` null, so multiple aliases can point to the same original URL.

## Design Decisions

- Generated codes use `u_` plus Base62 of the database id. The database id is unique, so generated short codes are collision-free. The reserved `u_` prefix prevents custom aliases from colliding with generated codes.
- Idempotency is enforced by a unique hash of the normalized URL. This avoids duplicate generated records without indexing the full URL.
- URL normalization lowercases scheme and host, removes fragments, removes default ports, and ensures an empty path becomes `/`.
- Redirect lookups are cached with Caffeine using a configurable TTL. This is simple and fast for a single instance. Redis would be a better fit for multiple application instances.

## Requirements

- Java 17
- Maven 3.9+
- Docker, if using the provided MySQL setup

## Run Locally

Start MySQL:

```bash
docker compose up -d
```

Apply the schema using the MySQL client inside the Docker container:

```bash
docker exec -i url-shortener-mysql mysql -uroot -ppassword url_shortener < src/main/resources/schema.sql
```

Run the app:

```bash
mvn spring-boot:run
```

The service starts at:

```text
http://localhost:8080
```

## Sample Requests

Create a generated short URL:

```bash
curl -i -X POST http://localhost:8080/shorten \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/articles?id=10"}'
```

Sample response:

```json
{
  "shortCode": "u_1",
  "shortUrl": "http://localhost:8080/u_1",
  "originalUrl": "https://example.com/articles?id=10"
}
```

Create a custom alias:

```bash
curl -i -X POST http://localhost:8080/shorten \
  -H 'Content-Type: application/json' \
  -d '{"url":"https://example.com/docs","customAlias":"docs"}'
```

Redirect:

```bash
curl -i http://localhost:8080/docs
```

Expected response:

```text
HTTP/1.1 301
Location: https://example.com/docs
```

Duplicate alias response:

```json
{
  "status": 409,
  "error": "Conflict",
  "message": "Alias already exists: docs",
  "path": "/shorten"
}
```

## Run Tests

```bash
mvn test
```

Tests use H2 in MySQL compatibility mode and cover service behavior, controller responses, URL normalization, Base62 encoding, duplicate aliases, invalid URLs, redirects, missing codes, and idempotency.

## Assumptions And Limitations

- Shortened URLs are immutable after creation.
- No authentication, ownership, analytics, expiry, or rate limiting is included.
- The in-memory cache is per JVM and is cleared on restart.
- Generated short codes expose relative insertion order because they are derived from database ids.
- Schema management is intentionally simple for this exercise. Flyway or Liquibase would be the next production hardening step.
