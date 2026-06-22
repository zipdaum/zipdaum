package com.ssafy.zipdaum.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.ssafy.zipdaum.auth.dto.AuthRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceItemRequest;
import com.ssafy.zipdaum.preference.dto.UserPreferenceRequest;
import com.ssafy.zipdaum.favorite.dto.FavoritePropertyCreateRequest;
import com.ssafy.zipdaum.interaction.dto.UserPropertyInteractionRequest;
import com.ssafy.zipdaum.property.dto.PropertySearchRequest;
import com.ssafy.zipdaum.property.dto.SurroundingRequest;
import com.ssafy.zipdaum.user.dto.UserSignUpRequest;
import com.ssafy.zipdaum.user.dto.UserUpdateRequest;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.List;
import org.junit.jupiter.api.Test;

class RequestValidationTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void validate_유효한_요청이면_검증_오류가_발생하지_않는다() {
    UserSignUpRequest signUpRequest = new UserSignUpRequest();
    signUpRequest.setEmail("user@example.com");
    signUpRequest.setPassword("password1234");
    signUpRequest.setName("홍길동");

    AuthRequest authRequest = new AuthRequest();
    authRequest.setEmail("user@example.com");
    authRequest.setPassword("password1234");

    UserUpdateRequest updateRequest = new UserUpdateRequest();
    updateRequest.setName("김집다움");

    PropertySearchRequest propertySearchRequest = new PropertySearchRequest();
    propertySearchRequest.setSggCd("26350");
    propertySearchRequest.setDealType("sale");
    propertySearchRequest.setMinPrice(0L);
    propertySearchRequest.setMaxPrice(100000L);
    propertySearchRequest.setSortBy("price");
    propertySearchRequest.setSortDirection("asc");

    SurroundingRequest surroundingRequest = new SurroundingRequest();
    surroundingRequest.setRadiusMeters(50);

    UserPreferenceRequest userPreferenceRequest = new UserPreferenceRequest();
    userPreferenceRequest.setPreferences(List.of(preference("SALE_PRICE", "500000000", 1)));

    UserPropertyInteractionRequest interactionRequest = new UserPropertyInteractionRequest();
    interactionRequest.setDwellTimeMillis(30000L);
    interactionRequest.setMaxScrollDepthPercent(80);
    interactionRequest.setRecommendationDetailClicked(true);
    interactionRequest.setDealHistoryClicked(false);

    assertThat(validator.validate(signUpRequest)).isEmpty();
    assertThat(validator.validate(authRequest)).isEmpty();
    assertThat(validator.validate(updateRequest)).isEmpty();
    assertThat(validator.validate(propertySearchRequest)).isEmpty();
    assertThat(validator.validate(surroundingRequest)).isEmpty();
    assertThat(validator.validate(userPreferenceRequest)).isEmpty();
    assertThat(validator.validate(interactionRequest)).isEmpty();
  }

  @Test
  void validate_회원가입_요청값이_유효하지_않으면_각_필드의_검증_오류가_발생한다() {
    UserSignUpRequest request = new UserSignUpRequest();
    request.setEmail("invalid-email");
    request.setPassword("short");
    request.setName(" ");

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("email", "password", "name");
  }

  @Test
  void validate_로그인_요청값이_유효하지_않으면_각_필드의_검증_오류가_발생한다() {
    AuthRequest request = new AuthRequest();
    request.setEmail("invalid-email");
    request.setPassword("short");

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("email", "password");
  }

  @Test
  void validate_회원정보_수정_이름이_유효하지_않으면_검증_오류가_발생한다() {
    UserUpdateRequest request = new UserUpdateRequest();
    request.setName(" ");

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("name");
  }

  @Test
  void validate_관심_주택_ID가_양수가_아니면_검증_오류가_발생한다() {
    FavoritePropertyCreateRequest request = new FavoritePropertyCreateRequest();
    request.setPropertyId(0L);

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("propertyId");
  }

  @Test
  void validate_주택_검색_요청값이_유효하지_않으면_각_필드의_검증_오류가_발생한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setSggCd("2635");
    request.setDealType("INVALID");
    request.setMinPrice(-1L);
    request.setMaxPrice(-1L);
    request.setSortBy("INVALID");
    request.setSortDirection("INVALID");

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("sggCd", "dealType", "minPrice", "maxPrice", "sortBy", "sortDirection");
  }

  @Test
  void validate_주변시설_반경이_허용범위를_벗어나면_검증_오류가_발생한다() {
    SurroundingRequest tooSmall = new SurroundingRequest();
    tooSmall.setRadiusMeters(0);

    SurroundingRequest tooLarge = new SurroundingRequest();
    tooLarge.setRadiusMeters(3001);

    assertThat(validator.validate(tooSmall))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("radiusMeters");
    assertThat(validator.validate(tooLarge))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("radiusMeters");
  }

  @Test
  void validate_맞춤_조건_요청값이_유효하지_않으면_각_필드의_검증_오류가_발생한다() {
    UserPreferenceRequest request = new UserPreferenceRequest();
    request.setPreferences(List.of(preference(" ", " ", 0)));

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains("preferences[0].code", "preferences[0].value", "preferences[0].priority");
  }

  @Test
  void validate_행동_로그_요청값이_유효하지_않으면_각_필드의_검증_오류가_발생한다() {
    UserPropertyInteractionRequest request = new UserPropertyInteractionRequest();
    request.setDwellTimeMillis(-1L);
    request.setMaxScrollDepthPercent(101);
    request.setRecommendationDetailClicked(null);
    request.setDealHistoryClicked(null);

    assertThat(validator.validate(request))
        .extracting(violation -> violation.getPropertyPath().toString())
        .contains(
            "dwellTimeMillis",
            "maxScrollDepthPercent",
            "recommendationDetailClicked",
            "dealHistoryClicked"
        );
  }

  private UserPreferenceItemRequest preference(String code, String value, Integer priority) {
    UserPreferenceItemRequest preference = new UserPreferenceItemRequest();
    preference.setCode(code);
    preference.setValue(value);
    preference.setPriority(priority);
    return preference;
  }
}
