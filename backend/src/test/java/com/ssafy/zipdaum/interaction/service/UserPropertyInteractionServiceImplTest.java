package com.ssafy.zipdaum.interaction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.interaction.dto.UserPropertyInteractionRequest;
import com.ssafy.zipdaum.interaction.dto.UserPropertyInteractionSaveCommand;
import com.ssafy.zipdaum.interaction.mapper.UserPropertyInteractionMapper;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class UserPropertyInteractionServiceImplTest {

  private final UserPropertyInteractionMapper userPropertyInteractionMapper =
      mock(UserPropertyInteractionMapper.class);
  private final PropertyMapper propertyMapper = mock(PropertyMapper.class);
  private final UserPropertyInteractionServiceImpl service =
      new UserPropertyInteractionServiceImpl(userPropertyInteractionMapper, propertyMapper);

  @Test
  void saveInteraction_주택이_존재하면_행동_로그를_저장한다() {
    UserPropertyInteractionRequest request = request(12000L, 80, true, true);
    given(propertyMapper.existsPropertyById(10L)).willReturn(true);

    service.saveInteraction(1L, 10L, request);

    ArgumentCaptor<UserPropertyInteractionSaveCommand> captor =
        ArgumentCaptor.forClass(UserPropertyInteractionSaveCommand.class);
    then(userPropertyInteractionMapper).should().saveUserPropertyInteraction(captor.capture());
    UserPropertyInteractionSaveCommand command = captor.getValue();
    assertThat(command.getUserId()).isEqualTo(1L);
    assertThat(command.getPropertyId()).isEqualTo(10L);
    assertThat(command.getDwellTimeMillis()).isEqualTo(12000L);
    assertThat(command.getMaxScrollDepthPercent()).isEqualTo(80);
    assertThat(command.getRecommendationDetailClicked()).isTrue();
    assertThat(command.getDealHistoryClicked()).isTrue();
  }

  @Test
  void saveInteraction_존재하지_않는_주택이면_PROPERTY_NOT_FOUND_예외가_발생한다() {
    given(propertyMapper.existsPropertyById(99L)).willReturn(false);

    assertThatThrownBy(() -> service.saveInteraction(1L, 99L, request(1000L, 10, false, false)))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROPERTY_NOT_FOUND)
        );
    then(userPropertyInteractionMapper).should(never()).saveUserPropertyInteraction(any());
  }

  @Test
  void saveInteraction_주택_ID가_null이면_INVALID_INPUT_VALUE_예외가_발생한다() {
    assertThatThrownBy(() -> service.saveInteraction(1L, null, request(1000L, 10, false, false)))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );
  }

  @Test
  void saveInteraction_주택_ID가_1보다_작으면_INVALID_PROPERTY_ID_예외가_발생한다() {
    assertThatThrownBy(() -> service.saveInteraction(1L, 0L, request(1000L, 10, false, false)))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_PROPERTY_ID)
        );
  }

  private UserPropertyInteractionRequest request(
      Long dwellTimeMillis,
      Integer maxScrollDepthPercent,
      Boolean recommendationDetailClicked,
      Boolean dealHistoryClicked
  ) {
    UserPropertyInteractionRequest request = new UserPropertyInteractionRequest();
    request.setDwellTimeMillis(dwellTimeMillis);
    request.setMaxScrollDepthPercent(maxScrollDepthPercent);
    request.setRecommendationDetailClicked(recommendationDetailClicked);
    request.setDealHistoryClicked(dealHistoryClicked);
    return request;
  }
}
