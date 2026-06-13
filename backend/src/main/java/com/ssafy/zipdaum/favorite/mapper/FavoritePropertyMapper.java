package com.ssafy.zipdaum.favorite.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoritePropertyMapper {

  void insertFavoriteProperty(
      @Param("userId") Long userId,
      @Param("propertyId") Long propertyId
  );

  int deleteFavoriteProperty(
      @Param("userId") Long userId,
      @Param("propertyId") Long propertyId
  );
}
