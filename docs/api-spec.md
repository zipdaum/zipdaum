# API 명세서

Notion에서 내보낸 API 명세 CSV를 Markdown 표로 정리한 문서이다.

## 전체 API 목록

| 기능 분류 | 기능 | Method | URL |
| --- | --- | --- | --- |
| 회원 | 로그인 | POST | `/auth/login` |
| 회원 | 로그아웃 | POST | `/auth/logout` |
| 회원 | 회원가입 | POST | `/users` |
| 회원 | 회원 정보 조회 | GET | `/users/info` |
| 회원 | 회원 정보 수정 | PATCH | `/users/info` |
| 회원 | 회원 탈퇴 | DELETE | `/users/info` |
| 실거래가 | 주택 실거래가 검색 | GET | `/properties` |
| 사용자 맞춤 | 사용자 맞춤 주택 추천 목록 조회 | GET | `/properties/recommendations` |
| 사용자 맞춤 | AI 주택 비교 | POST | `/properties/compare/ai` |
| 실거래가 | 실거래가 저장 | POST | `/properties` |
| 실거래가 | 주택 상세 조회 | GET | `/properties/{propertyId}` |
| 실거래가 | 거래 이력 조회 | GET | `/properties/{propertyId}/histories` |
| 사용자 맞춤 | 주택 맞춤 조건 적합도 조회 | GET | `/properties/{propertyId}/recommendation-score` |
| 사용자 맞춤 | 사용자 맞춤 지역 조건 후보 검색 | GET | `/users/info/preferences/regions/candidates` |
| 사용자 맞춤 | 주택 상세 화면 행동 로그 저장 | POST | `/properties/{propertyId}/interactions` |
| 사용자 맞춤 | 사용자 맞춤 조건 전체 저장(조건 조합) | PUT | `/users/info/preferences` |
| 사용자 맞춤 | 사용자 맞춤 조건 해제(조건 조합) | DELETE | `/users/info/preferences` |
| 사용자 맞춤 | 관심 지역 전체 조회 | GET | `/users/info/regions` |
| 사용자 맞춤 | 관심 지역 등록 | POST | `/users/info/regions` |
| 사용자 맞춤 | 관심 지역 해제 | DELETE | `/users/info/regions` |
| 사용자 맞춤 | 관심 주택 전체 조회 | GET | `/users/info/properties` |
| 사용자 맞춤 | 관심 주택 등록 | POST | `/users/info/properties` |
| 사용자 맞춤 | 관심 주택 해제 | DELETE | `/users/info/properties` |
| 사용자 맞춤 | 최근 본 주택 조회 | GET | `/users/info/recent-properties` |
| 실거래가 | 주택 주변 편의시설 조회 | GET | `/properties/{propertyId}/surroundings` |

## 회원 기능

| 기능 | Method | URL |
| --- | --- | --- |
| 로그인 | POST | `/auth/login` |
| 로그아웃 | POST | `/auth/logout` |
| 회원가입 | POST | `/users` |
| 회원 정보 조회 | GET | `/users/info` |
| 회원 정보 수정 | PATCH | `/users/info` |
| 회원 탈퇴 | DELETE | `/users/info` |

### 회원 탈퇴

로그인한 사용자가 현재 이름과 확인 문구를 입력하면 탈퇴를 신청한다.

요청 예시:

```json
{
  "name": "홍길동",
  "confirmationText": "delete/홍길동"
}
```

성공 시 계정은 즉시 비활성화되며, 회원 정보는 탈퇴 신청 시점으로부터 2주 뒤 물리 삭제된다.

## 실거래가 조회 및 주택/거래 상세 조회

| 기능 | Method | URL |
| --- | --- | --- |
| 주택 실거래가 검색 | GET | `/properties` |
| 사용자 맞춤 주택 추천 목록 조회 | GET | `/properties/recommendations` |
| AI 주택 비교 | POST | `/properties/compare/ai` |
| 실거래가 저장 | POST | `/properties` |
| 주택 상세 조회 | GET | `/properties/{propertyId}` |
| 거래 이력 조회 | GET | `/properties/{propertyId}/histories` |
| 주택 주변 편의시설 조회 | GET | `/properties/{propertyId}/surroundings` |
| 주택 맞춤 조건 적합도 조회 | GET | `/properties/{propertyId}/recommendation-score` |
| 주택 상세 화면 행동 로그 저장 | POST | `/properties/{propertyId}/interactions` |

### 주택 맞춤 조건 적합도 점수 기준

추천 점수는 사용자가 등록한 맞춤 조건을 기준으로 산정한다.

- `recommendationStatus`가 `EVALUATED`이면 점수 산정이 완료된 상태이다.
- `recommendationStatus`가 `NO_EVALUABLE_CONDITION`이면 평가 가능한 맞춤 조건이 없어 `score`는 `null`이다.
- `score`는 0~100 범위의 정수이다.
- 조건별 평가 결과는 `conditions`에 포함된다.
- 시설 조건 값이 `false`인 경우 추천 점수 평가 대상에서 제외한다.
- 가격 조건은 선호 금액 이하이면 100점, 선호 금액의 110% 이하이면 70점으로 평가한다.
- 면적 조건은 선호 면적 이상이면 100점, 선호 면적의 90% 이상이면 70점으로 평가한다.
- 건축연도 조건은 선호 연도 이상이면 100점, 선호 연도보다 5년 이내로 오래된 경우 70점으로 평가한다.
- 지역 조건은 `region` 테이블 기준으로 선호 시군구가 주택 읍면동을 포함하거나 선호 읍면동과 주택 읍면동이 일치하면 100점으로 평가한다.
- 시설 조건은 시설별 기준 반경 내 해당 시설이 1개 이상 있으면 100점으로 평가한다.
  - 버스, CCTV: 500m
  - 지하철역, 공원: 1000m
  - 병원: 1500m
