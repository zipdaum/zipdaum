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
| 실거래가 | 실거래가 저장 | POST | `/properties` |
| 실거래가 | 주택 상세 조회 | GET | `/properties/{propertyId}` |
| 실거래가 | 거래 이력 조회 | GET | `/properties/{propertyId}/histories` |
| 사용자 맞춤 | 사용자 맞춤 조건 등록 및 수정(조건 조합) | PUT | `/users/info/preferences` |
| 사용자 맞춤 | 사용자 맞춤 조건 해제(조건 조합) | DELETE | `/users/info/preferences` |
| 사용자 맞춤 | 관심 지역 전체 조회 | GET | `/users/info/regions` |
| 사용자 맞춤 | 관심 지역 등록 | POST | `/users/info/regions` |
| 사용자 맞춤 | 관심 지역 해제 | DELETE | `/users/info/regions` |
| 사용자 맞춤 | 관심 주택 전체 조회 | GET | `/users/info/properties` |
| 사용자 맞춤 | 관심 주택 등록 | POST | `/users/info/properties` |
| 사용자 맞춤 | 관심 주택 해제 | DELETE | `/users/info/properties` |
| 사용자 맞춤 | 최근 본 주택 조회 | GET | `/users/info/recent-properties` |
| 사용자 맞춤 | 최근 본 주택 저장 | POST | `/users/info/recent-properties` |
| 실거래가 | 주택 주변 편의시설 조회 | GET | `/properties/{propertyId}/surroundings` |
| 리포트 및 알림 | 관심 지역 가격 변동 리포트 | GET | `/reports/price` |
| 리포트 및 알림 | 관심 지역/주택 신규 거래 알림 | GET | `/users/info/notifications` |

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
| 실거래가 저장 | POST | `/properties` |
| 주택 상세 조회 | GET | `/properties/{propertyId}` |
| 거래 이력 조회 | GET | `/properties/{propertyId}/histories` |
| 주택 주변 편의시설 조회 | GET | `/properties/{propertyId}/surroundings` |

## 사용자 맞춤

| 기능 | Method | URL |
| --- | --- | --- |
| 사용자 맞춤 조건 등록 및 수정(조건 조합) | PUT | `/users/info/preferences` |
| 사용자 맞춤 조건 해제(조건 조합) | DELETE | `/users/info/preferences` |
| 관심 지역 전체 조회 | GET | `/users/info/regions` |
| 관심 지역 등록 | POST | `/users/info/regions` |
| 관심 지역 해제 | DELETE | `/users/info/regions` |
| 관심 주택 전체 조회 | GET | `/users/info/properties` |
| 관심 주택 등록 | POST | `/users/info/properties` |
| 관심 주택 해제 | DELETE | `/users/info/properties` |
| 최근 본 주택 조회 | GET | `/users/info/recent-properties` |
| 최근 본 주택 저장 | POST | `/users/info/recent-properties` |

## 리포트 및 알림

| 기능 | Method | URL |
| --- | --- | --- |
| 관심 지역 가격 변동 리포트 | GET | `/reports/price` |
| 관심 지역/주택 신규 거래 알림 | GET | `/users/info/notifications` |
