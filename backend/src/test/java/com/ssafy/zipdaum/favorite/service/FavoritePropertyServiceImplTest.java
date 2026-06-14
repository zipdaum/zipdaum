package com.ssafy.zipdaum.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.favorite.dto.FavoritePropertyResponse;
import com.ssafy.zipdaum.favorite.mapper.FavoritePropertyMapper;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.RegionCode;
import com.ssafy.zipdaum.property.mapper.PropertyMapper;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class FavoritePropertyServiceImplTest {

  private final FavoritePropertyMapper favoritePropertyMapper = mock(FavoritePropertyMapper.class);
  private final PropertyMapper propertyMapper = mock(PropertyMapper.class);
  private final FavoritePropertyServiceImpl service =
      new FavoritePropertyServiceImpl(favoritePropertyMapper, propertyMapper);

  @Test
  void findFavoriteProperties_관심_주택_목록에_지역명을_설정하여_반환한다() {
    FavoritePropertyResponse favoriteProperty = new FavoritePropertyResponse();
    favoriteProperty.setPropertyId(10L);
    favoriteProperty.setSggCd("26350");
    given(favoritePropertyMapper.selectFavoriteProperties(
        org.mockito.ArgumentMatchers.eq(1L),
        any(LocalDate.class)
    )).willReturn(List.of(favoriteProperty));

    List<FavoritePropertyResponse> result = service.findFavoriteProperties(1L);

    assertThat(result).containsExactly(favoriteProperty);
    assertThat(result.getFirst().getRegionName()).isEqualTo(RegionCode.nameOf("26350"));
    then(favoritePropertyMapper).should()
        .selectFavoriteProperties(org.mockito.ArgumentMatchers.eq(1L), any(LocalDate.class));
  }

  @Test
  void findFavoriteProperties_관심_주택이_없으면_빈_목록을_반환한다() {
    given(favoritePropertyMapper.selectFavoriteProperties(
        org.mockito.ArgumentMatchers.eq(1L),
        any(LocalDate.class)
    )).willReturn(List.of());

    List<FavoritePropertyResponse> result = service.findFavoriteProperties(1L);

    assertThat(result).isEmpty();
  }

  @Test
  void saveFavoriteProperty_존재하는_주택이면_관심_주택으로_등록한다() {
    given(propertyMapper.existsPropertyById(10L)).willReturn(true);

    service.saveFavoriteProperty(1L, 10L);

    then(favoritePropertyMapper).should().insertFavoriteProperty(1L, 10L);
  }

  @Test
  void saveFavoriteProperty_존재하지_않는_주택이면_PROPERTY_NOT_FOUND_예외가_발생한다() {
    given(propertyMapper.existsPropertyById(10L)).willReturn(false);

    assertThatThrownBy(() -> service.saveFavoriteProperty(1L, 10L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.PROPERTY_NOT_FOUND)
        );

    then(propertyMapper).should().existsPropertyById(10L);
    then(favoritePropertyMapper).shouldHaveNoMoreInteractions();
  }

  @Test
  void saveFavoriteProperty_이미_등록된_관심_주택이면_FAVORITE_ALREADY_EXISTS_예외가_발생한다() {
    given(propertyMapper.existsPropertyById(10L)).willReturn(true);
    willThrow(new DuplicateKeyException("중복 관심 주택"))
        .given(favoritePropertyMapper).insertFavoriteProperty(1L, 10L);

    assertThatThrownBy(() -> service.saveFavoriteProperty(1L, 10L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FAVORITE_ALREADY_EXISTS)
        );
  }

  @Test
  void removeFavoriteProperty_관심_목록에_있는_주택이면_해제한다() {
    given(favoritePropertyMapper.deleteFavoriteProperty(1L, 10L)).willReturn(1);

    service.removeFavoriteProperty(1L, 10L);

    then(favoritePropertyMapper).should().deleteFavoriteProperty(1L, 10L);
  }

  @Test
  void removeFavoriteProperty_관심_목록에_없는_주택이면_FAVORITE_NOT_FOUND_예외가_발생한다() {
    given(favoritePropertyMapper.deleteFavoriteProperty(1L, 10L)).willReturn(0);

    assertThatThrownBy(() -> service.removeFavoriteProperty(1L, 10L))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FAVORITE_NOT_FOUND)
        );

    then(favoritePropertyMapper).should().deleteFavoriteProperty(1L, 10L);
  }
}
