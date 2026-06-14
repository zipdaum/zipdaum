package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.SurroundingType;
import com.ssafy.zipdaum.property.dto.SurroundingFacilityResponse;
import com.ssafy.zipdaum.property.dto.SurroundingResponse;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import jakarta.annotation.PostConstruct;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurroundingServiceImpl implements SurroundingService {

  private static final int DEFAULT_RADIUS_METERS = 1000;
  private static final int MAX_RADIUS_METERS = 3000;
  private static final int MAX_FACILITY_COUNT = 80;

  private static final String BUS_CSV_PATH = "data/busan_bus_stops.csv";
  private static final String SUBWAY_CSV_PATH = "data/busan_subway_stations.csv";
  private static final String HOSPITAL_CSV_PATH = "data/busan_hospital.csv";
  private static final String CCTV_CSV_PATH = "data/busan_cctv.csv";
  private static final String PARK_CSV_PATH = "data/busan_15min_urban_parks.csv";

  private final PropertyMapper propertyMapper;
  private final List<FacilitySource> localFacilities = new ArrayList<>();

  @PostConstruct
  void loadFacilities() {
    localFacilities.clear();
    loadBusStops();
    loadSubwayStations();
    loadHospitals();
    loadCctvs();
    loadParks();
    log.info("주변 시설 로컬 데이터 로딩 완료 count={}", localFacilities.size());
  }

  @Override
  public SurroundingResponse findPropertySurroundings(Long propertyId, Integer radiusMeters) {
    validatePropertyId(propertyId);

    var property = propertyMapper.selectPropertyById(propertyId);
    if (property == null) {
      log.warn("주변 시설 조회 실패 - 존재하지 않는 주택 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }
    if (property.getLatitude() == null || property.getLongitude() == null) {
      log.warn("주변 시설 조회 실패 - 좌표 없음 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.COORDINATE_NOT_FOUND);
    }

    return findSurroundings(property.getLatitude(), property.getLongitude(), radiusMeters);
  }

  @Override
  public SurroundingResponse findSurroundings(BigDecimal latitude, BigDecimal longitude,
      Integer radiusMeters) {
    validateCoordinate(latitude, longitude);

    int radius = normalizeRadius(radiusMeters);
    double centerLat = latitude.doubleValue();
    double centerLng = longitude.doubleValue();

    List<SurroundingFacilityResponse> facilities = new ArrayList<>(
        findLocalFacilities(centerLat, centerLng, radius)
    );
    facilities.sort(Comparator.comparing(SurroundingFacilityResponse::getDistanceMeters)
        .thenComparing(SurroundingFacilityResponse::getName, Comparator.nullsLast(String::compareTo)));
    if (facilities.size() > MAX_FACILITY_COUNT) {
      facilities = facilities.subList(0, MAX_FACILITY_COUNT);
    }

    return new SurroundingResponse(latitude, longitude, radius, summarize(facilities), facilities);
  }

  private void validatePropertyId(Long propertyId) {
    if (propertyId == null || propertyId < 1) {
      log.warn("주변 시설 조회 실패 - 잘못된 주택 ID propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.INVALID_PROPERTY_ID);
    }
  }

  private void validateCoordinate(BigDecimal latitude, BigDecimal longitude) {
    if (latitude == null || longitude == null) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    double lat = latitude.doubleValue();
    double lng = longitude.doubleValue();
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private int normalizeRadius(Integer radiusMeters) {
    if (radiusMeters == null) {
      return DEFAULT_RADIUS_METERS;
    }
    if (radiusMeters < 100 || radiusMeters > MAX_RADIUS_METERS) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return radiusMeters;
  }

  private List<SurroundingFacilityResponse> findLocalFacilities(double centerLat, double centerLng,
      int radius) {
    return localFacilities.stream()
        .map(facility -> facility.toResponse(centerLat, centerLng))
        .filter(facility -> facility.getDistanceMeters() <= radius)
        .toList();
  }

  private void loadBusStops() {
    loadDelimitedFile(BUS_CSV_PATH, ',').forEach(row -> addFacility(
        SurroundingType.BUS,
        findString(row, "name", "정류소명", "정류장명"),
        null,
        findDecimal(row, "latitude", "위도", "gpsy"),
        findDecimal(row, "longitude", "경도", "gpsx"),
        "부산광역시_버스 정류소 정보(SHP)",
        findString(row, "type", "정류소구분", "stoptype")
    ));
  }

  private void loadSubwayStations() {
    loadDelimitedFile(SUBWAY_CSV_PATH, ',').forEach(row -> addFacility(
        SurroundingType.SUBWAY,
        findString(row, "역사명", "역명"),
        findString(row, "역사도로명주소", "주소"),
        findDecimal(row, "역위도", "위도", "latitude"),
        findDecimal(row, "역경도", "경도", "longitude"),
        "부산교통공사_도시철도역사정보",
        findString(row, "노선명", "환승노선명")
    ));
  }

  private void loadHospitals() {
    loadDelimitedFile(HOSPITAL_CSV_PATH, ',').forEach(row -> addFacility(
        SurroundingType.HOSPITAL,
        findString(row, "의료기관명", "병원명"),
        findString(row, "도로명주소", "주소"),
        findDecimal(row, "위도", "latitude"),
        findDecimal(row, "경도", "longitude"),
        "부산광역시_종합병원 현황",
        findString(row, "종별", "전화번호")
    ));
  }

  private void loadCctvs() {
    loadDelimitedFile(CCTV_CSV_PATH, ',').forEach(row -> addFacility(
        SurroundingType.CCTV,
        findString(row, "시설명칭", "관리번호"),
        findString(row, "구군"),
        findDecimal(row, "위도", "latitude"),
        findDecimal(row, "경도", "longitude"),
        "부산광역시_방범용 CCTV 정보",
        findString(row, "장비종류", "관리번호")
    ));
  }

  private void loadParks() {
    loadDelimitedFile(PARK_CSV_PATH, ',').forEach(row -> addFacility(
        SurroundingType.PARK,
        findString(row, "공원명"),
        findString(row, "상세주소"),
        findDecimal(row, "y좌표"),
        findDecimal(row, "x좌표"),
        "부산 15분도시 생활권 도시공원 CSV",
        findString(row, "공원종류")
    ));
  }

  private void addFacility(SurroundingType type, String name,
      String address, BigDecimal latitude, BigDecimal longitude, String source, String detail) {
    if (latitude == null || longitude == null) {
      return;
    }
    localFacilities.add(new FacilitySource(
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
      log.warn("주변 시설 파일 로딩 실패 path={}", path, e);
      return List.of();
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

  private SurroundingSummaryResponse summarize(List<SurroundingFacilityResponse> facilities) {
    return new SurroundingSummaryResponse(
        countByType(facilities, SurroundingType.BUS),
        countByType(facilities, SurroundingType.SUBWAY),
        countByType(facilities, SurroundingType.HOSPITAL),
        countByType(facilities, SurroundingType.CCTV),
        countByType(facilities, SurroundingType.PARK)
    );
  }

  private int countByType(List<SurroundingFacilityResponse> facilities,
      SurroundingType type) {
    return (int) facilities.stream()
        .filter(facility -> facility.getType() == type)
        .count();
  }

  private record FacilitySource(
      SurroundingType type,
      String name,
      String address,
      BigDecimal latitude,
      BigDecimal longitude,
      String source,
      String detail
  ) {

    SurroundingFacilityResponse toResponse(double centerLat, double centerLng) {
      int distance = calculateDistanceMetersStatic(centerLat, centerLng,
          latitude.doubleValue(), longitude.doubleValue());
      return new SurroundingFacilityResponse(
          type,
          name,
          address,
          latitude,
          longitude,
          distance,
          source,
          detail
      );
    }

    private static int calculateDistanceMetersStatic(double lat1, double lng1,
        double lat2, double lng2) {
      double earthRadius = 6371000;
      double latDistance = Math.toRadians(lat2 - lat1);
      double lngDistance = Math.toRadians(lng2 - lng1);
      double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
          + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
          * Math.sin(lngDistance / 2) * Math.sin(lngDistance / 2);
      double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
      return (int) Math.round(earthRadius * c);
    }
  }
}
