package com.ssafy.zipdaum.recent.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
import com.ssafy.zipdaum.recent.dto.RecentPropertySaveRequest;
import com.ssafy.zipdaum.recent.mapper.RecentPropertyMapper;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecentPropertyServiceImpl implements RecentPropertyService {

  private static final int MAX_RECENT_PROPERTY_COUNT = 30;

  private final RecentPropertyMapper recentPropertyMapper;
  private final PropertyMapper propertyMapper;

  @Override
  @Transactional(readOnly = true)
  public List<RecentPropertyResponse> findRecentProperties(Long userId) {
    List<RecentPropertyResponse> recentProperties =
        recentPropertyMapper.selectRecentProperties(userId);
    log.debug("최근 본 주택 조회 완료 userId={}, count={}", userId, recentProperties.size());
    return recentProperties;
  }

  @Override
  @Transactional
  public void saveRecentProperty(Long userId, RecentPropertySaveRequest request) {
    validateRequest(request);

    if (!propertyMapper.existsPropertyById(request.getPropertyId())) {
      log.warn("존재하지 않는 주택 propertyId={}", request.getPropertyId());
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    validateLastDeal(request);
    recentPropertyMapper.upsertRecentProperty(
        userId,
        request.getPropertyId(),
        request.getLastDealType(),
        request.getLastDealId()
    );
    deleteRecentPropertiesOverLimit(userId);

    log.info("최근 본 주택 저장 완료 userId={}, propertyId={}, lastDealType={}, lastDealId={}",
        userId, request.getPropertyId(), request.getLastDealType(), request.getLastDealId());
  }

  private void validateRequest(RecentPropertySaveRequest request) {
    if (request.getPropertyId() == null) {
      log.warn("최근 본 주택 저장 실패 - 주택 ID 누락");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    boolean hasDealType = request.getLastDealType() != null;
    boolean hasDealId = request.getLastDealId() != null;
    if (hasDealType != hasDealId) {
      log.warn("최근 본 주택 저장 실패 - 마지막 거래 정보 불완전 propertyId={}", request.getPropertyId());
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private void validateLastDeal(RecentPropertySaveRequest request) {
    if (request.getLastDealType() == null) {
      return;
    }
    if (!recentPropertyMapper.existsDealForProperty(
        request.getPropertyId(),
        request.getLastDealType(),
        request.getLastDealId()
    )) {
      log.warn("주택에 속하지 않는 거래 propertyId={}, dealType={}, dealId={}",
          request.getPropertyId(), request.getLastDealType(), request.getLastDealId());
      throw new BusinessException(ErrorCode.DEAL_NOT_FOUND);
    }
  }

  private void deleteRecentPropertiesOverLimit(Long userId) {
    List<Long> deleteIds =
        recentPropertyMapper.selectRecentPropertyIdsOverLimit(userId, MAX_RECENT_PROPERTY_COUNT);
    if (!deleteIds.isEmpty()) {
      recentPropertyMapper.deleteRecentPropertiesByIds(deleteIds);
    }
  }
}
