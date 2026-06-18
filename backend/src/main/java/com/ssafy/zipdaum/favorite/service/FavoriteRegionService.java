package com.ssafy.zipdaum.favorite.service;

import com.ssafy.zipdaum.favorite.dto.FavoriteRegionCandidateResponse;
import com.ssafy.zipdaum.favorite.dto.FavoriteRegionResponse;
import java.util.List;

public interface FavoriteRegionService {

  List<FavoriteRegionCandidateResponse> findFavoriteRegionCandidates(String keyword);

  List<FavoriteRegionResponse> findFavoriteRegions(Long userId);

  void saveFavoriteRegion(Long userId, String sggCd, String umdNm);

  void removeFavoriteRegion(Long userId, String sggCd, String umdNm);
}
