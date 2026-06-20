package com.ssafy.zipdaum.interaction.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.interaction.dto.UserPropertyInteractionRequest;
import com.ssafy.zipdaum.interaction.dto.UserPropertyInteractionSaveCommand;
import com.ssafy.zipdaum.interaction.mapper.UserPropertyInteractionMapper;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPropertyInteractionServiceImpl implements UserPropertyInteractionService {

  private final UserPropertyInteractionMapper userPropertyInteractionMapper;
  private final PropertyMapper propertyMapper;

  @Override
  @Transactional
  public void saveInteraction(
      Long userId,
      Long propertyId,
      UserPropertyInteractionRequest request
  ) {
    validatePropertyId(propertyId);

    if (!propertyMapper.existsPropertyById(propertyId)) {
      log.warn("행동 로그 저장 실패 - 존재하지 않는 주택 propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.PROPERTY_NOT_FOUND);
    }

    userPropertyInteractionMapper.saveUserPropertyInteraction(
        new UserPropertyInteractionSaveCommand(
            userId,
            propertyId,
            request.getDwellTimeMillis(),
            request.getMaxScrollDepthPercent(),
            request.getRecommendationDetailClicked(),
            request.getDealHistoryClicked()
        )
    );
    log.info("행동 로그 저장 완료 userId={}, propertyId={}", userId, propertyId);
  }

  private void validatePropertyId(Long propertyId) {
    if (propertyId == null) {
      log.warn("행동 로그 저장 실패 - 주택 ID 누락");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    if (propertyId < 1) {
      log.warn("행동 로그 저장 실패 - 잘못된 주택 ID propertyId={}", propertyId);
      throw new BusinessException(ErrorCode.INVALID_PROPERTY_ID);
    }
  }
}
