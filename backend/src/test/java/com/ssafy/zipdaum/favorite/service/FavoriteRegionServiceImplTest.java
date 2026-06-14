package com.ssafy.zipdaum.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.favorite.mapper.FavoriteRegionMapper;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import org.junit.jupiter.api.Test;

class FavoriteRegionServiceImplTest {

  private final FavoriteRegionMapper favoriteRegionMapper = mock(FavoriteRegionMapper.class);
  private final FavoriteRegionServiceImpl service =
      new FavoriteRegionServiceImpl(favoriteRegionMapper);

  @Test
  void saveFavoriteRegion_존재하는_지역이면_관심_지역으로_등록한다() {
    service.saveFavoriteRegion(1L, " 26350 ", " 우동 ");

    then(favoriteRegionMapper).should().insertFavoriteRegion(1L, "26350", "우동");
  }

  @Test
  void saveFavoriteRegion_존재하지_않는_지역이면_INVALID_REGION_CODE_예외가_발생한다() {
    assertThatThrownBy(() -> service.saveFavoriteRegion(1L, "99999", "없는동"))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REGION_CODE)
        );

    then(favoriteRegionMapper).shouldHaveNoInteractions();
  }
}
