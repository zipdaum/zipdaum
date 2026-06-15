package com.ssafy.zipdaum.preference.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.preference.domain.UserPreferenceType;
import com.ssafy.zipdaum.preference.dto.PreferenceTypeDto;
import com.ssafy.zipdaum.preference.dto.UserPreferenceItemRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.dto.UserPreferenceSaveCommand;
import com.ssafy.zipdaum.preference.mapper.UserPreferenceMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceServiceImpl implements UserPreferenceService {

  private final UserPreferenceMapper userPreferenceMapper;

  @Override
  @Transactional(readOnly = true)
  public List<UserPreferenceResponse> findPreferences(Long userId) {
    List<UserPreferenceResponse> preferences =
        userPreferenceMapper.selectUserPreferencesByUserId(userId);

    if (preferences.isEmpty()) {
      log.warn("설정된 맞춤 조건 없음 userId={}", userId);
      throw new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND);
    }

    log.debug("맞춤 조건 조회 완료 userId={}, preferenceCount={}", userId, preferences.size());
    return preferences;
  }

  @Override
  @Transactional
  public void savePreferences(Long userId, UserPreferenceRequest request) {
    List<UserPreferenceValue> values = toUserPreferenceValues(request);

    if (values.isEmpty()) {
      log.warn("등록할 맞춤 조건 없음 userId={}", userId);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    Map<String, PreferenceTypeDto> preferenceTypes = userPreferenceMapper.selectPreferenceTypesByCodes(
            values.stream().map(value -> value.type().name()).toList()
        ).stream()
        .collect(Collectors.toMap(PreferenceTypeDto::getCode, Function.identity()));

    if (preferenceTypes.size() != values.size()) {
      log.warn("맞춤 조건 타입 정보 누락 userId={}, requestedCount={}, foundCount={}",
          userId, values.size(), preferenceTypes.size());
      throw new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND);
    }

    List<UserPreferenceSaveCommand> items = values.stream()
        .map(value -> new UserPreferenceSaveCommand(
            preferenceTypes.get(value.type().name()).getId(),
            value.preferenceValue(),
            value.priority()
        ))
        .toList();

    userPreferenceMapper.deleteUserPreferencesByUserId(userId);
    userPreferenceMapper.insertUserPreferences(userId, items);

    log.info("맞춤 조건 저장 완료 userId={}, preferenceCount={}", userId, items.size());
  }

  @Override
  @Transactional
  public void removePreferences(Long userId) {
    int deletedCount = userPreferenceMapper.deleteUserPreferencesByUserId(userId);

    if (deletedCount == 0) {
      log.warn("해제할 맞춤 조건 없음 userId={}", userId);
      throw new BusinessException(ErrorCode.PREFERENCE_NOT_FOUND);
    }

    log.info("맞춤 조건 해제 완료 userId={}, deletedCount={}", userId, deletedCount);
  }

  private List<UserPreferenceValue> toUserPreferenceValues(UserPreferenceRequest request) {
    List<UserPreferenceValue> values = new ArrayList<>();
    Set<UserPreferenceType> requestedTypes = new HashSet<>();

    for (UserPreferenceItemRequest preference : request.getPreferences()) {
      UserPreferenceType type = parsePreferenceType(preference.getCode());
      if (!requestedTypes.add(type)) {
        log.warn("중복된 맞춤 조건 code={}", type.name());
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
      }

      values.add(new UserPreferenceValue(
          type,
          normalizePreferenceValue(type, preference.getValue()),
          preference.getPriority()
      ));
    }

    return values;
  }

  private UserPreferenceType parsePreferenceType(String code) {
    try {
      return UserPreferenceType.fromCode(code.trim().toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("지원하지 않는 맞춤 조건 code={}", code);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private String normalizePreferenceValue(UserPreferenceType type, String value) {
    String normalizedValue = value.trim();

    try {
      return switch (type) {
        case BUDGET -> String.valueOf(validateBudget(normalizedValue));
        case AREA -> validateArea(normalizedValue).stripTrailingZeros().toPlainString();
        case BUILD_YEAR -> String.valueOf(validateBuildYear(normalizedValue));
        case REGION -> normalizedValue;
        case BUS, SUBWAY, HOSPITAL, CCTV, PARK -> String.valueOf(validateBoolean(normalizedValue));
      };
    } catch (NumberFormatException e) {
      log.warn("맞춤 조건 값 형식 오류 type={}, value={}", type.name(), value);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private long validateBudget(String value) {
    long budget = Long.parseLong(value);
    if (budget < 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return budget;
  }

  private BigDecimal validateArea(String value) {
    BigDecimal area = new BigDecimal(value);
    if (area.compareTo(BigDecimal.ZERO) <= 0) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return area;
  }

  private int validateBuildYear(String value) {
    int buildYear = Integer.parseInt(value);
    if (buildYear < 1900 || buildYear > 2100) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return buildYear;
  }

  private boolean validateBoolean(String value) {
    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return Boolean.parseBoolean(value);
  }

  private record UserPreferenceValue(
      UserPreferenceType type,
      String preferenceValue,
      Integer priority
  ) {
  }
}
