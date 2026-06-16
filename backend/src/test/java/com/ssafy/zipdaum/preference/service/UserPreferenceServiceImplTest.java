package com.ssafy.zipdaum.preference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.preference.dto.UserPreferenceItemRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceResponse;
import com.ssafy.zipdaum.preference.dto.PreferenceTypeDto;
import com.ssafy.zipdaum.preference.mapper.UserPreferenceMapper;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserPreferenceServiceImplTest {

  private final UserPreferenceMapper userPreferenceMapper = mock(UserPreferenceMapper.class);
  private final UserPreferenceServiceImpl service = new UserPreferenceServiceImpl(userPreferenceMapper);

  @Test
  void findPreferences_맞춤_조건을_조회한다() {
    UserPreferenceResponse preference = new UserPreferenceResponse();
    preference.setCode("BUDGET");
    given(userPreferenceMapper.selectUserPreferencesByUserId(1L)).willReturn(List.of(preference));

    List<UserPreferenceResponse> result = service.findPreferences(1L);

    assertThat(result).containsExactly(preference);
  }

  @Test
  void findPreferences_맞춤_조건이_없으면_PREFERENCE_NOT_FOUND_예외가_발생한다() {
    given(userPreferenceMapper.selectUserPreferencesByUserId(1L)).willReturn(List.of());

    assertThatThrownBy(() -> service.findPreferences(1L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PREFERENCE_NOT_FOUND)
        );
  }

  @Test
  void savePreferences_입력된_조건을_등록한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(
        preference("BUDGET", "500000000", 3),
        preference("AREA", "84.50", 2),
        preference("REGION", " 부산광역시 해운대구 우동 ", 1),
        preference("BUS", "true", 5),
        preference("CCTV", "false", 4)
    ));

    given(userPreferenceMapper.selectPreferenceTypesByCodes(List.of(
        "BUDGET", "AREA", "REGION", "BUS", "CCTV"
    ))).willReturn(List.of(
        preferenceType(1L, "BUDGET"),
        preferenceType(2L, "AREA"),
        preferenceType(4L, "REGION"),
        preferenceType(5L, "BUS"),
        preferenceType(8L, "CCTV")
    ));

    service.savePreferences(1L, request);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<com.ssafy.zipdaum.preference.dto.UserPreferenceSaveCommand>> captor =
        ArgumentCaptor.forClass(List.class);
    then(userPreferenceMapper).should().deleteUserPreferencesByUserId(1L);
    then(userPreferenceMapper).should().insertUserPreferences(org.mockito.ArgumentMatchers.eq(1L),
        captor.capture());
    assertThat(captor.getValue())
        .extracting("preferenceValue")
        .containsExactly("500000000", "84.5", "부산광역시 해운대구 우동", "true", "false");
    assertThat(captor.getValue())
        .extracting("priority")
        .containsExactly(3, 2, 1, 5, 4);
  }

  @Test
  void savePreferences_등록할_조건이_없으면_INVALID_INPUT_VALUE_예외가_발생한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of());

    assertThatThrownBy(() -> service.savePreferences(1L, request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void savePreferences_지원하지_않는_조건이면_INVALID_INPUT_VALUE_예외가_발생한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(preference("INVALID", "1", 1)));

    assertThatThrownBy(() -> service.savePreferences(1L, request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void savePreferences_조건값이_타입에_맞지_않으면_INVALID_INPUT_VALUE_예외가_발생한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(preference("BUILD_YEAR", "1899", 1)));

    assertThatThrownBy(() -> service.savePreferences(1L, request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void savePreferences_중복된_조건이면_INVALID_INPUT_VALUE_예외가_발생한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(
        preference("BUDGET", "500000000", 1),
        preference("BUDGET", "600000000", 2)
    ));

    assertThatThrownBy(() -> service.savePreferences(1L, request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void removePreferences_삭제된_조건이_없으면_PREFERENCE_NOT_FOUND_예외가_발생한다() {
    given(userPreferenceMapper.deleteUserPreferencesByUserId(1L)).willReturn(0);

    assertThatThrownBy(() -> service.removePreferences(1L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PREFERENCE_NOT_FOUND)
        );
  }

  private PreferenceTypeDto preferenceType(Long id, String code) {
    PreferenceTypeDto preferenceType = new PreferenceTypeDto();
    preferenceType.setId(id);
    preferenceType.setCode(code);
    return preferenceType;
  }

  private UserPreferenceItemRequest preference(String code, String value, Integer priority) {
    UserPreferenceItemRequest preference = new UserPreferenceItemRequest();
    preference.setCode(code);
    preference.setValue(value);
    preference.setPriority(priority);
    return preference;
  }
}
