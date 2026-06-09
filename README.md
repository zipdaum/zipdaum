# Zipdaum

Vue.js와 Spring Boot를 분리해서 개발하는 프로젝트입니다.

## 프로젝트 구조

```text
zipdaum/
├── backend/    # Spring Boot API 서버
├── frontend/   # Vue 3 + Vite 클라이언트
└── docker-compose.yml
```

## 백엔드 실행

```bash
cd backend
./mvnw spring-boot:run
```

## 프론트엔드 실행

```bash
cd frontend
npm install
npm run dev
```

## 데이터베이스 실행

루트 경로에 로컬 환경변수 파일을 생성하고 비밀번호 값을 수정합니다.

```bash
cp .env.example .env
```

```bash
docker compose up -d
```

로컬 프로필로 백엔드를 실행합니다.

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

## 로컬 개발 실행 순서

먼저 MySQL을 실행합니다. `docker/mysql/init/01-schema.sql`의 스키마는 MySQL 볼륨이 처음 생성될 때 자동으로 적용됩니다.

```bash
cp .env.example .env
docker compose up -d
```

백엔드는 로컬 프로필로 실행합니다.

```bash
cd backend
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

다른 터미널에서 프론트엔드를 실행합니다.

```bash
cd frontend
npm install
npm run dev
```

접속 주소는 다음과 같습니다.

```text
Backend API: http://localhost:8080
Frontend:    http://localhost:5173
```

MySQL Workbench에서는 Docker MySQL에 다음 정보로 접속합니다.

```text
Connection Method: Standard TCP/IP
Hostname: 127.0.0.1
Port: 3306
Username: .env의 MYSQL_USER 값
Password: .env의 MYSQL_PASSWORD 값
Default Schema: zipdaum
```

이미 데이터베이스 볼륨이 생성된 뒤 스키마 SQL을 수정했다면 볼륨을 다시 생성해야 합니다. 이 명령은 기존 데이터도 함께 삭제합니다.

```bash
docker compose down -v
docker compose up -d
```
