# Schema-driven Spring + React generator (stocks & ETFs)

Design a **JSON schema** once and get a Spring MVC REST API **and** a react-admin UI whose list view is a
**TanStack Table** with server-side paging, sorting, filtering and search. The demo domain is **global stock
trading** (instrument types `STOCK` and `ETF`) with **12,000 instruments** and **15,000 trades** of synthetic data.

```
schemas/*.json ──► generator (Java + FreeMarker) ──┬─► Spring: @Entity, Repository, @RestController
                                                   └─► React : react-admin List (TanStack Table), Create/Edit
```

## Is there an existing framework?
Closest options: **JHipster** (entity JDL -> Spring Boot + React, full-stack but heavy/opinionated),
**OpenAPI Generator** (spec -> Spring/TypeScript clients, no table views), **Spring Data REST + react-admin**
(no code generation of views). This project is a small, hackable generator tailored to your schema + TanStack idea.

## Layout
| Path | Purpose |
|---|---|
| `schemas/` | Your entity schemas (`instrument.json`, `trade.json`) |
| `generator/` | Gradle module: schema model, validator, FreeMarker templates, CLI |
| `backend/` | Spring Boot 3 app (H2) + `SimpleRestSupport` + `DataSeeder` (12k / 15k rows) |
| `frontend/` | Vite + React + react-admin + `TanStackTable` component + Playwright tests |
| `.github/workflows/ci.yml` | Build, unit tests, Playwright; reports kept as artifacts for **1 day** |

## Schema format
```json
{ "name": "Instrument", "plural": "instruments", "label": "Instruments", "description": "...",
  "fields": [
    { "name": "id", "type": "long", "primaryKey": true, "generated": true },
    { "name": "symbol", "type": "string", "required": true, "maxLength": 16, "searchable": true, "filterable": true },
    { "name": "instrumentType", "type": "enum", "values": ["STOCK", "ETF"], "filterable": true } ] }
```
Types: `string long integer decimal boolean date datetime enum`. Field flags: `primaryKey, generated, required,
maxLength, precision, scale, sortable (default true), filterable, searchable, listVisible (default true), label, description`.
The primary key must be named `id`.

## Run locally (Java 21, Gradle 8.10+, Node 20)
```bash
gradle wrapper                         # optional: creates ./gradlew
gradle :backend:generateCode           # schemas -> backend/build/generated + frontend/src/generated
gradle :backend:bootRun                # API on :8080 (seeds data before starting)
cd frontend && npm install && npm run dev   # UI on http://127.0.0.1:5173
```
Add a new entity: drop `schemas/foo.json`, run `generateCode`; a new menu entry and list appear.

## Tests
* `gradle test` - generator unit tests + backend API test.
* `cd frontend && npx playwright test` - starts backend + frontend itself; report in `frontend/playwright-report/`
  (set `GRADLE_CMD=./gradlew` if you use the wrapper).

## CI artifacts (retention 1 day)
`playwright-report` (HTML), `playwright-test-results` (JUnit XML, JSON, screenshots, videos on failure),
`gradle-test-reports`. Download them from the workflow run's **Artifacts** section.

All data is synthetic; names, ISINs and prices are fictional.
