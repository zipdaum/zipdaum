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
  public PropertyDealHistoryResponse findPropertyDealHistories(Long propertyId) {
    validatePropertyId(propertyId);

    if (!propertyMapper.existsPropertyById(propertyId)) {
      log.warn("존재하지 않는 주택 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    var saleDeals = propertyMapper.selectSaleDealsByPropertyId(propertyId);
    var rentDeals = propertyMapper.selectRentDealsByPropertyId(propertyId);

    log.debug(
        "주택 거래 이력 조회 완료 propertyId={}, saleDealCount={}, rentDealCount={}",
        propertyId,
        saleDeals.size(),
        rentDeals.size()
    );
    return new PropertyDealHistoryResponse(saleDeals, rentDeals);
  }

  private void validatePropertyId(Long propertyId) {
    if (propertyId == null || propertyId < 1) {
      log.warn("주택 조회 실패 - 잘못된 주택 ID propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.INVALID_PROPERTY_ID);
    }
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
