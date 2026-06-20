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
| 실거래가 | 실거래가 저장 | POST | `/properties` |
| 실거래가 | 주택 상세 조회 | GET | `/properties/{propertyId}` |
| 실거래가 | 거래 이력 조회 | GET | `/properties/{propertyId}/histories` |
| 사용자 맞춤 | 주택 맞춤 조건 적합도 조회 | GET | `/properties/{propertyId}/recommendation-score` |
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

## 실거래가 조회 및 주택/거래 상세 조회

| 기능 | Method | URL |
| --- | --- | --- |
| 주택 실거래가 검색 | GET | `/properties` |
| 사용자 맞춤 주택 추천 목록 조회 | GET | `/properties/recommendations` |
| 실거래가 저장 | POST | `/properties` |
| 주택 상세 조회 | GET | `/properties/{propertyId}` |
| 거래 이력 조회 | GET | `/properties/{propertyId}/histories` |
| 주택 주변 편의시설 조회 | GET | `/properties/{propertyId}/surroundings` |
| 주택 맞춤 조건 적합도 조회 | GET | `/properties/{propertyId}/recommendation-score` |

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
- 지역 조건은 선호 지역과 주택 지역이 일치하면 100점으로 평가한다.
- 시설 조건은 시설별 기준 반경 내 해당 시설이 1개 이상 있으면 100점으로 평가한다.
  - 버스, CCTV: 500m
  - 지하철역, 공원: 1000m
  - 병원: 1500m
- 사용자 맞춤 주택 추천 목록에서는 최근 본 주택인 경우 조회 횟수에 따라 5점씩 가산하며 최대 15점까지 반영한다.
- 사용자 맞춤 주택 추천 목록에서는 평가 가능한 맞춤 조건이 없어도 최근 본 주택 이력이 있으면 최근 본 주택 점수만으로 추천할 수 있다.
- 최근 본 주택 가산점 반영 후 최종 `score`는 100점을 초과하지 않는다.
- 사용자 맞춤 주택 추천 목록은 최종 `score`가 높은 순서로 정렬한다.
- 주택 맞춤 조건 적합도 조회는 평가 가능한 맞춤 조건이 없으면 최근 본 주택 이력을 반영하지 않고 `NO_EVALUABLE_CONDITION`을 반환한다.

## 사용자 맞춤

| 기능 | Method | URL |
| --- | --- | --- |
| 사용자 맞춤 조건 전체 저장(조건 조합) | PUT | `/users/info/preferences` |
| 사용자 맞춤 조건 해제(조건 조합) | DELETE | `/users/info/preferences` |
| 관심 지역 전체 조회 | GET | `/users/info/regions` |
| 관심 지역 등록 | POST | `/users/info/regions` |
| 관심 지역 해제 | DELETE | `/users/info/regions` |
| 관심 주택 전체 조회 | GET | `/users/info/properties` |
| 관심 주택 등록 | POST | `/users/info/properties` |
| 관심 주택 해제 | DELETE | `/users/info/properties` |
| 최근 본 주택 조회 | GET | `/users/info/recent-properties` |

