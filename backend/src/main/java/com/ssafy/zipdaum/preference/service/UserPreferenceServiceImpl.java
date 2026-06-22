package com.ssafy.zipdaum.preference.service;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.preference.domain.UserPreferenceType;
import com.ssafy.zipdaum.preference.dto.PreferenceTypeDto;
import com.ssafy.zipdaum.preference.dto.UserPreferenceItemRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceRegionCandidateResponse;
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

  private static final int MAX_SEARCH_KEYWORD_LENGTH = 50;

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
  @Transactional(readOnly = true)
  public List<UserPreferenceRegionCandidateResponse> findRegionCandidates(String keyword) {
    String normalizedKeyword = normalizeSearchKeyword(keyword);
    String searchKeyword = escapeLikeKeyword(removeBlank(normalizedKeyword));

    List<UserPreferenceRegionCandidateResponse> candidates =
        userPreferenceMapper.selectUserPreferenceRegionCandidates(searchKeyword);

    log.debug("맞춤 지역 조건 후보 검색 완료 candidateCount={}", candidates.size());
    return candidates;
  }

  @Override
  @Transactional
  public void savePreferences(Long userId, UserPreferenceRequest request) {
    List<UserPreferenceValue> values = toUserPreferenceValues(request);

    if (values.isEmpty()) {
      log.warn("등록할 맞춤 조건 없음 userId={}", userId);
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    List<String> requestedCodes = values.stream()
        .map(value -> value.type().name())
        .distinct()
        .toList();
    Map<String, PreferenceTypeDto> preferenceTypes = userPreferenceMapper.selectPreferenceTypesByCodes(
            requestedCodes
        ).stream()
        .collect(Collectors.toMap(PreferenceTypeDto::getCode, Function.identity()));

    if (preferenceTypes.size() != requestedCodes.size()) {
      log.warn("맞춤 조건 타입 정보 누락 userId={}, requestedCount={}, foundCount={}",
          userId, requestedCodes.size(), preferenceTypes.size());
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
    List<String> requestedRegions = new ArrayList<>();

    for (UserPreferenceItemRequest preference : request.getPreferences()) {
      UserPreferenceType type = parsePreferenceType(preference.getCode());
      String normalizedValue = normalizePreferenceValue(type, preference.getValue());
      if (type == UserPreferenceType.REGION) {
        RegionAddDecision decision = prepareRegionPreference(values, requestedRegions, normalizedValue);
        if (decision == RegionAddDecision.SKIP) {
          continue;
        }
        if (decision == RegionAddDecision.DUPLICATE) {
          log.warn("중복된 맞춤 지역");
          throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
        }
      } else if (!requestedTypes.add(type)) {
        log.warn("중복된 맞춤 조건 code={}", type.name());
        throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
      }

      values.add(new UserPreferenceValue(
          type,
          normalizedValue,
          preference.getPriority()
      ));
    }

    return values;
  }

  private RegionAddDecision prepareRegionPreference(
      List<UserPreferenceValue> values,
      List<String> requestedRegions,
      String region) {
    String normalizedRegion = normalizeRegionName(region);

    for (String requestedRegion : requestedRegions) {
      String normalizedRequestedRegion = normalizeRegionName(requestedRegion);
      if (normalizedRequestedRegion.equals(normalizedRegion)) {
        return RegionAddDecision.DUPLICATE;
      }
      if (isParentRegion(normalizedRequestedRegion)
          && normalizedRegion.startsWith(normalizedRequestedRegion)) {
        return RegionAddDecision.SKIP;
      }
    }

    if (isParentRegion(normalizedRegion)) {
      requestedRegions.removeIf(requestedRegion ->
          normalizeRegionName(requestedRegion).startsWith(normalizedRegion)
      );
      values.removeIf(value -> value.type() == UserPreferenceType.REGION
          && normalizeRegionName(value.preferenceValue()).startsWith(normalizedRegion));
    }

    requestedRegions.add(region);
    return RegionAddDecision.ADD;
  }

  private String normalizeRegionName(String value) {
    return value
        .replaceFirst("^부산광역시\\s*", "")
        .replaceAll("\\s+", "");
  }

  private boolean isParentRegion(String normalizedRegion) {
    return normalizedRegion.endsWith("구") || normalizedRegion.endsWith("군");
  }

  private String normalizeSearchKeyword(String keyword) {
    if (keyword == null) {
      log.warn("맞춤 지역 조건 후보 검색 실패 - 검색어 누락");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    String normalizedKeyword = keyword.trim();

    if (normalizedKeyword.isBlank()) {
      log.warn("맞춤 지역 조건 후보 검색 실패 - 검색어 공백");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    if (normalizedKeyword.length() > MAX_SEARCH_KEYWORD_LENGTH) {
      log.warn("맞춤 지역 조건 후보 검색 실패 - 검색어 길이 초과 length={}", normalizedKeyword.length());
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    return normalizedKeyword;
  }

  private String escapeLikeKeyword(String keyword) {
    return keyword
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }

  private String removeBlank(String value) {
    return value.replaceAll("\\s+", "");
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
        case SALE_PRICE, DEPOSIT, MONTHLY_RENT ->
            String.valueOf(validatePrice(type, normalizedValue));
        case AREA -> validateArea(normalizedValue).stripTrailingZeros().toPlainString();
        case BUILD_YEAR -> String.valueOf(validateBuildYear(normalizedValue));
        case REGION -> validateRegion(normalizedValue);
        case BUS, SUBWAY, HOSPITAL, CCTV, PARK ->
            String.valueOf(validateBoolean(type, normalizedValue));
      };
    } catch (NumberFormatException e) {
      log.warn("맞춤 조건 값 형식 오류 type={}", type.name());
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
  }

  private long validatePrice(UserPreferenceType type, String value) {
    long price = Long.parseLong(value);
    if (price < 0) {
      log.warn("맞춤 조건 값 범위 오류 type={}", type.name());
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return price;
  }

  private BigDecimal validateArea(String value) {
    BigDecimal area = new BigDecimal(value);
    if (area.compareTo(BigDecimal.ZERO) <= 0) {
      log.warn("맞춤 조건 값 범위 오류 type=AREA");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return area;
  }

  private int validateBuildYear(String value) {
    int buildYear = Integer.parseInt(value);
    if (buildYear < 1900 || buildYear > 2100) {
      log.warn("맞춤 조건 값 범위 오류 type=BUILD_YEAR, min=1900, max=2100");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return buildYear;
  }

  private String validateRegion(String value) {
    if (!userPreferenceMapper.existsRegionDisplayName(value)) {
      log.warn("존재하지 않는 맞춤 지역");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }
    return value;
  }

  private boolean validateBoolean(UserPreferenceType type, String value) {
    if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
      log.warn("맞춤 조건 값 형식 오류 type={}", type.name());
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

  private enum RegionAddDecision {
    ADD,
    SKIP,
    DUPLICATE
  }
}
