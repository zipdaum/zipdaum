package com.ssafy.zipdaum.favorite.service;

import com.ssafy.zipdaum.favorite.dto.FavoritePropertyResponse;
import java.util.List;

public interface FavoritePropertyService {

  List<FavoritePropertyResponse> findFavoriteProperties(Long userId);

  void saveFavoriteProperty(Long userId, Long propertyId);

  void removeFavoriteProperty(Long userId, Long propertyId);
}
