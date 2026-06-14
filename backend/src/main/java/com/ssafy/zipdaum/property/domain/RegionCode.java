package com.ssafy.zipdaum.property.domain;

import java.util.Arrays;
import lombok.Getter;

@Getter
public enum RegionCode { // TODO 부산 외 지역코드 추가 시 DB 테이블 추가 검토
  BUSAN_JUNG("26110", "부산 중구"),
  BUSAN_SEO("26140", "부산 서구"),
  BUSAN_DONG("26170", "부산 동구"),
  BUSAN_YEONGDO("26200", "부산 영도구"),
  BUSAN_BUSANJIN("26230", "부산진구"),
  BUSAN_DONGNAE("26260", "부산 동래구"),
  BUSAN_NAM("26290", "부산 남구"),
  BUSAN_BUK("26320", "부산 북구"),
  BUSAN_HAEUNDAE("26350", "부산 해운대구"),
  BUSAN_SAHA("26380", "부산 사하구"),
  BUSAN_GEUMJEONG("26410", "부산 금정구"),
  BUSAN_GANGSEO("26440", "부산 강서구"),
  BUSAN_YEONJE("26470", "부산 연제구"),
  BUSAN_SUYEONG("26500", "부산 수영구"),
  BUSAN_SASANG("26530", "부산 사상구"),
  BUSAN_GIJANG("26710", "부산 기장군");

  private final String lawdCd;
  private final String name;

  RegionCode(String lawdCd, String name) {
    this.lawdCd = lawdCd;
    this.name = name;
  }

  public static String nameOf(String lawdCd) {
    return Arrays.stream(values())
        .filter(regionCode -> regionCode.lawdCd.equals(lawdCd))
        .map(RegionCode::getName)
        .findFirst()
        .orElse("부산");
  }

  public static boolean isValid(String lawdCd) {
    return Arrays.stream(values())
        .anyMatch(regionCode -> regionCode.lawdCd.equals(lawdCd));
  }
}
