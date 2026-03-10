# Local Development Setup

This document describes how to set up the local development environment for the **quemfaz** project.

## Prerequisites

- [Docker](https://www.docker.com/) and [Docker Compose](https://docs.docker.com/compose/)
- [JDK 17+](https://adoptium.net/)

## Infrastructure

The project uses PostgreSQL as its primary database. A `docker-compose.local.yml` file is provided to start a local PostgreSQL instance.

### Starting PostgreSQL

Run the following command at the project root:

```bash
docker compose -f docker-compose.local.yml up -d
```

This will start a PostgreSQL instance on `localhost:5432` with:
- **Database:** `quemfaz`
- **User:** `quemfaz`
- **Password:** `quemfaz`

## Running the Server

You can run the server directly from your IDE by executing the `main` function in `server/src/main/kotlin/com/fugisawa/quemfaz/Application.kt`.

Alternatively, use Gradle:

```bash
./gradlew :server:run
```

The server starts on port `8080` by default.

## Configuration

The application uses HOCON configuration files located in `server/src/main/resources`.

- `application.conf`: Base configuration with local defaults.
- `application-local.conf`: Optional local overrides (git-ignored).

### Environment Variables

You can override configuration using environment variables:

| Variable | Description | Default |
|----------|-------------|---------|
| `APP_ENV` | Application environment (`local`, `dev`, `sandbox`, `production`) | `local` |
| `DB_HOST` | Database host | `localhost` |
| `DB_PORT` | Database port | `5432` |
| `DB_NAME` | Database name | `quemfaz` |
| `DB_USER` | Database user | `quemfaz` |
| `DB_PASS` | Database password | `quemfaz` |
| `SMS_PROVIDER` | SMS provider type (`FAKE`, `AWS`) | `FAKE` |

## SMS & OTP

For local development, the `SMS_PROVIDER` is set to `FAKE` by default.

- **Fake SMS Sender:** Instead of sending real SMS messages, it logs the message content and recipient to the server console. This allows you to "receive" OTP codes during development without an external service.
- **OTP Generation:** Uses a random 6-digit code generator by default.

## Future AWS Integration

The infrastructure is designed to be pluggable. To use AWS SNS for SMS in the future:
1. Set `SMS_PROVIDER=AWS`.
2. Provide necessary AWS configuration (region, sender ID) in the corresponding `application-{env}.conf` or via environment variables.
3. The `AwsSmsSender` class is currently a placeholder and will be implemented in a future step.
