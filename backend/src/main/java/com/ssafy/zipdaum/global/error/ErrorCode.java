package com.ssafy.zipdaum.global.error;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {

  // 1. Common
  INVALID_INPUT_VALUE(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다."),
  INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 내부 오류가 발생했습니다."),
  METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "C003", "지원하지 않는 HTTP 메서드입니다."),

  // 2. Auth
  USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다."),
  INVALID_CREDENTIALS(HttpStatus.BAD_REQUEST, "U002", "이메일 또는 비밀번호가 올바르지 않습니다."),
  DUPLICATED_EMAIL(HttpStatus.BAD_REQUEST, "U003", "이미 존재하는 이메일입니다."),
  UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "U004", "인증 자격 증명이 유효하지 않습니다."),
  FORBIDDEN_MEMBER_ONLY(HttpStatus.FORBIDDEN, "U005", "회원가입을 하면 예산, 면적, 선호지역 등을 기준으로 내 조건에 맞는 주택 정보를 확인할 수 있습니다."),
  TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "U006", "만료된 토큰입니다."),
  TOKEN_INVALID(HttpStatus.UNAUTHORIZED, "U007", "유효하지 않은 토큰입니다."),

  // 3. Property / Deal
  PROPERTY_NOT_FOUND(HttpStatus.NOT_FOUND, "P001", "주택 정보를 찾을 수 없습니다."),
  DEAL_NOT_FOUND(HttpStatus.NOT_FOUND, "P002", "실거래가 정보를 찾을 수 없습니다."),
  INVALID_LAWD_CODE(HttpStatus.BAD_REQUEST, "P003", "법정동 코드는 5자리 숫자여야 합니다."),
  INVALID_DEAL_YMD(HttpStatus.BAD_REQUEST, "P004", "계약년월은 6자리 숫자여야 합니다."),
  REAL_ESTATE_API_KEY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "P005", "공공데이터 API 키가 설정되지 않았습니다."),
  KAKAO_API_KEY_NOT_FOUND(HttpStatus.INTERNAL_SERVER_ERROR, "P006", "카카오 API 키가 설정되지 않았습니다."),
  COORDINATE_NOT_FOUND(HttpStatus.NOT_FOUND, "P007", "주소에 해당하는 좌표 정보를 찾을 수 없습니다."),
  INVALID_MIN_PRICE(HttpStatus.BAD_REQUEST, "P008", "최소 가격은 0 이상이어야 합니다."),
  INVALID_MAX_PRICE(HttpStatus.BAD_REQUEST, "P009", "최대 가격은 0 이상이어야 합니다."),
  INVALID_PRICE_RANGE(HttpStatus.BAD_REQUEST, "P010", "최소 가격은 최대 가격보다 클 수 없습니다."),
  INVALID_DEAL_TYPE(HttpStatus.BAD_REQUEST, "P011", "거래 유형은 SALE, JEONSE, MONTHLY_RENT 중 하나여야 합니다."),
  INVALID_SORT_OPTION(HttpStatus.BAD_REQUEST, "P012", "정렬 기준은 LATEST, PRICE, NAME 중 하나여야 합니다."),
  INVALID_SORT_DIRECTION(HttpStatus.BAD_REQUEST, "P013", "정렬 방향은 ASC, DESC 중 하나여야 합니다."),
  INVALID_PROPERTY_ID(HttpStatus.BAD_REQUEST, "P014", "주택 ID는 1 이상의 숫자여야 합니다."),
  INVALID_REGION_CODE(HttpStatus.BAD_REQUEST, "P015", "지원하지 않는 지역 코드입니다."),

  // 4. Favorite
  FAVORITE_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "관심 목록에서 해당 정보를 찾을 수 없습니다."),
  FAVORITE_ALREADY_EXISTS(HttpStatus.BAD_REQUEST, "F002", "이미 관심 목록에 존재하는 정보입니다."),

  // 5. Condition
  CONDITION_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "설정된 맞춤 조건 정보를 찾을 수 없습니다."),

  // 6. Notification
  NOTIFICATION_NOT_FOUND(HttpStatus.NOT_FOUND, "N001", "알림 정보를 찾을 수 없습니다."),

  // 7. External API
  EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "E001", "외부 API 연동 중 오류가 발생했습니다.");

  private final HttpStatus status;
  private final String code;
  private final String message;

  ErrorCode(HttpStatus status, String code, String message){
    this.status = status;
    this.code = code;
    this.message = message;
  }
}
