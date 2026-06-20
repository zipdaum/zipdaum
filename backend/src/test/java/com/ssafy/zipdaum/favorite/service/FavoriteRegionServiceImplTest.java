package com.ssafy.zipdaum.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;

import com.ssafy.zipdaum.favorite.dto.FavoriteRegionCandidateResponse;
import com.ssafy.zipdaum.favorite.dto.FavoriteRegionResponse;
import com.ssafy.zipdaum.favorite.mapper.FavoriteRegionMapper;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class FavoriteRegionServiceImplTest {

  private final FavoriteRegionMapper favoriteRegionMapper = mock(FavoriteRegionMapper.class);
  private final FavoriteRegionServiceImpl service =
      new FavoriteRegionServiceImpl(favoriteRegionMapper);

  @Test
  void findFavoriteRegionCandidates_검색어_공백을_제거한_키워드로_지역_후보를_조회한다() {
    FavoriteRegionCandidateResponse candidate = new FavoriteRegionCandidateResponse();
    candidate.setSggCd("26350");
    candidate.setUmdNm("우동");
    candidate.setDisplayName("부산광역시 해운대구 우동");
    given(favoriteRegionMapper.selectFavoriteRegionCandidates("해운대우동"))
        .willReturn(List.of(candidate));

    List<FavoriteRegionCandidateResponse> result =
        service.findFavoriteRegionCandidates(" 해운대 우동 ");

    assertThat(result).containsExactly(candidate);
    then(favoriteRegionMapper).should().selectFavoriteRegionCandidates("해운대우동");
  }

  @Test
  void findFavoriteRegionCandidates_LIKE_특수문자를_이스케이프해서_조회한다() {
    given(favoriteRegionMapper.selectFavoriteRegionCandidates("\\%우\\_동"))
        .willReturn(List.of());

    List<FavoriteRegionCandidateResponse> result =
        service.findFavoriteRegionCandidates("%우_동");

    assertThat(result).isEmpty();
    then(favoriteRegionMapper).should().selectFavoriteRegionCandidates("\\%우\\_동");
  }

  @Test
  void findFavoriteRegionCandidates_검색어가_null이면_INVALID_INPUT_VALUE_예외가_발생한다() {
    assertThatThrownBy(() -> service.findFavoriteRegionCandidates(null))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );

    then(favoriteRegionMapper).shouldHaveNoInteractions();
  }

  @Test
  void findFavoriteRegionCandidates_검색어가_공백이면_INVALID_INPUT_VALUE_예외가_발생한다() {
    assertThatThrownBy(() -> service.findFavoriteRegionCandidates("   "))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );

    then(favoriteRegionMapper).shouldHaveNoInteractions();
  }

  @Test
  void findFavoriteRegionCandidates_검색어가_너무_길면_INVALID_INPUT_VALUE_예외가_발생한다() {
    String keyword = "a".repeat(51);

    assertThatThrownBy(() -> service.findFavoriteRegionCandidates(keyword))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_INPUT_VALUE)
        );

    then(favoriteRegionMapper).shouldHaveNoInteractions();
  }

  @Test
  void findFavoriteRegions_관심_지역_목록을_조회한다() {
    FavoriteRegionResponse favoriteRegion = new FavoriteRegionResponse();
    favoriteRegion.setSggCd("26350");
    favoriteRegion.setRegionName("부산광역시 해운대구");
    favoriteRegion.setUmdNm("우동");
    given(favoriteRegionMapper.selectFavoriteRegions(
        org.mockito.ArgumentMatchers.eq(1L),
        any(LocalDate.class)
    )).willReturn(List.of(favoriteRegion));

    List<FavoriteRegionResponse> result = service.findFavoriteRegions(1L);

    assertThat(result).containsExactly(favoriteRegion);
    then(favoriteRegionMapper).should()
        .selectFavoriteRegions(org.mockito.ArgumentMatchers.eq(1L), any(LocalDate.class));
  }

  @Test
  void findFavoriteRegions_관심_지역이_없으면_빈_목록을_반환한다() {
    given(favoriteRegionMapper.selectFavoriteRegions(
        org.mockito.ArgumentMatchers.eq(1L),
        any(LocalDate.class)
    )).willReturn(List.of());

    List<FavoriteRegionResponse> result = service.findFavoriteRegions(1L);

    assertThat(result).isEmpty();
  }

  @Test
  void saveFavoriteRegion_존재하는_지역이면_관심_지역으로_등록한다() {
    given(favoriteRegionMapper.existsRegion("26350", "우동")).willReturn(true);

    service.saveFavoriteRegion(1L, "26350", "우동");

    then(favoriteRegionMapper).should().existsRegion("26350", "우동");
    then(favoriteRegionMapper).should().insertFavoriteRegion(1L, "26350", "우동");
  }

  @Test
  void saveFavoriteRegion_존재하지_않는_지역이면_INVALID_REGION_CODE_예외가_발생한다() {
    given(favoriteRegionMapper.existsRegion("99999", "없는동")).willReturn(false);

    assertThatThrownBy(() -> service.saveFavoriteRegion(1L, "99999", "없는동"))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_REGION_CODE)
        );

    then(favoriteRegionMapper).should().existsRegion("99999", "없는동");
    then(favoriteRegionMapper).should(never())
        .insertFavoriteRegion(any(Long.class), any(String.class), any(String.class));
  }

  @Test
  void saveFavoriteRegion_이미_등록된_관심_지역이면_FAVORITE_ALREADY_EXISTS_예외가_발생한다() {
    given(favoriteRegionMapper.existsRegion("26350", "우동")).willReturn(true);
    willThrow(new DuplicateKeyException("중복 관심 지역"))
        .given(favoriteRegionMapper).insertFavoriteRegion(1L, "26350", "우동");

    assertThatThrownBy(() -> service.saveFavoriteRegion(1L, "26350", "우동"))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FAVORITE_ALREADY_EXISTS)
        );
  }

  @Test
  void removeFavoriteRegion_관심_목록에_있는_지역이면_해제한다() {
    given(favoriteRegionMapper.deleteFavoriteRegion(1L, "26350", "우동")).willReturn(1);

    service.removeFavoriteRegion(1L, " 26350 ", " 우동 ");

    then(favoriteRegionMapper).should().deleteFavoriteRegion(1L, "26350", "우동");
  }

  @Test
  void removeFavoriteRegion_관심_목록에_없는_지역이면_FAVORITE_NOT_FOUND_예외가_발생한다() {
    given(favoriteRegionMapper.deleteFavoriteRegion(1L, "26350", "우동")).willReturn(0);

    assertThatThrownBy(() -> service.removeFavoriteRegion(1L, "26350", "우동"))
        .isInstanceOfSatisfying(BusinessException.class, exception ->
            assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FAVORITE_NOT_FOUND)
        );

    then(favoriteRegionMapper).should().deleteFavoriteRegion(1L, "26350", "우동");
  }
}
