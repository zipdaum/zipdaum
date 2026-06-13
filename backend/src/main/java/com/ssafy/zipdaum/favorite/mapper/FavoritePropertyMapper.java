package com.ssafy.zipdaum.favorite.mapper;

import com.ssafy.zipdaum.favorite.dto.FavoritePropertyResponse;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoritePropertyMapper {

  List<FavoritePropertyResponse> selectFavoriteProperties(
      @Param("userId") Long userId,
      @Param("oneYearAgo") LocalDate oneYearAgo
  );

  void insertFavoriteProperty(
      @Param("userId") Long userId,
      @Param("propertyId") Long propertyId
  );

  int deleteFavoriteProperty(
      @Param("userId") Long userId,
      @Param("propertyId") Long propertyId
  );
}
