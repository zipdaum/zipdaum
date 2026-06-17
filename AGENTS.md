## Project Overview

ZipDaum is a web service that provides housing transaction price lookup, favorite area management, personalized recommendations, and notification features.

The frontend is built with Vue 3, and the backend uses Spring Boot and MyBatis.

---

## Tech Stack

### Backend

* Java 21
* Spring Boot 3.5.14
* MyBatis 3.0.5
* MySQL 8.4 LTS
* H2 (Local/Test)

### Frontend

* Vue 3
* Vite
* Axios

---

## Repository Structure

```text
zipdaum
├── frontend
├── backend
├── docs
└── AGENTS.md
```

---

## Documentation Priority

Before starting any task, review the following documents:

1. docs/requirements/requirements.md
2. docs/erd.png
3. docs/api-spec.md
4. docs/ground-rule.md

If there is any conflict between the implementation and the documentation, follow the documentation.

---

## Backend Architecture

```text
Controller
  ↓
Service
  ↓
Mapper (MyBatis)
  ↓
Database
```

### Rules

* Controllers handle request and response processing only.
* Business logic must be implemented in Services.
* Data access must be handled through Mappers.
* Use DTOs for data transfer.
* Never expose Entities directly through APIs.
* Lombok is allowed.
* Use ResponseEntity for API responses.
* Handle exceptions through GlobalExceptionHandler.

---

## Frontend Architecture

```text
views
components
api
router
stores
assets
```

### Rules

* Use the Composition API.
* Use `<script setup>`.
* Use Axios for HTTP communication.
* Extract reusable UI components into `components`.
* Use `views` for page-level components.

---

## AI Coding Rules

When generating code, always follow these principles:

* Preserve the existing project structure.
* Create only the files that are necessary.
* Do not generate unnecessary example code.
* Avoid over-engineering and excessive abstraction.
* Do not add features that are not part of the requirements.
* Interpret table relationships based on `docs/erd.md`.
* Implement APIs according to `docs/api-spec.md`.
* Analyze existing code before making changes.
* Follow the existing coding style whenever possible.

---

## Development Goal

Prioritize a simple and clear architecture that matches the project's scope.

Favor maintainable and practical solutions over complex designs.

Avoid unnecessary architectural patterns or abstractions that do not provide clear value for this project.
