package com.ssafy.zipdaum.favorite.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteRegionMapper {

  void insertFavoriteRegion(
      @Param("userId") Long userId,
      @Param("sggCd") String sggCd,
      @Param("umdNm") String umdNm
  );
}
