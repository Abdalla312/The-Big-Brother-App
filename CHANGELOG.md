# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

Nothing yet.

---

## [0.2.1] - 2026-09-08

### Added
- Soft-Delete mechanism – Timestamp-based soft delete `(deleted_at)` for transactions, categories, budgets, recurring transactions, and users with restore functionality and scheduled cleanup job (30-day retention)
- Trash/Restore endpoints – `GET /trash` and `PUT /{id}/restore` for categories, transactions, recurring transactions, and budgets
- Combined category filtering – Support for `type=INCOME|EXPENSE` with `defaultCategories=true|false` query parameters
- Scheduled cleanup – Cron job (`@Scheduled`) to permanently delete soft-deleted entities after 30 days
- New dependencies – Apache Commons Compress 1.26.0, Apache Commons Lang3 3.20.0
- Frontend trash modal – Reusable UI component for viewing/restoring soft-deleted entities

### Changed

- Category API – Replaced string-based type param (`user|default|all`) with TransactionType enum + explicit defaultCategories boolean
- SpringDoc OpenAPI – Upgraded from 2.8.5 → 2.8.14
- Flyway migrations – Added V12 migration adding `deleted_at` columns to all entity tables
- Docker Compose – Removed Mailhog (PostgreSQL only for local dev)
- BaseIntegrationTest – Added `performPut()` helper for REST endpoint testing

### Fixed

- BudgetController – Fixed @PageableDefault parameter (`page=20` → `size=20`)
- CategoryService – prevent restore of system-default categories (null user_id) with 403

## [0.2.0] — 2026-09-03

### Added
- Recurring transactions module with daily scheduler, pause/resume, and frontend UI
- `RecurringTransactionProcessor` extracting recurring-entry scheduling logic
- Password reset flow (`forgot-password` / `reset-password`) via `EmailVerificationToken`
- `token_type` column on `EmailVerificationToken` for password-reset tokens
- Password complexity validation (`@ValidPassword`)
- Transaction CSV export endpoint plus frontend download action and API docs
- HTML/CSS/JS development client (auth, transactions, categories, budgets, reports)
- Persistent collapsible sidebar in the frontend
- Thymeleaf email templating system
- Docker Compose environment (PostgreSQL) for local dev
- Configurable CORS origins via `CORS_ALLOWED_ORIGINS` env var
- Single-flight token refresh and safe retry in the frontend API client
- AWS ECS Fargate deployment pipeline with ECR and task definitions

### Changed
- Migrated integration tests from H2 to Testcontainers PostgreSQL
- Frontend served from the backend container to fix mixed content
- Dockerfile enhancements (Maven dependency caching, Railway compatibility)
- Aligned JPA column metadata with Flyway schema for `ddl-auto=validate`
- Consolidated email verification unique constraint into V7 migration
- Updated README (password reset, recurring transactions, CSV export, infrastructure)

### Fixed
- Category deletion FK violation when referenced by recurring transactions
- Recurring transaction isolation (`REQUIRES_NEW`) and pagination offset drift
- `inMonth` `DateTimeException` on malformed date input
- Filter-chain crash on invalid JWT
- Report budget comparison including budgeted and unbudgeted categories
- Empty-string handling (`??` → `||`)
- Password reset token expiry configuration
- Registration flow and rate limiting
- Frontend auth issues (token refresh, API client retries)

---

## [0.1.0] — 2026-07-25

### Added
- User authentication with JWT (15-min access token and refresh token rotation)
- Transaction management (CRUD + filtering + pagination)
- Category system (system-default and user-custom with colors/icons)
- Budget tracking with spending calculation (spent, remaining, percent used)
- Reports & analytics (monthly summary, category breakdown, payment method breakdown, budget comparison, trends)
- Rate limiting (token-bucket algorithm, configurable capacity/refill)
- Refresh token lifecycle management (rotation, revocation, bulk revoke)
- Email verification flow (register, resend, change email)
- OpenAPI 3.1 spec bundled in the backend
- Docker multi-stage build
- Flyway database migrations (9 migrations)
- Comprehensive testing (152 tests, 0 failures)
- Architecture documentation (ADRs, ER diagram, system diagram)

---

[0.2.0]: https://github.com/Abdalla312/The-Big_Brother-App/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Abdalla312/The-Big_Brother-App/releases/tag/v0.1.0