- 사용자 맞춤 주택 추천 목록에서는 상세 화면 행동 로그의 조회 횟수에 따라 5점씩 가산하며 최대 15점까지 반영한다.
- 사용자 맞춤 주택 추천 목록에서는 상세 화면 행동 로그에 따라 추가 행동 항목별 5점씩 가산하며 최대 20점까지 반영한다.
  - 체류시간이 30초 이상이면 5점을 가산한다.
  - 최대 스크롤 깊이가 80% 이상이면 5점을 가산한다.
  - 적합도 상세 보기를 1회 이상 클릭했으면 5점을 가산한다.
  - 전체 거래 보러가기를 1회 이상 클릭했으면 5점을 가산한다.
- 사용자 맞춤 주택 추천 목록에서는 평가 가능한 맞춤 조건이 없어도 상세 화면 행동 로그가 있으면 해당 가산점만으로 추천할 수 있다.
- 상세 화면 행동 로그 가산점 반영 후 최종 `score`는 100점을 초과하지 않는다.
- 사용자 맞춤 주택 추천 목록은 최종 `score`가 높은 순서로 정렬한다.
- 주택 맞춤 조건 적합도 조회는 평가 가능한 맞춤 조건이 없으면 상세 화면 행동 로그를 반영하지 않고 `NO_EVALUABLE_CONDITION`을 반환한다.

### AI 주택 비교

로그인한 사용자가 선택한 두 주택의 상세 정보, 거래 이력, 주변시설, 맞춤 적합도, 사용자 맞춤 조건, 최근 본 주택, 관심 주택 정보를 기반으로 AI 비교 결과를 생성한다.

요청 예시:

```json
{
  "propertyIds": [1, 2],
  "comparisonPurpose": "실거주 관점의 주택 비교"
}
```

- `propertyIds`는 서로 다른 주택 ID 2개를 전달한다.
- 외부 AI API 키는 서버 환경 변수 `GMS_KEY`로 관리한다.

응답 예시:

```json
{
  "oneLineSummary": "A는 예산과 병원 접근성이 좋고, B는 가격은 낮지만 선호 시설 근거가 부족합니다.",
  "recommendedProperty": "A",
  "recommendationReason": "사용자 선호 조건과 주변시설 정보를 함께 보면 A가 더 안정적인 선택입니다.",
  "comparisonTable": [
    {
      "criterion": "맞춤 적합도",
      "propertyA": "예산 적합, 병원 가까움",
      "propertyB": "가격은 좋지만 선호 시설 부족",
      "better": "A",
      "reason": "A가 사용자 선호 조건을 더 많이 충족합니다."
    }
  ],
  "propertyAPros": ["예산 조건에 적합합니다."],
  "propertyACons": ["제공된 정보 기준 단점은 제한적입니다."],
  "propertyBPros": ["가격 부담이 상대적으로 낮습니다."],
  "propertyBCons": ["선호 시설 충족 근거가 부족합니다."],
  "cautions": ["AI 비교는 제공된 데이터만 기준으로 하며 실제 계약 전 추가 확인이 필요합니다."],
  "recommendedFor": {
    "propertyA": "선호 조건 충족도를 중요하게 보는 사용자",
    "propertyB": "가격 부담을 우선으로 보는 사용자"
  }
}
```

## 사용자 맞춤

| 기능 | Method | URL |
| --- | --- | --- |
| 사용자 맞춤 지역 조건 후보 검색 | GET | `/users/info/preferences/regions/candidates` |
| 사용자 맞춤 조건 전체 저장(조건 조합) | PUT | `/users/info/preferences` |
| 사용자 맞춤 조건 해제(조건 조합) | DELETE | `/users/info/preferences` |
| 관심 지역 전체 조회 | GET | `/users/info/regions` |
| 관심 지역 등록 | POST | `/users/info/regions` |
| 관심 지역 해제 | DELETE | `/users/info/regions` |
| 관심 주택 전체 조회 | GET | `/users/info/properties` |
| 관심 주택 등록 | POST | `/users/info/properties` |
| 관심 주택 해제 | DELETE | `/users/info/properties` |
| 최근 본 주택 조회 | GET | `/users/info/recent-properties` |

### 주택 상세 화면 행동 로그 저장

로그인한 사용자가 주택 상세 화면을 떠날 때 아래 값을 저장한다.

- `dwellTimeMillis`: 상세 화면 체류시간(ms)
- `maxScrollDepthPercent`: 상세 화면 최대 스크롤 깊이(0~100)
- `recommendationDetailClicked`: 적합도 상세 보기 클릭 여부
- `dealHistoryClicked`: 전체 거래 보러가기 클릭 여부

요청 예시:

```json
{
  "dwellTimeMillis": 30000,
  "maxScrollDepthPercent": 80,
  "recommendationDetailClicked": true,
  "dealHistoryClicked": true
}
```

성공 시 `204 No Content`를 반환한다.
