package com.ssafy.zipdaum.favorite.mapper;

import com.ssafy.zipdaum.favorite.dto.FavoriteRegionResponse;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface FavoriteRegionMapper {

  List<FavoriteRegionCandidateResponse> selectFavoriteRegionCandidates(
      @Param("sggCds") List<String> sggCds,
      @Param("umdKeyword") String umdKeyword
  );

  List<FavoriteRegionResponse> selectFavoriteRegions(
      @Param("userId") Long userId,
      @Param("oneYearAgo") LocalDate oneYearAgo
  );

  void insertFavoriteRegion(
      @Param("userId") Long userId,
      @Param("sggCd") String sggCd,
      @Param("umdNm") String umdNm
  );

  int deleteFavoriteRegion(
      @Param("userId") Long userId,
      @Param("sggCd") String sggCd,
      @Param("umdNm") String umdNm
  );
}
