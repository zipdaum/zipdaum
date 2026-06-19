package com.ssafy.zipdaum.recent.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
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
  void recordRecentProperty_주택이_존재하면_최근_본_주택을_저장한다() {
    given(propertyMapper.existsPropertyById(10L)).willReturn(true);
    given(recentPropertyMapper.selectRecentPropertyIdsOverLimit(1L, 30)).willReturn(List.of());

    service.recordRecentProperty(1L, 10L);

    then(recentPropertyMapper).should().upsertRecentProperty(1L, 10L);
    then(recentPropertyMapper).should(never()).deleteRecentPropertiesByIds(
        org.mockito.ArgumentMatchers.anyList()
    );
  }

  @Test
  void recordRecentProperty_30개를_초과하면_오래된_최근_본_주택을_삭제한다() {
    given(propertyMapper.existsPropertyById(10L)).willReturn(true);
    given(recentPropertyMapper.selectRecentPropertyIdsOverLimit(1L, 30)).willReturn(List.of(31L));

    service.recordRecentProperty(1L, 10L);

    then(recentPropertyMapper).should().deleteRecentPropertiesByIds(List.of(31L));
  }

  @Test
  void recordRecentProperty_존재하지_않는_주택이면_PROPERTY_NOT_FOUND_예외가_발생한다() {
    given(propertyMapper.existsPropertyById(99L)).willReturn(false);

    assertThatThrownBy(() -> service.recordRecentProperty(1L, 99L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROPERTY_NOT_FOUND)
        );
    then(recentPropertyMapper).should(never()).upsertRecentProperty(
        org.mockito.ArgumentMatchers.anyLong(),
        org.mockito.ArgumentMatchers.anyLong()
    );
  }

  @Test
  void recordRecentProperty_주택_ID가_null이면_INVALID_INPUT_VALUE_예외가_발생한다() {
    assertThatThrownBy(() -> service.recordRecentProperty(1L, null))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void recordRecentProperty_주택_ID가_1보다_작으면_INVALID_PROPERTY_ID_예외가_발생한다() {
    assertThatThrownBy(() -> service.recordRecentProperty(1L, 0L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROPERTY_ID)
        );
  }
}
