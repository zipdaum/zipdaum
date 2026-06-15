package com.ssafy.zipdaum.property.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.dto.PropertyDealHistoryResponse;
import com.ssafy.zipdaum.property.dto.PropertyDetailResponse;
import com.ssafy.zipdaum.property.dto.PropertyRentDealResponse;
import com.ssafy.zipdaum.property.dto.PropertySaleDealResponse;
import com.ssafy.zipdaum.property.dto.PropertySearchRequest;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class PropertyServiceImplTest {

  private final PropertyMapper propertyMapper = mock(PropertyMapper.class);
  private final PropertyServiceImpl service = new PropertyServiceImpl(propertyMapper);

  @Test
  void findPropertyDetail_주택_기본정보를_조회한다() {
    Long propertyId = 1L;
    PropertyDetailResponse detail = new PropertyDetailResponse();
    detail.setId(propertyId);
    given(propertyMapper.selectPropertyById(propertyId)).willReturn(detail);

    PropertyDetailResponse result = service.findPropertyDetail(propertyId);

    assertThat(result).isSameAs(detail);
    then(propertyMapper).should(never()).countSaleDealsByPropertyId(propertyId);
  }

  @Test
  void findPropertyDealHistories_전체_거래이력을_조회한다() {
    Long propertyId = 1L;
    List<PropertySaleDealResponse> saleDeals = List.of(new PropertySaleDealResponse());
    List<PropertyRentDealResponse> rentDeals = List.of(new PropertyRentDealResponse());
    given(propertyMapper.existsPropertyById(propertyId)).willReturn(true);
    given(propertyMapper.countSaleDealsByPropertyId(propertyId)).willReturn(1L);
    given(propertyMapper.countRentDealsByPropertyId(propertyId, "JEONSE")).willReturn(1L);
    given(propertyMapper.countRentDealsByPropertyId(propertyId, "MONTHLY_RENT")).willReturn(0L);
    given(propertyMapper.selectSaleDealsByPropertyId(propertyId, 5, 0)).willReturn(saleDeals);
    given(propertyMapper.selectRentDealsByPropertyId(propertyId, "JEONSE", 5, 0)).willReturn(rentDeals);

    PropertyDealHistoryResponse result = service.findPropertyDealHistories(propertyId, null, null, null, null);

    assertThat(result.getSaleDeals()).isSameAs(saleDeals);
    assertThat(result.getRentDeals()).isSameAs(rentDeals);
    assertThat(result.getSaleTotalCount()).isEqualTo(1);
    assertThat(result.getRentDealType()).isEqualTo("JEONSE");
    assertThat(result.getRentTotalCount()).isEqualTo(1);
  }

  @Test
  void findPropertyDealHistories_주택이_없으면_예외가_발생한다() {
    Long propertyId = 1L;
    given(propertyMapper.existsPropertyById(propertyId)).willReturn(false);

    assertThatThrownBy(() -> service.findPropertyDealHistories(propertyId, null, null, null, null))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROPERTY_NOT_FOUND)
        );
  }

  @Test
  void findPropertyDealHistories_주택ID가_유효하지_않으면_예외가_발생한다() {
    assertThatThrownBy(() -> service.findPropertyDealHistories(0L, null, null, null, null))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROPERTY_ID)
        );
  }

  @Test
  void findPropertyDealHistories_페이지조건을_적용해_조회한다() {
    Long propertyId = 1L;
    given(propertyMapper.existsPropertyById(propertyId)).willReturn(true);
    given(propertyMapper.countSaleDealsByPropertyId(propertyId)).willReturn(12L);
    given(propertyMapper.countRentDealsByPropertyId(propertyId, "JEONSE")).willReturn(3L);
    given(propertyMapper.countRentDealsByPropertyId(propertyId, "MONTHLY_RENT")).willReturn(8L);
    given(propertyMapper.selectSaleDealsByPropertyId(propertyId, 5, 5)).willReturn(List.of());
    given(propertyMapper.selectRentDealsByPropertyId(propertyId, "MONTHLY_RENT", 5, 10))
        .willReturn(List.of());

    PropertyDealHistoryResponse result =
        service.findPropertyDealHistories(propertyId, "monthly_rent", 2, 3, 5);

    assertThat(result.getSalePage()).isEqualTo(2);
    assertThat(result.getSaleTotalPages()).isEqualTo(3);
    assertThat(result.getRentDealType()).isEqualTo("MONTHLY_RENT");
    assertThat(result.getRentPage()).isEqualTo(3);
    assertThat(result.getRentTotalCount()).isEqualTo(8);
    assertThat(result.getMonthlyRentTotalCount()).isEqualTo(8);
  }

  @Test
  void findPropertyDealHistories_전월세유형이_올바르지_않으면_예외가_발생한다() {
    Long propertyId = 1L;
    given(propertyMapper.existsPropertyById(propertyId)).willReturn(true);

    assertThatThrownBy(() -> service.findPropertyDealHistories(propertyId, "INVALID", null, null, null))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_DEAL_TYPE)
        );
    then(propertyMapper).should(never()).countSaleDealsByPropertyId(propertyId);
  }

  @Test
  void findPropertyDealHistories_페이지크기가_허용범위를_벗어나면_예외가_발생한다() {
    Long propertyId = 1L;
    given(propertyMapper.existsPropertyById(propertyId)).willReturn(true);

    assertThatThrownBy(() -> service.findPropertyDealHistories(propertyId, null, null, null, 51))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void findPropertyDetail_주택이_없으면_예외가_발생한다() {
    Long propertyId = 1L;
    given(propertyMapper.selectPropertyById(propertyId)).willReturn(null);

    assertThatThrownBy(() -> service.findPropertyDetail(propertyId))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROPERTY_NOT_FOUND)
        );
  }

  @Test
  void findPropertyDetail_주택ID가_유효하지_않으면_예외가_발생한다() {
    assertThatThrownBy(() -> service.findPropertyDetail(0L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROPERTY_ID)
        );
  }

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
    assertThat(actual.getSortBy()).isEqualTo("LATEST");
    assertThat(actual.getSortDirection()).isEqualTo("DESC");
  }

  @Test
  void searchProperties_정렬조건을_대문자로_정리한_뒤_조회한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setSortBy("price");
    request.setSortDirection("asc");

    service.searchProperties(request);

    ArgumentCaptor<PropertySearchRequest> captor = ArgumentCaptor.forClass(PropertySearchRequest.class);
    then(propertyMapper).should().selectProperties(captor.capture());

    PropertySearchRequest actual = captor.getValue();
    assertThat(actual.getSortBy()).isEqualTo("PRICE");
    assertThat(actual.getSortDirection()).isEqualTo("ASC");
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

  @Test
  void searchProperties_정렬기준이_올바르지_않으면_예외가_발생한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setSortBy("INVALID");

    assertThatThrownBy(() -> service.searchProperties(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SORT_OPTION)
        );
  }

  @Test
  void searchProperties_정렬방향이_올바르지_않으면_예외가_발생한다() {
    PropertySearchRequest request = new PropertySearchRequest();
    request.setSortDirection("INVALID");

    assertThatThrownBy(() -> service.searchProperties(request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_SORT_DIRECTION)
        );
  }
}
