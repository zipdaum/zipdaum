package com.ssafy.zipdaum.property.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.DealType;
import com.ssafy.zipdaum.property.dto.PropertyDetailResponse;
import com.ssafy.zipdaum.property.dto.PropertySearchRequest;
import com.ssafy.zipdaum.property.dto.PropertySearchResponse;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
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
    PropertyDetailResponse detail = propertyMapper.selectPropertyById(propertyId);
    if (detail == null) {
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    detail.setSaleDeals(propertyMapper.selectSaleDealsByPropertyId(propertyId));
    detail.setRentDeals(propertyMapper.selectRentDealsByPropertyId(propertyId));
    return detail;
  }

  private void validateSearchRequest(PropertySearchRequest request) {
    if (request.getSggCd() != null && !request.getSggCd().isBlank()
        && !request.getSggCd().matches("\\d{5}")) {
      throw new BusinessException(ErrorCode.INVALID_LAWD_CODE);
    }
    if (request.getMinPrice() != null && request.getMinPrice() < 0) {
      throw new BusinessException(ErrorCode.INVALID_MIN_PRICE);
    }
    if (request.getMaxPrice() != null && request.getMaxPrice() < 0) {
      throw new BusinessException(ErrorCode.INVALID_MAX_PRICE);
    }
    if (request.getMinPrice() != null && request.getMaxPrice() != null
        && request.getMinPrice() > request.getMaxPrice()) {
      throw new BusinessException(ErrorCode.INVALID_PRICE_RANGE);
    }
    if (request.getDealType() != null && !request.getDealType().isBlank()) {
      try {
        DealType.valueOf(request.getDealType().trim().toUpperCase());
      } catch (IllegalArgumentException e) {
        throw new BusinessException(ErrorCode.INVALID_DEAL_TYPE);
      }
    }
    if (request.getSortBy() != null && !request.getSortBy().isBlank()
        && !SORT_OPTIONS.contains(request.getSortBy().trim().toUpperCase())) {
      throw new BusinessException(ErrorCode.INVALID_SORT_OPTION);
    }
    if (request.getSortDirection() != null && !request.getSortDirection().isBlank()
        && !SORT_DIRECTIONS.contains(request.getSortDirection().trim().toUpperCase())) {
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
