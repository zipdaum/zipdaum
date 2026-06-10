# AGENTS.md

## Project Overview

집다움(ZipDaum)은 주택 실거래가 조회, 관심지역 관리, 맞춤 추천 및 알림 기능을 제공하는 웹 서비스이다.

Frontend는 Vue3, Backend는 Spring Boot + MyBatis를 사용한다.

---

## Tech Stack

### Backend

- Java 21
- Spring Boot 3.5.14
- MyBatis 3.0.5
- MySQL 8.4 LTS
- H2 (Local/Test)

### Frontend

- Vue 3
- Vite
- Axios

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

작업 전 반드시 아래 문서를 확인한다.

1. docs/requirements/기획서.md
2. docs/erd.png
3. docs/api-spec.md
4. ground-rule.md

문서와 구현 내용이 충돌할 경우 문서를 우선한다.

---

## Backend Architecture

```text
Controller
  ↓
Service
  ↓
Mapper(MyBatis)
  ↓
Database
```

### Rules

- Controller는 요청/응답 처리만 담당한다.
- 비즈니스 로직은 Service에 작성한다.
- 데이터 접근은 Mapper를 사용한다.
- DTO를 사용한다.
- API 응답으로 Entity를 직접 반환하지 않는다.
- Lombok 사용 가능
- ResponseEntity 사용
- 예외 처리는 GlobalExceptionHandler 사용

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

- Composition API 사용
- script setup 사용
- axios 사용
- 공통 UI는 components로 분리
- 페이지 단위는 views 사용

---

## AI Coding Rules

코드 생성 시 반드시 다음 원칙을 따른다.

- 기존 프로젝트 구조를 유지한다.
- 필요한 파일만 생성한다.
- 불필요한 예제 코드 생성 금지
- 과도한 추상화 금지
- 요구사항에 없는 기능 추가 금지
- docs/erd.md 기반으로 테이블 관계를 해석한다.
- docs/api-spec.md 기반으로 API를 구현한다.
- 구현 전 기존 코드를 우선 분석한다.
- 기존 코드 스타일을 우선 따른다.

---

## Development Goal

프로젝트 규모에 맞게 단순하고 명확한 구조를 우선한다.

관통 프로젝트 규모에 적합한 단순하고 유지보수 가능한 구조를 우선한다.
