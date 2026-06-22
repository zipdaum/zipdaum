package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.property.domain.SurroundingType;
import com.ssafy.zipdaum.property.dto.FacilitySource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FacilitySourceLoader {

  private static final String BUS_CSV_PATH = "data/busan_bus_stops.csv";
  private static final String SUBWAY_CSV_PATH = "data/busan_subway_stations.csv";
  private static final String HOSPITAL_CSV_PATH = "data/busan_hospital.csv";
  private static final String CCTV_CSV_PATH = "data/busan_cctv.csv";
  private static final String PARK_CSV_PATH = "data/busan_15min_urban_parks.csv";

  public List<FacilitySource> loadAll() {
    List<FacilitySource> facilities = new ArrayList<>();
    loadBusStops(facilities);
    loadSubwayStations(facilities);
    loadHospitals(facilities);
    loadCctvs(facilities);
    loadParks(facilities);
    log.info("주변 시설 로컬 데이터 로딩 완료 count={}", facilities.size());
    return facilities;
  }

  private void loadBusStops(List<FacilitySource> facilities) {
    loadDelimitedFile(BUS_CSV_PATH, ',').forEach(row -> addFacility(
        facilities,
        SurroundingType.BUS,
        findString(row, "name", "정류소명", "정류장명"),
        null,
        findDecimal(row, "latitude", "위도", "gpsy"),
        findDecimal(row, "longitude", "경도", "gpsx"),
        "부산광역시_버스 정류소 정보(SHP)",
        findString(row, "type", "정류소구분", "stoptype")
    ));
  }

  private void loadSubwayStations(List<FacilitySource> facilities) {
    loadDelimitedFile(SUBWAY_CSV_PATH, ',').forEach(row -> addFacility(
        facilities,
        SurroundingType.SUBWAY,
        findString(row, "역사명", "역명"),
        findString(row, "역사도로명주소", "주소"),
        findDecimal(row, "역위도", "위도", "latitude"),
        findDecimal(row, "역경도", "경도", "longitude"),
        "부산교통공사_도시철도역사정보",
        findString(row, "노선명", "환승노선명")
    ));
  }

  private void loadHospitals(List<FacilitySource> facilities) {
    loadDelimitedFile(HOSPITAL_CSV_PATH, ',').forEach(row -> addFacility(
        facilities,
        SurroundingType.HOSPITAL,
        findString(row, "의료기관명", "병원명"),
        findString(row, "도로명주소", "주소"),
        findDecimal(row, "위도", "latitude"),
        findDecimal(row, "경도", "longitude"),
        "부산광역시_종합병원 현황",
        findString(row, "종별", "전화번호")
    ));
  }

  private void loadCctvs(List<FacilitySource> facilities) {
    loadDelimitedFile(CCTV_CSV_PATH, ',').forEach(row -> addFacility(
        facilities,
        SurroundingType.CCTV,
        findString(row, "시설명칭", "관리번호"),
        findString(row, "구군"),
        findDecimal(row, "위도", "latitude"),
        findDecimal(row, "경도", "longitude"),
        "부산광역시_방범용 CCTV 정보",
        findString(row, "장비종류", "관리번호")
    ));
  }

  private void loadParks(List<FacilitySource> facilities) {
    loadDelimitedFile(PARK_CSV_PATH, ',').forEach(row -> addFacility(
        facilities,
        SurroundingType.PARK,
        findString(row, "공원명"),
        findString(row, "상세주소"),
        findDecimal(row, "y좌표"),
        findDecimal(row, "x좌표"),
        "부산 15분도시 생활권 도시공원 CSV",
        findString(row, "공원종류")
    ));
  }

  private void addFacility(List<FacilitySource> facilities, SurroundingType type, String name,
      String address, BigDecimal latitude, BigDecimal longitude, String source, String detail) {
    if (latitude == null || longitude == null) {
      return;
    }
    facilities.add(new FacilitySource(
        type,
        name == null || name.isBlank() ? type.name() : name,
        address,
        latitude,
        longitude,
        source,
        detail
    ));
  }

  private List<Map<String, String>> loadDelimitedFile(String path, char delimiter) {
    ClassPathResource resource = new ClassPathResource(path);
    try (BufferedReader reader = new BufferedReader(
        new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
      String headerLine = reader.readLine();
      if (headerLine == null) {
        return List.of();
      }

      List<String> headers = parseDelimitedLine(stripBom(headerLine), delimiter);
      List<Map<String, String>> rows = new ArrayList<>();
      String line;
      while ((line = reader.readLine()) != null) {
        List<String> values = parseDelimitedLine(line, delimiter);
        Map<String, String> row = new LinkedHashMap<>();
        for (int i = 0; i < headers.size() && i < values.size(); i++) {
          row.put(headers.get(i), values.get(i));
        }
        rows.add(row);
      }
      log.info("주변 시설 파일 로딩 완료 path={}, count={}", path, rows.size());
      return rows;
    } catch (IOException e) {
      throw new IllegalStateException("주변 시설 파일 로딩 실패 path=" + path, e);
    }
  }

  private List<String> parseDelimitedLine(String line, char delimiter) {
    List<String> values = new ArrayList<>();
    StringBuilder value = new StringBuilder();
    boolean quoted = false;
    for (int i = 0; i < line.length(); i++) {
      char current = line.charAt(i);
      if (current == '"') {
        quoted = !quoted;
      } else if (current == delimiter && !quoted) {
        values.add(value.toString());
        value.setLength(0);
      } else {
        value.append(current);
      }
    }
    values.add(value.toString());
    return values;
  }

  private String stripBom(String value) {
    return value == null ? null : value.replace("\uFEFF", "");
  }

  private String findString(Map<?, ?> row, String... names) {
    Object value = findByNormalizedName(row, names);
    return value == null ? null : value.toString().trim();
  }

  private BigDecimal findDecimal(Map<?, ?> row, String... names) {
    Object value = findByNormalizedName(row, names);
    if (value == null || value.toString().isBlank()) {
      return null;
    }
    try {
      return new BigDecimal(value.toString().trim());
    } catch (NumberFormatException e) {
      return null;
    }
  }

  private Object findByNormalizedName(Map<?, ?> row, String... names) {
    Map<String, Object> normalizedRow = new LinkedHashMap<>();
    row.forEach((key, value) -> normalizedRow.put(normalizeKey(key == null ? "" : key.toString()), value));
    for (String name : names) {
      Object value = normalizedRow.get(normalizeKey(name));
      if (value != null) {
        return value;
      }
    }
    return null;
  }

  private String normalizeKey(String key) {
    return key == null ? "" : key.toLowerCase(Locale.ROOT).replaceAll("[\\s_()\\-]", "");
  }
}
