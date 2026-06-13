package com.ssafy.zipdaum.favorite.service;

public interface FavoritePropertyService {

  void saveFavoriteProperty(Long userId, Long propertyId);

  void removeFavoriteProperty(Long userId, Long propertyId);
}
