package com.ssafy.zipdaum.favorite.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;

import com.ssafy.zipdaum.favorite.dto.FavoriteRegionCandidateResponse;
import com.ssafy.zipdaum.favorite.dto.FavoriteRegionResponse;
import com.ssafy.zipdaum.favorite.mapper.FavoriteRegionMapper;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.RegionCode;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;

@SuppressWarnings("unchecked")
class FavoriteRegionServiceImplTest {

  private final FavoriteRegionMapper favoriteRegionMapper = mock(FavoriteRegionMapper.class);
  private final FavoriteRegionServiceImpl service =
      new FavoriteRegionServiceImpl(favoriteRegionMapper);

  @Test
  void findFavoriteRegionCandidates_구와_동_검색어로_관심_지역_등록_후보를_조회한다() {
    FavoriteRegionCandidateResponse candidate = new FavoriteRegionCandidateResponse();
    candidate.setSggCd("26350");
    candidate.setUmdNm("우동");
    given(favoriteRegionMapper.selectFavoriteRegionCandidates(List.of("26350"), "우동"))
        .willReturn(List.of(candidate));

    List<FavoriteRegionCandidateResponse> result =
        service.findFavoriteRegionCandidates("해운대우동");

    assertThat(result).containsExactly(candidate);
    assertThat(result.getFirst().getDisplayName()).isEqualTo("부산 해운대구 우동");
    then(favoriteRegionMapper).should()
        .selectFavoriteRegionCandidates(List.of("26350"), "우동");
  }

  @Test
  void findFavoriteRegionCandidates_구_검색어로_관심_지역_등록_후보를_조회한다() {
    FavoriteRegionCandidateResponse candidate = new FavoriteRegionCandidateResponse();
    candidate.setSggCd("26350");
    candidate.setUmdNm("우동");
    given(favoriteRegionMapper.selectFavoriteRegionCandidates(List.of("26350"), null))
        .willReturn(List.of(candidate));

    List<FavoriteRegionCandidateResponse> result =
        service.findFavoriteRegionCandidates(" 부산 해운대 ");

    assertThat(result).containsExactly(candidate);
    assertThat(result.getFirst().getDisplayName()).isEqualTo("부산 해운대구 우동");
    then(favoriteRegionMapper).should()
        .selectFavoriteRegionCandidates(List.of("26350"), null);
  }

  @Test
  void findFavoriteRegionCandidates_동_검색어로_관심_지역_등록_후보를_조회한다() {
    FavoriteRegionCandidateResponse candidate = new FavoriteRegionCandidateResponse();
    candidate.setSggCd("26350");
    candidate.setUmdNm("우동");
    given(favoriteRegionMapper.selectFavoriteRegionCandidates(List.of(), "우동"))
        .willReturn(List.of(candidate));

    List<FavoriteRegionCandidateResponse> result =
        service.findFavoriteRegionCandidates("우동");

    assertThat(result).containsExactly(candidate);
    assertThat(result.getFirst().getDisplayName()).isEqualTo("부산 해운대구 우동");
    then(favoriteRegionMapper).should()
        .selectFavoriteRegionCandidates(List.of(), "우동");
  }

  @Test
  void findFavoriteRegionCandidates_서구_검색어는_서구와_강서구를_함께_조회한다() {
    given(favoriteRegionMapper.selectFavoriteRegionCandidates(
        org.mockito.ArgumentMatchers.anyList(),
        org.mockito.ArgumentMatchers.isNull()
    )).willReturn(List.of());

    service.findFavoriteRegionCandidates("서구");

    ArgumentCaptor<List<String>> sggCdsCaptor = ArgumentCaptor.forClass(List.class);
    then(favoriteRegionMapper).should()
        .selectFavoriteRegionCandidates(sggCdsCaptor.capture(), org.mockito.ArgumentMatchers.isNull());
    assertThat(sggCdsCaptor.getValue()).contains("26140", "26440");
  }

  @Test
  void findFavoriteRegionCandidates_강서구_검색어는_서구를_제외하고_강서구만_조회한다() {
    given(favoriteRegionMapper.selectFavoriteRegionCandidates(
        org.mockito.ArgumentMatchers.anyList(),
        org.mockito.ArgumentMatchers.isNull()
    )).willReturn(List.of());

    service.findFavoriteRegionCandidates("강서구");

    ArgumentCaptor<List<String>> sggCdsCaptor = ArgumentCaptor.forClass(List.class);
    then(favoriteRegionMapper).should()
        .selectFavoriteRegionCandidates(sggCdsCaptor.capture(), org.mockito.ArgumentMatchers.isNull());
    assertThat(sggCdsCaptor.getValue()).containsExactly("26440");
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
  void findFavoriteRegions_관심_지역에_지역명을_설정하여_반환한다() {
    FavoriteRegionResponse favoriteRegion = new FavoriteRegionResponse();
    favoriteRegion.setSggCd("26350");
    given(favoriteRegionMapper.selectFavoriteRegions(
        org.mockito.ArgumentMatchers.eq(1L),
        any(LocalDate.class)
    )).willReturn(List.of(favoriteRegion));

    List<FavoriteRegionResponse> result = service.findFavoriteRegions(1L);

    assertThat(result).containsExactly(favoriteRegion);
    assertThat(result.getFirst().getRegionName()).isEqualTo(RegionCode.nameOf("26350"));
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
    service.saveFavoriteRegion(1L, "26350", "우동");

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

  @Test
  void saveFavoriteRegion_이미_등록된_관심_지역이면_FAVORITE_ALREADY_EXISTS_예외가_발생한다() {
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
