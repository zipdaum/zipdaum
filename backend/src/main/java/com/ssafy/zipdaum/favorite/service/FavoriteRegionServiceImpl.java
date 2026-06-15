package com.ssafy.zipdaum.favorite.service;

import com.ssafy.zipdaum.favorite.dto.FavoriteRegionResponse;
import com.ssafy.zipdaum.favorite.mapper.FavoriteRegionMapper;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.RegionCode;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteRegionServiceImpl implements FavoriteRegionService {

  private final FavoriteRegionMapper favoriteRegionMapper;

  @Override
  @Transactional(readOnly = true)
  public List<FavoriteRegionResponse> findFavoriteRegions(Long userId) {
    List<FavoriteRegionResponse> favoriteRegions =
        favoriteRegionMapper.selectFavoriteRegions(userId, LocalDate.now().minusYears(1));

    favoriteRegions.forEach(region ->
        region.setRegionName(RegionCode.nameOf(region.getSggCd()))
    );

    log.debug("관심 지역 조회 완료 userId={}, regionCount={}", userId, favoriteRegions.size());

    return favoriteRegions;
  }

  @Override
  @Transactional
  public void saveFavoriteRegion(Long userId, String sggCd, String umdNm) {
    String normalizedSggCd = sggCd.trim();
    String normalizedUmdNm = umdNm.trim();

    if (!RegionCode.isValid(normalizedSggCd)) {
      log.warn("존재하지 않는 지역 코드 sggCd={}", normalizedSggCd);
      throw new BusinessException(ErrorCode.INVALID_REGION_CODE);
    }

    try {
      favoriteRegionMapper.insertFavoriteRegion(userId, normalizedSggCd, normalizedUmdNm);
    } catch (DuplicateKeyException e) {
      log.warn(
          "이미 등록된 관심 지역 userId={}, sggCd={}, umdNm={}",
          userId,
          normalizedSggCd,
          normalizedUmdNm
      );
      throw new BusinessException(ErrorCode.FAVORITE_ALREADY_EXISTS);
    }

    log.info(
        "관심 지역 등록 완료 userId={}, sggCd={}, umdNm={}",
        userId,
        normalizedSggCd,
        normalizedUmdNm
    );
  }

  @Override
  @Transactional
  public void removeFavoriteRegion(Long userId, String sggCd, String umdNm) {
    String normalizedSggCd = sggCd.trim();
    String normalizedUmdNm = umdNm.trim();

    int deletedCount = favoriteRegionMapper.deleteFavoriteRegion(
        userId,
        normalizedSggCd,
        normalizedUmdNm
    );

    if (deletedCount == 0) {
      log.warn(
          "관심 목록에 없는 지역 userId={}, sggCd={}, umdNm={}",
          userId,
          normalizedSggCd,
          normalizedUmdNm
      );
      throw new BusinessException(ErrorCode.FAVORITE_NOT_FOUND);
    }

    log.info(
        "관심 지역 해제 완료 userId={}, sggCd={}, umdNm={}",
        userId,
        normalizedSggCd,
        normalizedUmdNm
    );
  }
}
