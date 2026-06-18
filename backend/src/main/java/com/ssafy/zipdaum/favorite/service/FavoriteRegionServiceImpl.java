package com.ssafy.zipdaum.favorite.service;

import com.ssafy.zipdaum.favorite.dto.FavoriteRegionCandidateResponse;
import com.ssafy.zipdaum.favorite.dto.FavoriteRegionResponse;
import com.ssafy.zipdaum.favorite.mapper.FavoriteRegionMapper;
import com.ssafy.zipdaum.global.error.ErrorCode;
import com.ssafy.zipdaum.global.exception.BusinessException;
import com.ssafy.zipdaum.property.domain.RegionCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class FavoriteRegionServiceImpl implements FavoriteRegionService {

  private static final int MAX_SEARCH_KEYWORD_LENGTH = 50;

  private final FavoriteRegionMapper favoriteRegionMapper;

  @Override
  @Transactional(readOnly = true)
  public List<FavoriteRegionCandidateResponse> findFavoriteRegionCandidates(String keyword) {
    String normalizedKeyword = normalizeSearchKeyword(keyword);
    RegionSearchCondition condition = buildRegionSearchCondition(normalizedKeyword);

    List<FavoriteRegionCandidateResponse> candidates =
        favoriteRegionMapper.selectFavoriteRegionCandidates(
            condition.sggCds(),
            condition.umdKeyword()
        );

    candidates.forEach(candidate -> {
      String regionName = RegionCode.nameOf(candidate.getSggCd());
      candidate.setDisplayName(regionName + " " + candidate.getUmdNm());
    });

    log.debug("관심 지역 등록 후보 검색 완료 candidateCount={}", candidates.size());

    return candidates;
  }

  @Override
  @Transactional(readOnly = true)
  public List<FavoriteRegionResponse> findFavoriteRegions(Long userId) {
    List<FavoriteRegionResponse> favoriteRegions =
        favoriteRegionMapper.selectFavoriteRegions(userId, LocalDate.now().minusYears(1));

    favoriteRegions.forEach(region ->
        region.setRegionName(RegionCode.nameOf(region.getSggCd()))
    );

    log.info("관심 지역 조회 완료 userId={}, regionCount={}", userId, favoriteRegions.size());

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

  private String normalizeSearchKeyword(String keyword) {
    if (keyword == null) {
      log.warn("관심 지역 등록 후보 검색 실패 - 검색어 누락");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    String normalizedKeyword = keyword.trim();

    if (normalizedKeyword.isBlank()) {
      log.warn("관심 지역 등록 후보 검색 실패 - 검색어 공백");
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    if (normalizedKeyword.length() > MAX_SEARCH_KEYWORD_LENGTH) {
      log.warn("관심 지역 등록 후보 검색 실패 - 검색어 길이 초과 length={}", normalizedKeyword.length());
      throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE);
    }

    return normalizedKeyword;
  }

  private String escapeLikeKeyword(String keyword) {
    return keyword
        .replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }

  private RegionSearchCondition buildRegionSearchCondition(String keyword) {
    String compactKeyword = removeBlank(keyword);
    List<RegionMatch> matches = findRegionMatches(compactKeyword);

    if (matches.isEmpty()) {
      return new RegionSearchCondition(List.of(), escapeLikeKeyword(compactKeyword));
    }

    RegionMatch longestMatch = matches.getFirst();
    String remainingKeyword = compactKeyword.replace(longestMatch.keyword(), "");
    String umdKeyword = remainingKeyword.isBlank() ? null : escapeLikeKeyword(remainingKeyword);
    List<String> sggCds = matches.stream()
        .map(RegionMatch::sggCd)
        .distinct()
        .toList();

    return new RegionSearchCondition(sggCds, umdKeyword);
  }

  private List<RegionMatch> findRegionMatches(String keyword) {
    List<RegionMatch> matches = new ArrayList<>();

    for (RegionCode regionCode : RegionCode.values()) {
      for (String regionKeyword : createRegionKeywords(regionCode.getName())) {
        if (keyword.contains(regionKeyword)) {
          matches.add(new RegionMatch(regionCode.getLawdCd(), regionKeyword));
        }
      }
    }

    return matches.stream()
        .sorted(Comparator.comparingInt((RegionMatch match) -> match.keyword().length()).reversed())
        .toList();
  }

  private Set<String> createRegionKeywords(String regionName) {
    String compactRegionName = removeBlank(regionName);
    String withoutBusan = compactRegionName.replace("부산", "");
    String withFullBusan = compactRegionName.replace("부산", "부산광역시");

    Set<String> keywords = new LinkedHashSet<>();
    addRegionKeyword(keywords, compactRegionName);
    addRegionKeyword(keywords, withoutBusan);
    addRegionKeyword(keywords, withFullBusan);
    addRegionKeyword(keywords, removeRegionSuffix(compactRegionName));
    addRegionKeyword(keywords, removeRegionSuffix(withoutBusan));
    addRegionKeyword(keywords, removeRegionSuffix(withFullBusan));

    return keywords;
  }

  private void addRegionKeyword(Set<String> keywords, String keyword) {
    if (keyword != null && keyword.length() >= 2) {
      keywords.add(keyword);
    }
  }

  private String removeRegionSuffix(String keyword) {
    if (keyword.endsWith("구") || keyword.endsWith("군")) {
      return keyword.substring(0, keyword.length() - 1);
    }
    return keyword;
  }

  private String removeBlank(String value) {
    return value.replaceAll("\\s+", "");
  }

  private record RegionSearchCondition(List<String> sggCds, String umdKeyword) {
  }

  private record RegionMatch(String sggCd, String keyword) {
  }
}
