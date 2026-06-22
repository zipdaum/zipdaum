package com.ssafy.zipdaum.preference.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.preference.dto.UserPreferenceItemRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceRegionCandidateResponse;
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
    preference.setCode("SALE_PRICE");
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
  void findRegionCandidates_검색어_공백을_제거한_키워드로_맞춤_지역_후보를_조회한다() {
    UserPreferenceRegionCandidateResponse candidate = new UserPreferenceRegionCandidateResponse();
    candidate.setSggCd("26350");
    candidate.setUmdNm("우동");
    candidate.setDisplayName("부산광역시 해운대구 우동");
    given(userPreferenceMapper.selectUserPreferenceRegionCandidates("해운대우동"))
        .willReturn(List.of(candidate));

    List<UserPreferenceRegionCandidateResponse> result =
        service.findRegionCandidates(" 해운대 우동 ");

    assertThat(result).containsExactly(candidate);
    then(userPreferenceMapper).should().selectUserPreferenceRegionCandidates("해운대우동");
  }

  @Test
  void findRegionCandidates_LIKE_특수문자를_이스케이프해서_조회한다() {
    given(userPreferenceMapper.selectUserPreferenceRegionCandidates("\\%우\\_동"))
        .willReturn(List.of());

    List<UserPreferenceRegionCandidateResponse> result = service.findRegionCandidates("%우_동");

    assertThat(result).isEmpty();
    then(userPreferenceMapper).should().selectUserPreferenceRegionCandidates("\\%우\\_동");
  }

  @Test
  void findRegionCandidates_검색어가_null이면_INVALID_INPUT_VALUE_예외가_발생한다() {
    assertThatThrownBy(() -> service.findRegionCandidates(null))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void findRegionCandidates_검색어가_공백이면_INVALID_INPUT_VALUE_예외가_발생한다() {
    assertThatThrownBy(() -> service.findRegionCandidates("   "))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void findRegionCandidates_검색어가_너무_길면_INVALID_INPUT_VALUE_예외가_발생한다() {
    String keyword = "a".repeat(51);

    assertThatThrownBy(() -> service.findRegionCandidates(keyword))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void savePreferences_입력된_조건을_등록한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(
        preference("SALE_PRICE", "500000000", 3),
        preference("DEPOSIT", "300000000", 6),
        preference("MONTHLY_RENT", "800000", 7),
        preference("AREA", "84.50", 2),
        preference("REGION", " 부산광역시 해운대구 우동 ", 1),
        preference("BUS", "true", 5),
        preference("CCTV", "false", 4)
    ));

    given(userPreferenceMapper.selectPreferenceTypesByCodes(List.of(
        "SALE_PRICE", "DEPOSIT", "MONTHLY_RENT", "AREA", "REGION", "BUS", "CCTV"
    ))).willReturn(List.of(
        preferenceType(1L, "SALE_PRICE"),
        preferenceType(2L, "DEPOSIT"),
        preferenceType(3L, "MONTHLY_RENT"),
        preferenceType(4L, "AREA"),
        preferenceType(5L, "REGION"),
        preferenceType(6L, "BUS"),
        preferenceType(9L, "CCTV")
    ));
    given(userPreferenceMapper.existsRegionDisplayName("부산광역시 해운대구 우동"))
        .willReturn(true);

    service.savePreferences(1L, request);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<com.ssafy.zipdaum.preference.dto.UserPreferenceSaveCommand>> captor =
        ArgumentCaptor.forClass(List.class);
    then(userPreferenceMapper).should().deleteUserPreferencesByUserId(1L);
    then(userPreferenceMapper).should().insertUserPreferences(org.mockito.ArgumentMatchers.eq(1L),
        captor.capture());
    assertThat(captor.getValue())
        .extracting("preferenceValue")
        .containsExactly(
            "500000000",
            "300000000",
            "800000",
            "84.5",
            "부산광역시 해운대구 우동",
            "true",
            "false"
        );
    assertThat(captor.getValue())
        .extracting("priority")
        .containsExactly(3, 6, 7, 2, 1, 5, 4);
  }

  @Test
  void savePreferences_REGION_조건은_여러_개_등록할_수_있다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(
        preference("REGION", "부산광역시 해운대구 우동", 1),
        preference("REGION", "부산광역시 수영구 광안동", 2)
    ));

    given(userPreferenceMapper.existsRegionDisplayName("부산광역시 해운대구 우동"))
        .willReturn(true);
    given(userPreferenceMapper.existsRegionDisplayName("부산광역시 수영구 광안동"))
        .willReturn(true);
    given(userPreferenceMapper.selectPreferenceTypesByCodes(List.of("REGION")))
        .willReturn(List.of(preferenceType(5L, "REGION")));

    service.savePreferences(1L, request);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<com.ssafy.zipdaum.preference.dto.UserPreferenceSaveCommand>> captor =
        ArgumentCaptor.forClass(List.class);
    then(userPreferenceMapper).should().insertUserPreferences(org.mockito.ArgumentMatchers.eq(1L),
        captor.capture());
    assertThat(captor.getValue())
        .extracting("preferenceValue")
        .containsExactly("부산광역시 해운대구 우동", "부산광역시 수영구 광안동");
  }

  @Test
  void savePreferences_존재하지_않는_REGION이면_INVALID_INPUT_VALUE_예외가_발생한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(preference("REGION", "부산광역시 해운대구 없는동", 1)));

    given(userPreferenceMapper.existsRegionDisplayName("부산광역시 해운대구 없는동"))
        .willReturn(false);

    assertThatThrownBy(() -> service.savePreferences(1L, request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void savePreferences_하위_REGION_이후_상위_REGION을_등록하면_상위_REGION으로_대체한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(
        preference("REGION", "부산광역시 해운대구 우동", 1),
        preference("REGION", "부산광역시 해운대구", 2)
    ));

    given(userPreferenceMapper.existsRegionDisplayName("부산광역시 해운대구 우동"))
        .willReturn(true);
    given(userPreferenceMapper.existsRegionDisplayName("부산광역시 해운대구"))
        .willReturn(true);
    given(userPreferenceMapper.selectPreferenceTypesByCodes(List.of("REGION")))
        .willReturn(List.of(preferenceType(5L, "REGION")));

    service.savePreferences(1L, request);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<com.ssafy.zipdaum.preference.dto.UserPreferenceSaveCommand>> captor =
        ArgumentCaptor.forClass(List.class);
    then(userPreferenceMapper).should().insertUserPreferences(org.mockito.ArgumentMatchers.eq(1L),
        captor.capture());
    assertThat(captor.getValue())
        .extracting("preferenceValue")
        .containsExactly("부산광역시 해운대구");
  }

  @Test
  void savePreferences_상위_REGION_이후_하위_REGION을_등록하면_하위_REGION은_무시한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(
        preference("REGION", "부산광역시 해운대구", 1),
        preference("REGION", "부산광역시 해운대구 우동", 2)
    ));

    given(userPreferenceMapper.existsRegionDisplayName("부산광역시 해운대구"))
        .willReturn(true);
    given(userPreferenceMapper.existsRegionDisplayName("부산광역시 해운대구 우동"))
        .willReturn(true);
    given(userPreferenceMapper.selectPreferenceTypesByCodes(List.of("REGION")))
        .willReturn(List.of(preferenceType(5L, "REGION")));

    service.savePreferences(1L, request);

    @SuppressWarnings("unchecked")
    ArgumentCaptor<List<com.ssafy.zipdaum.preference.dto.UserPreferenceSaveCommand>> captor =
        ArgumentCaptor.forClass(List.class);
    then(userPreferenceMapper).should().insertUserPreferences(org.mockito.ArgumentMatchers.eq(1L),
        captor.capture());
    assertThat(captor.getValue())
        .extracting("preferenceValue")
        .containsExactly("부산광역시 해운대구");
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
        preference("SALE_PRICE", "500000000", 1),
        preference("SALE_PRICE", "600000000", 2)
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
