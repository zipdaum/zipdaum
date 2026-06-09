# Zipdaum

Vue.js와 Spring Boot를 분리해서 개발하는 프로젝트입니다.

## Structure

```text
zipdaum/
├── backend/    # Spring Boot API server
├── frontend/   # Vue 3 + Vite client
└── docker-compose.yml
```

## Backend

```bash
cd backend
./mvnw spring-boot:run
```

## Frontend

```bash
cd frontend
npm install
npm run dev
```

## Database

```bash
docker compose up -d
```
