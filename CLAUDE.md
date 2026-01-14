# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Cremer-Service is a Spring Boot 3.3.5 REST API with WebSocket support for managing manufacturing production orders. It connects to MySQL and provides real-time updates via STOMP WebSocket.

## Build and Run Commands

```bash
# Build the project
./mvnw clean package

# Run the application (default port 8600)
./mvnw spring-boot:run

# Run with development profile (MySQL on port 3309)
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Run with production profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod

# Run tests
./mvnw test

# Run a single test class
./mvnw test -Dtest=CremerApplicationTests
```

## Architecture

### Package Structure
- `com.rnp.cremer.controller` - REST controllers with `/api/v1/` prefix
- `com.rnp.cremer.service` - Business logic services
- `com.rnp.cremer.repository` - Spring Data JPA repositories
- `com.rnp.cremer.model` - JPA entities
- `com.rnp.cremer.dto` - Data transfer objects
- `com.rnp.cremer.config` - Configuration (Security, WebSocket)
- `com.rnp.cremer.exception` - Custom exceptions and global handler
- `com.rnp.cremer.mappers` - Entity-DTO mappers

### Core Domain: Order Lifecycle
Orders follow this state machine:
```
CREADA → EN_PROCESO ↔ PAUSADA
              ↓
   finalizar(acumula=false) → FINALIZADA
   finalizar(acumula=true)  → ESPERA_MANUAL → PROCESO_MANUAL → FINALIZADA
```

Key entities:
- `Order` - Main production order entity
- `Pause` - Tracks pause periods within orders
- `Metricas` - OEE and performance metrics (calculated once when order leaves EN_PROCESO)
- `Acumula` - Manual accumulation process data
- `BottleCounter` - Real-time bottle counting

### WebSocket Configuration
- STOMP endpoint: `/ws` (with SockJS fallback)
- Message broker prefixes: `/topic`, `/queue`
- Application destination prefix: `/app`
- Real-time notifications sent to `/topic/orders` and `/topic/orders/{id}`

### API Documentation
Swagger UI available at `/swagger-ui.html` when running.

## Key Technical Details

- **Java Version**: 17
- **Database**: MySQL 8 with HikariCP connection pool
- **Timezone**: Configured to `Europe/Madrid` (application-wide)
- **Security**: CORS enabled for all origins, CSRF disabled (development config)
- **Export formats**: Excel (Apache POI) and PDF (iTextPDF, OpenPDF)

## Configuration Profiles

- `application.properties` - Common settings (port 8600, logging, Swagger)
- `application-dev.properties` - Development (MySQL on localhost:3309)
- `application-prod.properties` - Production settings
