# Zipdaum

## 프로젝트 구조

```text
zipdaum/
├── backend/    # Spring Boot API 서버
├── frontend/   # Vue 3 + Vite 클라이언트
└── docker-compose.yml
```

## 로컬 서버 실행 순서

### 도커
`docker/mysql/init/01-schema.sql`의 스키마는 MySQL 볼륨이 처음 생성될 때 자동으로 적용됩니다.
이미 데이터베이스 볼륨이 생성된 뒤 스키마 SQL을 수정했다면 볼륨을 다시 생성해야 합니다. 이 명령은 기존 데이터도 함께 삭제합니다.

```bash
cp .env.example .env
docker compose down -v
docker compose up -d
```

### 백엔드
`git switch [브랜치명]` 직후, 더미 파일이 남아있을 수 있어 `./mvnw clean` 실행을 권장합니다.

```bash
cd backend
./mvnw clean
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

### 프론트엔드

```bash
cd frontend
npm install
npm run dev
```

### 서버 접속 주소

```text
Backend API: http://localhost:8080
Frontend:    http://localhost:5173
```

### MySQL Workbench 접속 방법

```text
Connection Method: Standard TCP/IP
Hostname: 127.0.0.1
Port: 3306
Username: .env의 MYSQL_USER 값
Password: .env의 MYSQL_PASSWORD 값
Default Schema: zipdaum
```

## 로컬 공공데이터 CSV

주택 상세 화면의 주변 편의시설 조회는 외부 API를 매 요청마다 호출하지 않고,
`backend/src/main/resources/data`의 UTF-8 CSV 파일을 로딩해 사용합니다.

| 파일 | 용도 | 출처                                                                                                                                                                                          |
| --- | --- |---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `busan_bus_stops.csv` | 버스 정류소 | [부산광역시_버스 정류소 정보(SHP)](https://www.data.go.kr/data/15084251/fileData.do)                                                                                                                    |
| `busan_subway_stations.csv` | 지하철역 | [부산교통공사_도시철도역사정보](https://www.data.go.kr/data/15043686/fileData.do)                                                                                                                         |
| `busan_hospital.csv` | 병원 | [부산광역시_종합병원 현황](https://www.data.go.kr/data/15083386/fileData.do)                                                                                                                           |
| `busan_cctv.csv` | 방범용 CCTV | [부산광역시_방범용 CCTV 정보](https://www.data.go.kr/data/15082060/fileData.do)                                                                                                                       |
| `busan_15min_urban_parks.csv` | 도시공원 | [부산 15분도시 생활권 도시공원 CSV](https://data.busan.go.kr/bdip/opendata/detail.do?publicdatapk=15152993&searchKeyword=%EA%B3%B5%EC%9B%90&searchOption=AND&uuid=26470484-ba91-4904-aeb0-f52d3d6fb519) |
