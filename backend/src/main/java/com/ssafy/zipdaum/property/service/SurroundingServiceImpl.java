package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.SurroundingType;
import com.ssafy.zipdaum.property.dto.FacilitySource;
import com.ssafy.zipdaum.property.dto.SurroundingFacilityResponse;
import com.ssafy.zipdaum.property.dto.SurroundingResponse;
import com.ssafy.zipdaum.property.dto.SurroundingSummaryResponse;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import jakarta.annotation.PostConstruct;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class SurroundingServiceImpl implements SurroundingService {

  private static final int DEFAULT_RADIUS_METERS = 1000;
  private static final int MAX_RADIUS_METERS = 3000;
  private static final int MAX_FACILITY_COUNT = 80;

  private final PropertyMapper propertyMapper;
  private final FacilitySourceLoader facilitySourceLoader;
  private final List<FacilitySource> localFacilities = new ArrayList<>();

  @PostConstruct
  void loadFacilities() {
    localFacilities.clear();
    localFacilities.addAll(facilitySourceLoader.loadAll());
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

    SurroundingResponse response = findSurroundings(
        property.getLatitude(),
        property.getLongitude(),
        radiusMeters
    );
    log.debug("주변 시설 조회 완료 propertyId={}, radiusMeters={}, facilityCount={}",
        propertyId, response.getRadiusMeters(), response.getFacilities().size());
    return response;
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
      log.warn("주변 시설 조회 실패 - 좌표 파라미터 누락 latitude={}, longitude={}", latitude, longitude);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    double lat = latitude.doubleValue();
    double lng = longitude.doubleValue();
    if (lat < -90 || lat > 90 || lng < -180 || lng > 180) {
      log.warn("주변 시설 조회 실패 - 잘못된 좌표 latitude={}, longitude={}", latitude, longitude);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private int normalizeRadius(Integer radiusMeters) {
    if (radiusMeters == null) {
      return DEFAULT_RADIUS_METERS;
    }
    if (radiusMeters < 1 || radiusMeters > MAX_RADIUS_METERS) {
      log.warn("주변 시설 조회 실패 - 잘못된 반경 radiusMeters={}", radiusMeters);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return radiusMeters;
  }

  private List<SurroundingFacilityResponse> findLocalFacilities(double centerLat, double centerLng,
      int radius) {
    return localFacilities.stream()
        .map(facility -> new FacilityDistance(facility).toResponse(centerLat, centerLng))
        .filter(facility -> facility.getDistanceMeters() <= radius)
        .toList();
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

  private record FacilityDistance(
      FacilitySource facility
  ) {

    SurroundingFacilityResponse toResponse(double centerLat, double centerLng) {
      int distance = calculateDistanceMetersStatic(centerLat, centerLng,
          facility.getLatitude().doubleValue(), facility.getLongitude().doubleValue());
      return new SurroundingFacilityResponse(
          facility.getType(),
          facility.getName(),
          facility.getAddress(),
          facility.getLatitude(),
          facility.getLongitude(),
          distance,
          facility.getSource(),
          facility.getDetail()
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
