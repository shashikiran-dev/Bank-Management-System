# Bank Management System (Java + Spring Boot)

![Java](https://img.shields.io/badge/Java-21-blue)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.x-green)
![License](https://img.shields.io/badge/License-MIT-yellow)

A modular, backend-focused **Bank Management System** built incrementally to demonstrate **clean domain modeling, data-structure–driven workflows, and layered backend architecture** using Java and Spring Boot.

The project is intentionally designed as a **learning-first but industry-aligned system**, emphasizing correctness, separation of concerns, and extensibility.

---

## Phase 1 – Core Banking Engine (Pure Java, OOP)

**Module:** `core-engine`

- Designed a **domain-driven banking core** using pure Java
- Implemented multiple account types using **inheritance and polymorphism**
- Encapsulated critical business rules:
  - balance validation
  - frozen account enforcement
- Used **Factory Pattern** for account creation
- Built immutable `Transaction` model with timestamps
- Defined **custom domain exceptions** for invariant enforcement
- Core engine is completely **framework-agnostic**
  - no Spring
  - no database
  - no external dependencies

**Focus:** Object modeling, correctness, and domain integrity

---

## Phase 2 – Service Layer & Data Structures

- Introduced a service contract via `BankOperations`
- Enforced **interface-driven design** for loose coupling
- Implemented algorithmic workflows:
  - **Undo transactions using Stack (LIFO)**
  - **Loan request handling using Queue (FIFO)**
- Maintained in-memory transaction history for auditability
- Ensured strict separation between:
  - domain logic
  - service orchestration
  - data structure responsibilities

**Focus:** Algorithms, data structures, and service contracts

---

## Phase 3 – Spring Boot REST API

**Module:** `bank-api`

- Exposed core banking operations via **RESTful APIs**
- Implemented transaction workflows:
  - deposit
  - withdraw
  - transfer
- Integrated:
  - stack-based undo
  - queue-based loan processing
- Added **global exception handling** for domain errors
- Integrated **Swagger / OpenAPI** for API documentation
- Platform stabilized on:
  - Java 21 (LTS)
  - Spring Boot 3.3.x

**Focus:** Backend architecture, API design, developer experience

---

## Phase 4 – Persistence Layer (Database-Backed)

### What is implemented

- Added relational persistence using **Spring Data JPA**
- Designed schema and entities for:
  - Accounts
  - Transactions (append-only ledger)
  - Loan requests
- Introduced **Flyway-compatible migrations**
- Implemented repositories for all aggregates
- Introduced explicit **Domain ↔ Entity mappers**
- Controllers expose **DTO-style views**, not JPA entities
- Persistence tested using in-memory database

### Architectural guarantees

- `core-engine` has **zero dependency** on Spring or JPA
- `bank-api` adapts persistence to domain via mappers
- Domain rules are enforced **before** persistence
- Module boundaries are compiler-enforced

### Current status

- Core banking flows (create, deposit, withdraw, transfer) are fully DB-backed
- Loan processing and undo are **intentionally staged** for further hardening
  (e.g., durability, concurrency, restart safety)

---

## Technology Stack

- Java 21
- Spring Boot 3.3.x
- Maven (with Maven Wrapper)
- Spring Data JPA
- Flyway
- Swagger / OpenAPI
- Java Collections (Stack, Queue) for algorithmic workflows

---

## Project Structure

bank-management-system/
├── core-engine/ # Pure Java domain & business logic
├── bank-api/ # Spring Boot REST API
└── README.md

---

## Upcoming Phases

- **Phase 5 – Security**

  - Spring Security
  - JWT authentication
  - Role-based access control

- **Phase 6 – AI Integration**
  - Fraud detection
  - Risk scoring
  - ML-powered decision support

> `application.properties` is ignored by Git to prevent credential leaks.

---

## How to Run

```bash
# Build core engine
cd core-engine
mvn clean install

# Run Spring Boot API
cd ../bank-api
mvn spring-boot:run


```

---

## Configuration & Git Hygiene

This project follows standard Git and configuration best practices to ensure
security and clean version control.

### Environment Variables

- Database credentials and environment-specific configuration are **not hardcoded**
- Sensitive values are expected to be supplied via environment variables
- Example configuration is referenced in `application.properties` using placeholders

```properties
spring.datasource.url=${DB_URL}
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}
```
