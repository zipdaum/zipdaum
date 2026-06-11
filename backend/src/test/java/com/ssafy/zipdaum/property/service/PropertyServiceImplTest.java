package com.ssafy.zipdaum.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.dto.PropertySearchRequest;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PropertyServiceImplTest {

  private final PropertyMapper propertyMapper = mock(PropertyMapper.class);
  private final PropertyServiceImpl service = new PropertyServiceImpl(propertyMapper);

  @Test
  void searchProperties_검색조건을_정리한_뒤_조회한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setSggCd("26350");
    request.setName(" 테스트 ");
    request.setDealType("sale");

    service.searchProperties(request);

    ArgumentCaptor<PropertySearchRequest> captor = ArgumentCaptor.forClass(PropertySearchRequest.class);
    then(propertyMapper).should().selectProperties(captor.capture());

    PropertySearchRequest actual = captor.getValue();
    assertThat(actual.getSggCd()).isEqualTo("26350");
    assertThat(actual.getName()).isEqualTo("테스트");
    assertThat(actual.getDealType()).isEqualTo("SALE");
  }

  @Test
  void searchProperties_법정동_코드가_5자리_숫자가_아니면_예외가_발생한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setSggCd("2635");

    assertThatThrownBy(() -> service.searchProperties(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_LAWD_CODE)
        );
  }

  @Test
  void searchProperties_최소가격이_음수이면_예외가_발생한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setMinPrice(-1L);

    assertThatThrownBy(() -> service.searchProperties(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_MIN_PRICE)
        );
  }

  @Test
  void searchProperties_최대가격이_음수이면_예외가_발생한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setMaxPrice(-1L);

    assertThatThrownBy(() -> service.searchProperties(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_MAX_PRICE)
        );
  }

  @Test
  void searchProperties_최소가격이_최대가격보다_크면_예외가_발생한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setMinPrice(2000L);
    request.setMaxPrice(1000L);

    assertThatThrownBy(() -> service.searchProperties(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PRICE_RANGE)
        );
  }

  @Test
  void searchProperties_거래유형이_올바르지_않으면_예외가_발생한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setDealType("INVALID");

    assertThatThrownBy(() -> service.searchProperties(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DEAL_TYPE)
        );
  }
}
