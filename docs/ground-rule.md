# 프로젝트 그라운드 룰

# Ground Rule

## 1. 기본 원칙

- 코드는 팀원이 읽기 쉽게 작성한다.
- 임시 코드, 테스트용 주석, 사용하지 않는 코드는 커밋하지 않는다.
- 기능 구현 전 API 명세, ERD, 화면 흐름을 먼저 확인한다.
- 하나의 메서드는 하나의 책임만 가지도록 작성한다.
- 중복 로직은 공통 메서드 또는 유틸 클래스로 분리한다.
- Entity/Domain 객체를 API 응답으로 직접 반환하지 않고 DTO를 사용한다.

---

## 2. 패키지 구조

기능 단위로 패키지를 분리한다.

```
com.ssafy.zipdaum
├── auth
├── user
├── property
├── favorite
├── condition
├── notification
├── report
└── global
```

각 기능 패키지는 필요한 경우 아래 구조를 따른다.

```
controller
service
mapper
dto
domain
```

공통 설정, 예외 처리, 공통 응답 객체는 `global` 패키지에 둔다.

```
global
├── config
├── exception
├── response
└── util
```

---

## 3. 클래스 네이밍 규칙

### Controller

```
기능명 + Controller
```

예시:

```java
UserController
PropertyController
FavoriteRegionController
NotificationController
```

### Service

인터페이스와 구현체를 분리한다.

```java
UserService
UserServiceImpl

PropertyService
PropertyServiceImpl
```

### Mapper

MyBatis Mapper는 다음 형식으로 작성한다.

```java
UserMapper
PropertyMapper
FavoritePropertyMapper
ConditionItemMapper
```

### DTO

요청과 응답 DTO를 구분한다.

```java
LoginRequest
LoginResponse

PropertySearchRequest
PropertySearchResponse

PropertyDetailResponse
ConditionSaveRequest
NotificationResponse
```

### Domain

DB 테이블과 매핑되는 객체는 테이블명 기준으로 작성한다.

```java
User
Property
SaleDeal
RentDeal
FavoriteProperty
FavoriteRegion
ConditionItem
ConditionType
Notification
```

---

## 4. 메서드 네이밍 규칙

Controller는 HTTP 요청 처리 의미가 드러나도록 작성한다.

```java
login()
signup()
getUserInfo()
searchProperties()
getPropertyDetail()
addFavoriteProperty()
deleteFavoriteProperty()
```

Service는 비즈니스 행위 중심으로 작성한다.

```java
searchProperties()
findPropertyDetail()
saveFavoriteProperty()
removeFavoriteProperty()
saveConditionItems()
calculateMatchScore()
```

Mapper는 SQL 동작이 드러나도록 작성한다.

```java
selectUserByEmail()
selectPropertyById()
selectProperties()
insertFavoriteProperty()
deleteFavoriteProperty()
selectConditionItemsByUserId()
```

---

## 5. DB 네이밍 규칙

- 테이블명과 컬럼명은 `snake_case`를 사용한다.
- 테이블명은 단수형으로 작성한다.
- PK는 `id`로 통일한다.
- FK는 `테이블명_id` 형식으로 작성한다.
- 생성일, 수정일은 `created_at`, `updated_at`으로 통일한다.

예시:

```
user
property
sale_deal
rent_deal
favorite_property
favorite_region
condition_item
condition_type
notification
```

컬럼 예시:

```
user_id
property_id
condition_type_id
created_at
updated_at
```

---

## 6. MyBatis 작성 규칙

- SQL 키워드는 대문자로 작성한다.
- `SELECT *` 사용을 지양하고 필요한 컬럼을 명시한다.
- 조건 검색은 `<where>`, `<if>`, `<foreach>`를 활용한다.
- 단순 조회는 `resultType`, JOIN 또는 복잡한 매핑은 `resultMap`을 사용한다.
- 컬럼명이 충돌할 경우 alias를 명확히 지정한다.

예시:

```sql
SELECT
    p.id,
    p.property_type,
    p.name,
    p.sgg_cd,
    p.umd_nm,
    p.jibun
FROM property p
WHERE p.sgg_cd = #{sggCd}
```

---

## 7. API 응답 규칙

성공 응답과 실패 응답 형식을 통일한다.

### 성공 응답

```json
{
  "success": true,
  "data": {}
}
```

### 실패 응답

```json
{
  "success": false,
  "code": "PROPERTY_NOT_FOUND",
  "message": "주택 정보를 찾을 수 없습니다."
}
```

---

## 8. 예외 처리 규칙

공통 예외 구조를 사용한다.

```
BusinessException
ErrorCode
GlobalExceptionHandler
```

ErrorCode 예시:

```java
USER_NOT_FOUND
INVALID_PASSWORD
DUPLICATED_EMAIL
PROPERTY_NOT_FOUND
FAVORITE_ALREADY_EXISTS
UNAUTHORIZED
EXTERNAL_API_ERROR
```

Controller나 Service에서 문자열로 직접 에러 응답을 만들지 않는다.

---

## 10. 비회원 / 회원 기능 구분

### 비회원 가능 기능

- 실거래가 검색
- 검색 결과 조회
- 주택 상세 조회
- 주변 시설 조회

### 회원 전용 기능

- 맞춤 조건 설정
- 맞춤 적합도 조회
- 관심 지역 관리
- 관심 주택 관리
- 알림 조회
- 마이페이지

비회원이 회원 전용 기능에 접근하면 로그인/회원가입 유도 메시지를 표시한다.

```
회원가입을 하면 예산, 면적, 선호지역, 생활 편의 조건을 기준으로
내 조건에 맞는 주택 정보를 확인할 수 있습니다.
```

---

## 11. 민감 정보 관리

다음 정보는 Git에 올리지 않는다.

```
DB 비밀번호
JWT Secret
공공데이터 API Key
지도 API Key
```

민감 정보는 환경 변수 또는 별도 설정 파일로 관리한다.

```
spring.datasource.password=${DB_PASSWORD}
jwt.secret=${JWT_SECRET}
openapi.service-key=${OPEN_API_SERVICE_KEY}
map.api-key=${MAP_API_KEY}
```

---

## 12. 개발 전 확인 사항

기능 개발 전 아래 내용을 먼저 확인한다.

- API 명세와 URL이 맞는지
- ERD 기준으로 필요한 테이블과 컬럼이 있는지
- 화면에서 필요한 응답 데이터가 무엇인지
- 비회원 접근 가능 기능인지 회원 전용 기능인지
- 예외 상황을 어떻게 처리할지