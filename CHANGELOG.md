# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

Nothing yet.

---

## [0.2.0] — 2026-09-03

### Added
- Recurring transactions module with daily scheduler, pause/resume, and frontend UI
- `RecurringTransactionProcessor` extracting recurring-entry scheduling logic
- Password reset flow (`forgot-password` / `reset-password`) via `EmailVerificationToken`
- `token_type` column on `EmailVerificationToken` for password-reset tokens
- Password complexity validation (`@ValidPassword`)
- Transaction CSV export endpoint plus frontend download action and API docs
- HTML/CSS/JS development client (auth, transactions, categories, budgets, reports)
- Persistent collapsible sidebar in frontend
- Thymeleaf email templating system
- Docker Compose environment (PostgreSQL + Mailhog) for local dev
- Configurable CORS origins via `CORS_ALLOWED_ORIGINS` env var
- Single-flight token refresh and safe retry in frontend API client
- AWS ECS Fargate deployment pipeline with ECR and task definitions

### Changed
- Migrated integration tests from H2 to Testcontainers PostgreSQL
- Frontend served from backend container to fix mixed content
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
- User authentication with JWT (15-min access token + refresh token rotation)
- Transaction management (CRUD + filtering + pagination)
- Category system (system-default + user-custom with colors/icons)
- Budget tracking with spending calculation (spent, remaining, percent used)
- Reports & analytics (monthly summary, category breakdown, payment method breakdown, budget comparison, trends)
- Rate limiting (token-bucket algorithm, configurable capacity/refill)
- Refresh token lifecycle management (rotation, revocation, bulk revoke)
- Email verification flow (register, resend, change email)
- OpenAPI 3.1 spec bundled in backend
- Docker multi-stage build
- Flyway database migrations (9 migrations)
- Comprehensive testing (152 tests, 0 failures)
- Architecture documentation (ADRs, ER diagram, system diagram)

---

[0.2.0]: https://github.com/Abdalla312/The-Big_Brother-App/compare/v0.1.0...v0.2.0
[0.1.0]: https://github.com/Abdalla312/The-Big_Brother-App/releases/tag/v0.1.0