package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.DealType;
import com.ssafy.zipdaum.property.dto.PropertyDealHistoryResponse;
import com.ssafy.zipdaum.property.dto.PropertyDetailResponse;
import com.ssafy.zipdaum.property.dto.PropertySearchRequest;
import com.ssafy.zipdaum.property.dto.PropertySearchResponse;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class PropertyServiceImpl implements PropertyService {

  private static final Set<String> SORT_OPTIONS = Set.of("LATEST", "PRICE", "NAME");
  private static final Set<String> SORT_DIRECTIONS = Set.of("ASC", "DESC");
  private static final Set<String> RENT_DEAL_TYPES = Set.of("JEONSE", "MONTHLY_RENT");
  private static final int DEFAULT_HISTORY_PAGE = 1;
  private static final int DEFAULT_HISTORY_SIZE = 5;
  private static final int MAX_HISTORY_SIZE = 50;

  private final PropertyMapper propertyMapper;

  @Override
  public List<PropertySearchResponse> searchProperties(PropertySearchRequest request) {
    validateSearchRequest(request);
    normalizeRequest(request);
    return propertyMapper.selectProperties(request);
  }

  @Override
  public PropertyDetailResponse findPropertyDetail(Long propertyId) {
    validatePropertyId(propertyId);

    PropertyDetailResponse detail = propertyMapper.selectPropertyById(propertyId);
    if (detail == null) {
      log.warn("존재하지 않는 주택 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    log.debug("주택 상세 조회 완료 propertyId={}", propertyId);
    return detail;
  }

  @Override
  public PropertyDealHistoryResponse findPropertyDealHistories(
      Long propertyId,
      String rentDealType,
      Integer salePage,
      Integer rentPage,
      Integer size) {
    validatePropertyId(propertyId);

    if (!propertyMapper.existsPropertyById(propertyId)) {
      log.warn("존재하지 않는 주택 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    String normalizedRentDealType = normalizeRentDealType(rentDealType);
    int normalizedSalePage = normalizeHistoryPage(salePage);
    int normalizedRentPage = normalizeHistoryPage(rentPage);
    int normalizedSize = normalizeHistorySize(size);
    int saleOffset = (normalizedSalePage - 1) * normalizedSize;
    int rentOffset = (normalizedRentPage - 1) * normalizedSize;

    long saleTotalCount = propertyMapper.countSaleDealsByPropertyId(propertyId);
    long jeonseTotalCount = propertyMapper.countRentDealsByPropertyId(propertyId, "JEONSE");
    long monthlyRentTotalCount = propertyMapper.countRentDealsByPropertyId(propertyId, "MONTHLY_RENT");
    long rentTotalCount = "MONTHLY_RENT".equals(normalizedRentDealType)
        ? monthlyRentTotalCount
        : jeonseTotalCount;

    var saleDeals = propertyMapper.selectSaleDealsByPropertyId(
        propertyId,
        normalizedSize,
        saleOffset
    );
    var rentDeals = propertyMapper.selectRentDealsByPropertyId(
        propertyId,
        normalizedRentDealType,
        normalizedSize,
        rentOffset
    );

    log.debug(
        "주택 거래 이력 조회 완료 propertyId={}, salePage={}, rentDealType={}, rentPage={}, size={}",
        propertyId,
        normalizedSalePage,
        normalizedRentDealType,
        normalizedRentPage,
        normalizedSize
    );
    return new PropertyDealHistoryResponse(
        saleDeals,
        rentDeals,
        normalizedSalePage,
        normalizedSize,
        saleTotalCount,
        calculateTotalPages(saleTotalCount, normalizedSize),
        normalizedRentDealType,
        normalizedRentPage,
        normalizedSize,
        rentTotalCount,
        calculateTotalPages(rentTotalCount, normalizedSize),
        jeonseTotalCount,
        monthlyRentTotalCount
    );
  }

  private void validatePropertyId(Long propertyId) {
    if (propertyId == null || propertyId < 1) {
      log.warn("주택 조회 실패 - 잘못된 주택 ID propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.INVALID_PROPERTY_ID);
    }
  }

  private String normalizeRentDealType(String rentDealType) {
    if (rentDealType == null || rentDealType.isBlank()) {
      return "JEONSE";
    }
    String normalized = rentDealType.trim().toUpperCase();
    if (!RENT_DEAL_TYPES.contains(normalized)) {
      log.warn("주택 거래 이력 조회 실패 - 잘못된 전월세 유형 rentDealType={}", rentDealType);
      throw new BusinessException(ErrorCode.INVALID_DEAL_TYPE);
    }
    return normalized;
  }

  private int normalizeHistoryPage(Integer page) {
    if (page == null) {
      return DEFAULT_HISTORY_PAGE;
    }
    if (page < 1) {
      log.warn("주택 거래 이력 조회 실패 - 잘못된 페이지 page={}", page);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return page;
  }

  private int normalizeHistorySize(Integer size) {
    if (size == null) {
      return DEFAULT_HISTORY_SIZE;
    }
    if (size < 1 || size > MAX_HISTORY_SIZE) {
      log.warn("주택 거래 이력 조회 실패 - 잘못된 페이지 크기 size={}", size);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return size;
  }

  private int calculateTotalPages(long totalCount, int size) {
    return Math.max((int) Math.ceil((double) totalCount / size), 1);
  }

  private void validateSearchRequest(PropertySearchRequest request) {
    if (request.getSggCd() != null && !request.getSggCd().isBlank()
        && !request.getSggCd().matches("\\d{5}")) {
      log.warn("주택 실거래가 검색 실패 - 잘못된 법정동 코드 sggCd={}", request.getSggCd());
      throw new BusinessException(ErrorCode.INVALID_LAWD_CODE);
    }
    if (request.getMinPrice() != null && request.getMinPrice() < 0) {
      log.warn("주택 실거래가 검색 실패 - 잘못된 최소 가격 minPrice={}", request.getMinPrice());
      throw new BusinessException(ErrorCode.INVALID_MIN_PRICE);
    }
    if (request.getMaxPrice() != null && request.getMaxPrice() < 0) {
      log.warn("주택 실거래가 검색 실패 - 잘못된 최대 가격 maxPrice={}", request.getMaxPrice());
      throw new BusinessException(ErrorCode.INVALID_MAX_PRICE);
    }
    if (request.getMinPrice() != null && request.getMaxPrice() != null
        && request.getMinPrice() > request.getMaxPrice()) {
      log.warn("주택 실거래가 검색 실패 - 잘못된 가격 범위 minPrice={}, maxPrice={}",
          request.getMinPrice(), request.getMaxPrice());
      throw new BusinessException(ErrorCode.INVALID_PRICE_RANGE);
    }
    if (request.getDealType() != null && !request.getDealType().isBlank()) {
      try {
        DealType.valueOf(request.getDealType().trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        log.warn("주택 실거래가 검색 실패 - 잘못된 거래 유형 dealType={}", request.getDealType());
        throw new BusinessException(ErrorCode.INVALID_DEAL_TYPE);
      }
    }
    if (request.getSortBy() != null && !request.getSortBy().isBlank()
        && !SORT_OPTIONS.contains(request.getSortBy().trim().toUpperCase())) {
      log.warn("주택 실거래가 검색 실패 - 잘못된 정렬 기준 sortBy={}", request.getSortBy());
      throw new BusinessException(ErrorCode.INVALID_SORT_OPTION);
    }
    if (request.getSortDirection() != null && !request.getSortDirection().isBlank()
        && !SORT_DIRECTIONS.contains(request.getSortDirection().trim().toUpperCase())) {
      log.warn("주택 실거래가 검색 실패 - 잘못된 정렬 방향 sortDirection={}", request.getSortDirection());
      throw new BusinessException(ErrorCode.INVALID_SORT_DIRECTION);
    }
  }

  private void normalizeRequest(PropertySearchRequest request) {
    request.setSggCd(normalizeBlankToNull(request.getSggCd()));
    request.setUmdNm(normalizeBlankToNull(request.getUmdNm()));
    request.setName(normalizeBlankToNull(request.getName()));
    request.setPropertyType(normalizeBlankToNull(request.getPropertyType()));
    if (request.getDealType() != null && !request.getDealType().isBlank()) {
      request.setDealType(request.getDealType().trim().toUpperCase());
    } else {
      request.setDealType(null);
    }
    request.setSortBy(normalizeSortBy(request.getSortBy()));
    request.setSortDirection(normalizeSortDirection(request.getSortDirection()));
  }

  private String normalizeBlankToNull(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }

  private String normalizeSortBy(String value) {
    if (value == null || value.isBlank()) {
      return "LATEST";
    }
    return value.trim().toUpperCase();
  }

  private String normalizeSortDirection(String value) {
    if (value == null || value.isBlank()) {
      return "DESC";
    }
    return value.trim().toUpperCase();
  }
}
