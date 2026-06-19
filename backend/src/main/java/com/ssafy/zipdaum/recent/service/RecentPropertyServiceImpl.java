package com.ssafy.zipdaum.recent.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
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
  public void recordRecentProperty(Long userId, Long propertyId) {
    validatePropertyId(propertyId);

    if (!propertyMapper.existsPropertyById(propertyId)) {
      log.warn("존재하지 않는 주택 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    recentPropertyMapper.upsertRecentProperty(userId, propertyId);
    deleteRecentPropertiesOverLimit(userId);

    log.info("최근 본 주택 저장 완료 userId={}, propertyId={}", userId, propertyId);
  }

  private void validatePropertyId(Long propertyId) {
    if (propertyId == null) {
      log.warn("최근 본 주택 저장 실패 - 주택 ID 누락");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    if (propertyId < 1) {
      log.warn("최근 본 주택 저장 실패 - 잘못된 주택 ID propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.INVALID_PROPERTY_ID);
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
