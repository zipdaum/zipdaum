package com.ssafy.zipdaum.recent.mapper;

import com.ssafy.zipdaum.recent.dto.RecentPropertyResponse;
import com.ssafy.zipdaum.recent.dto.RecentPropertyScoreFactor;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface RecentPropertyMapper {

  List<RecentPropertyResponse> selectRecentProperties(@Param("userId") Long userId);

  List<RecentPropertyScoreFactor> selectRecentPropertyScoreFactors(@Param("userId") Long userId);

  void upsertRecentProperty(
      @Param("userId") Long userId,
      @Param("propertyId") Long propertyId);

  List<Long> selectRecentPropertyIdsOverLimit(
      @Param("userId") Long userId,
      @Param("limit") int limit);

  int deleteRecentPropertiesByIds(@Param("ids") List<Long> ids);
}
