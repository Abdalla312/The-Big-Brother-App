# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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

## [Unreleased]

### Added
- Architecture Decisions section in README
- Mermaid ER and architecture diagrams in README
- AWS ECS Fargate deployment section in README
- Screenshots/Demo placeholder section in README
- `.env.example` template file
- `docker-compose.yml` with PostgreSQL 17 + Mailhog
- Apache 2.0 license