package com.ssafy.zipdaum.favorite.service;

public interface FavoriteRegionService {

  void saveFavoriteRegion(Long userId, String sggCd, String umdNm);

  void removeFavoriteRegion(Long userId, String sggCd, String umdNm);
}
