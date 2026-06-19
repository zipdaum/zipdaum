package com.ssafy.zipdaum.recent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.DealType;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
import com.ssafy.zipdaum.recent.dto.RecentPropertySaveRequest;
import com.ssafy.zipdaum.recent.mapper.RecentPropertyMapper;
import java.util.List;
import org.junit.jupiter.api.Test;

class RecentPropertyServiceImplTest {

  private final RecentPropertyMapper recentPropertyMapper = mock(RecentPropertyMapper.class);
  private final PropertyMapper propertyMapper = mock(PropertyMapper.class);
  private final RecentPropertyServiceImpl service =
      new RecentPropertyServiceImpl(recentPropertyMapper, propertyMapper);

  @Test
  void findRecentProperties_최근_본_주택을_조회한다() {
    RecentPropertyResponse response = new RecentPropertyResponse();
    response.setPropertyId(1L);
    given(recentPropertyMapper.selectRecentProperties(1L)).willReturn(List.of(response));

    List<RecentPropertyResponse> result = service.findRecentProperties(1L);

    assertThat(result).containsExactly(response);
  }

  @Test
  void saveRecentProperty_주택이_존재하면_최근_본_주택을_저장한다() {
    RecentPropertySaveRequest request = request(10L, null, null);
    given(propertyMapper.existsPropertyById(10L)).willReturn(true);
    given(recentPropertyMapper.selectRecentPropertyIdsOverLimit(1L, 30)).willReturn(List.of());

    service.saveRecentProperty(1L, request);

    then(recentPropertyMapper).should().upsertRecentProperty(1L, 10L, null, null);
    then(recentPropertyMapper).should(never()).deleteRecentPropertiesByIds(
        org.mockito.ArgumentMatchers.anyList()
    );
  }

  @Test
  void saveRecentProperty_마지막_거래가_주택에_속하면_거래_정보도_저장한다() {
    RecentPropertySaveRequest request = request(10L, DealType.SALE, 100L);
    given(propertyMapper.existsPropertyById(10L)).willReturn(true);
    given(recentPropertyMapper.existsDealForProperty(10L, DealType.SALE, 100L)).willReturn(true);
    given(recentPropertyMapper.selectRecentPropertyIdsOverLimit(1L, 30)).willReturn(List.of());

    service.saveRecentProperty(1L, request);

    then(recentPropertyMapper).should().upsertRecentProperty(1L, 10L, DealType.SALE, 100L);
  }

  @Test
  void saveRecentProperty_30개를_초과하면_오래된_최근_본_주택을_삭제한다() {
    RecentPropertySaveRequest request = request(10L, null, null);
    given(propertyMapper.existsPropertyById(10L)).willReturn(true);
    given(recentPropertyMapper.selectRecentPropertyIdsOverLimit(1L, 30)).willReturn(List.of(31L));

    service.saveRecentProperty(1L, request);

    then(recentPropertyMapper).should().deleteRecentPropertiesByIds(List.of(31L));
  }

  @Test
  void saveRecentProperty_존재하지_않는_주택이면_PROPERTY_NOT_FOUND_예외가_발생한다() {
    RecentPropertySaveRequest request = request(99L, null, null);
    given(propertyMapper.existsPropertyById(99L)).willReturn(false);

    assertThatThrownBy(() -> service.saveRecentProperty(1L, request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROPERTY_NOT_FOUND)
        );
    then(recentPropertyMapper).should(never()).upsertRecentProperty(
        org.mockito.ArgumentMatchers.anyLong(),
        org.mockito.ArgumentMatchers.anyLong(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()
    );
  }

  @Test
  void saveRecentProperty_마지막_거래_정보가_불완전하면_INVALID_INPUT_VALUE_예외가_발생한다() {
    RecentPropertySaveRequest request = request(10L, DealType.JEONSE, null);

    assertThatThrownBy(() -> service.saveRecentProperty(1L, request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void saveRecentProperty_마지막_거래가_주택에_속하지_않으면_DEAL_NOT_FOUND_예외가_발생한다() {
    RecentPropertySaveRequest request = request(10L, DealType.MONTHLY_RENT, 100L);
    given(propertyMapper.existsPropertyById(10L)).willReturn(true);
    given(recentPropertyMapper.existsDealForProperty(10L, DealType.MONTHLY_RENT, 100L))
        .willReturn(false);

    assertThatThrownBy(() -> service.saveRecentProperty(1L, request))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.DEAL_NOT_FOUND)
        );
    then(recentPropertyMapper).should(never()).upsertRecentProperty(
        org.mockito.ArgumentMatchers.anyLong(),
        org.mockito.ArgumentMatchers.anyLong(),
        org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any()
    );
  }

  private RecentPropertySaveRequest request(
      Long propertyId,
      DealType lastDealType,
      Long lastDealId) {
    RecentPropertySaveRequest request = new RecentPropertySaveRequest();
    request.setPropertyId(propertyId);
    request.setLastDealType(lastDealType);
    request.setLastDealId(lastDealId);
    return request;
  }
}
